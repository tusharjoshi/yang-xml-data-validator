package com.example;

import com.ctc.wstx.stax.WstxInputFactory;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizationResultHolder;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.spi.source.FileYangTextSource;
import org.opendaylight.yangtools.yang.parser.api.*;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.ServiceLoader;

public class YangXmlDataValidator {

    private static final YangParserFactory PARSER_FACTORY;

    static {
        final var it = ServiceLoader.load(YangParserFactory.class).iterator();
        if (!it.hasNext()) {
            throw new IllegalStateException("No YangParserFactory found");
        }
        PARSER_FACTORY = it.next();
    }


    public static void main(String[] args) throws Exception {
        Path yangFile = Path.of("src/main/resources/module.yang");
        final YangParser parser = PARSER_FACTORY.createParser(YangParserConfiguration.DEFAULT);
        EffectiveModelContext context;

        try {
            // Create YANG source from file
            FileYangTextSource yangSource = new FileYangTextSource(yangFile);
            parser.addSource(yangSource);
        } catch (YangSyntaxErrorException e) {
            throw new IllegalArgumentException("Malformed source", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read a source", e);
        }

        try {
            context = parser.buildEffectiveModel();
            System.out.println("Successfully loaded YANG schema: " + context.getModules().iterator().next().getName());
        } catch (YangParserException e) {
            throw new IllegalStateException("Failed to assemble SchemaContext", e);
        }

        XMLInputFactory factory = new WstxInputFactory();
        File xmlFile = new File("src/main/resources/input.xml");

        // Yang Parser needs a root element
        String xmlText = "<root>" + Files.readString(xmlFile.toPath()) + "</root>";

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(xmlText.getBytes(StandardCharsets.UTF_8));

        XMLStreamReader reader = factory.createXMLStreamReader(byteArrayInputStream);
        Map<String, String> xpathMap = XPathCreator.createXPathMap(xmlText);
        
        // print the XPath map for debugging - sorted by line number then by element counter
        xpathMap.entrySet().stream()
            .sorted((e1, e2) -> {
                String[] parts1 = e1.getKey().split(":");
                String[] parts2 = e2.getKey().split(":");
                if (parts1.length >= 2 && parts2.length >= 2) {
                    try {
                        int line1 = Integer.parseInt(parts1[0]);
                        int line2 = Integer.parseInt(parts2[0]);
                        if (line1 != line2) {
                            return Integer.compare(line1, line2);
                        }
                        // Same line, sort by element counter
                        int counter1 = Integer.parseInt(parts1[1]);
                        int counter2 = Integer.parseInt(parts2[1]);
                        return Integer.compare(counter1, counter2);
                    } catch (NumberFormatException e) {
                        return e1.getKey().compareTo(e2.getKey());
                    }
                }
                return e1.getKey().compareTo(e2.getKey());
            })
            .forEach(entry -> {
                String[] keyParts = entry.getKey().split(":");
                if (keyParts.length >= 3) {
                    String lineNumber = keyParts[0];
                    String elementName = keyParts[2];
                    System.out.println("Line " + lineNumber + " (" + elementName + "): " + entry.getValue());
                }
            });

        try {
            NormalizationResultHolder result = new NormalizationResultHolder();
            NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);

            XmlParserStream xmlParser = XmlParserStream.create(streamWriter, context);
            xmlParser.parse(reader);

            System.out.println("Yang data validation completed successfully!");
        } catch (Exception e) {
            String xpath = findXPathForLineNumber(xpathMap, reader.getLocation().getLineNumber());
            System.err.println("\nLine " + reader.getLocation().getLineNumber() +
                    ", Column " + reader.getLocation().getColumnNumber()
                    + "\nFor XPath: " + (xpath != null ? xpath.substring(5) : "Unknown") // Remove the leading "/root"
                    + "\nError: " + MessageProcessor.processMessage(e.getMessage()));
        } finally {
            reader.close();
        }
    }
    
    private static String findXPathForLineNumber(Map<String, String> xpathMap, int lineNumber) {
        // Look for entries that match the line number
        // If multiple entries exist for the same line, return the last one (most likely the problematic element)
        String result = null;
        for (Map.Entry<String, String> entry : xpathMap.entrySet()) {
            String[] keyParts = entry.getKey().split(":");
            if (keyParts.length >= 1) {
                try {
                    int entryLineNumber = Integer.parseInt(keyParts[0]);
                    if (entryLineNumber == lineNumber) {
                        result = entry.getValue(); // Keep updating to get the last matching entry
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
        }
        return result;
    }
}
