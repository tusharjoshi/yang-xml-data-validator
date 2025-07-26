import com.ctc.wstx.stax.WstxInputFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class XPathCreator {
    public static Map<Integer, String> createXPathMap(String xmlText) throws
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
