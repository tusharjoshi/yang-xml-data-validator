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
        Map<Integer, String> xpathMap = XPathCreator.createXPathMap(xmlText);

        try {
            NormalizationResultHolder result = new NormalizationResultHolder();
            NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);

            XmlParserStream xmlParser = XmlParserStream.create(streamWriter, context);
            xmlParser.parse(reader);

            System.out.println("Yang data validation completed successfully!");
        } catch (Exception e) {
            String xpath = xpathMap.get(reader.getLocation().getLineNumber());
            System.err.println("\nLine " + reader.getLocation().getLineNumber() +
                    ", Column " + reader.getLocation().getColumnNumber()
                    + "\nFor XPath: " + xpath.substring(5) // Remove the leading "/root"
                    + "\nError: " + MessageProcessor.processMessage(e.getMessage()));
        } finally {
            reader.close();
        }
    }
}
