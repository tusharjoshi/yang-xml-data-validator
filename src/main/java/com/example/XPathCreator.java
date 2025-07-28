package com.example;

import com.ctc.wstx.stax.WstxInputFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class XPathCreator {
    public static Map<String, String> createXPathMap(String xmlText) throws
            XMLStreamException {
        XMLInputFactory factory = new WstxInputFactory();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(xmlText.getBytes(StandardCharsets.UTF_8));
        XPathXMLStreamReader reader = new XPathXMLStreamReader(factory.createXMLStreamReader(byteArrayInputStream));
        while (reader.hasNext()) {
            reader.next();
        }
        return reader.getXpathMap();
    }
}
