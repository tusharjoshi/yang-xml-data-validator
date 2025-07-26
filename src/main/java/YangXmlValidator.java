import com.ctc.wstx.stax.WstxInputFactory;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.spi.source.FileYangTextSource;
import org.opendaylight.yangtools.yang.parser.api.*;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.transform.dom.DOMSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.Stack;

public class YangXmlValidator {

    private static final @NonNull YangParserFactory PARSER_FACTORY;

    static {
        final var it = ServiceLoader.load(YangParserFactory.class).iterator();
        if (!it.hasNext()) {
            throw new IllegalStateException("No YangParserFactory found");
        }
        PARSER_FACTORY = it.next();
    }



    public static void main(String[] args) throws Exception {
        // Load the YANG schema context using a simple file-based approach
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

        // Setup Woodstox for line tracking
        XMLInputFactory factory = new WstxInputFactory();
        File xmlFile = new File("src/main/resources/input.xml");
        XPathXMLStreamReader reader = new XPathXMLStreamReader(factory.createXMLStreamReader(new FileInputStream(xmlFile)));

        readXMLForXPath();

        // Create tracking writer that delegates to result writer
        TrackingWriter trackingWriter = new TrackingWriter(reader);

        try {
            XmlParserStream xmlParser = XmlParserStream.create(trackingWriter, context);
            xmlParser.parse(reader);
            
            System.out.println("XML validation completed successfully!");
        } catch (Exception e) {
            System.err.println("Error at line " + reader.getLocation().getLineNumber() +
                    ", column " + reader.getLocation().getColumnNumber() + " " + e.getMessage());
            System.out.println(reader.getCurrentXPath());
            System.out.println(reader.getLocalName());
        } finally {
            reader.close();
        }
    }

    private static void readXMLForXPath() throws XMLStreamException, FileNotFoundException {
        System.out.println("Reading XML for XPath tracking...");
        XMLInputFactory factory = new WstxInputFactory();
        File xmlFile = new File("src/main/resources/input.xml");
        XPathXMLStreamReader reader = new XPathXMLStreamReader(factory.createXMLStreamReader(new FileInputStream(xmlFile)));
        while(reader.hasNext()) {
            reader.next();
        }
    }

    // A simple writer that tracks current XPath and logs line numbers
    public static class TrackingWriter implements NormalizedNodeStreamWriter {
        private final XMLStreamReader reader;
        public final Stack<String> pathStack = new Stack<>();

        public TrackingWriter(XMLStreamReader reader) {
            this.reader = reader;
        }

        @Override
        public void startLeafNode(YangInstanceIdentifier.NodeIdentifier name) throws IOException {
            pathStack.push(name.getNodeType().getLocalName());
        }

        @Override
        public void startLeafSet(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException {

        }

        @Override
        public void startOrderedLeafSet(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException {

        }

        @Override
        public void startLeafSetEntryNode(YangInstanceIdentifier.NodeWithValue<?> name) throws IOException {

        }

        @Override
        public void startContainerNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException {
            pathStack.push(name.getNodeType().getLocalName());
        }

        @Override
        public void startUnkeyedList(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException {

        }

        @Override
        public void startUnkeyedListItem(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException {

        }

        @Override
        public void startMapNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException {

        }

        @Override
        public void startMapEntryNode(YangInstanceIdentifier.NodeIdentifierWithPredicates identifier, int childSizeHint) throws IOException {

        }

        @Override
        public void startOrderedMapNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException {

        }

        @Override
        public void startChoiceNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException {

        }

        @Override
        public boolean startAnydataNode(YangInstanceIdentifier.NodeIdentifier name, Class<?> objectModel) throws IOException {
            return true;
        }

        @Override
        public boolean startAnyxmlNode(YangInstanceIdentifier.NodeIdentifier name, Class<?> objectModel) throws IOException {
            return true;
        }

        @Override
        public void domSourceValue(DOMSource value) throws IOException {

        }

        @Override
        public void endNode() throws IOException {
            if (!pathStack.isEmpty()) pathStack.pop();
        }

        @Override
        public void scalarValue(@NonNull Object value) throws IOException {
        }

        @Override
        public void close() throws IOException {

        }

        @Override
        public void flush() throws IOException {

        }
    }

    public static class XPathXMLStreamReader implements XMLStreamReader {
        private final XMLStreamReader delegate;
        private final Stack<String> elementStack = new Stack<>();
        private String currentXPath = "/";

        public XPathXMLStreamReader(XMLStreamReader delegate) {
            this.delegate = delegate;
        }

        public String getCurrentXPath() {
            return currentXPath;
        }

        private void updateXPath(int event) {
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    elementStack.push(delegate.getLocalName());
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (!elementStack.isEmpty()) {
                        elementStack.pop();
                    }
                    break;
            }
            buildXPath();
        }

        private void buildXPath() {
            if (elementStack.isEmpty()) {
                currentXPath = "/";
            } else {
                currentXPath = String.join("/", elementStack);
            }
        }

        @Override
        public Object getProperty(String name) throws IllegalArgumentException {
            return delegate.getProperty(name);
        }

        @Override
        public int next() throws XMLStreamException {
            int result = delegate.next();
            updateXPath(result);
            return result;
        }

        @Override
        public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
            delegate.require(type, namespaceURI, localName);
        }

        @Override
        public String getElementText() throws XMLStreamException {
            return delegate.getElementText();
        }

        @Override
        public int nextTag() throws XMLStreamException {
            int result = delegate.nextTag();
            updateXPath(result);
            return result;
        }

        @Override
        public boolean hasNext() throws XMLStreamException {
            return delegate.hasNext();
        }

        @Override
        public void close() throws XMLStreamException {
            delegate.close();
        }

        @Override
        public String getNamespaceURI(String prefix) {
            return delegate.getNamespaceURI(prefix);
        }

        @Override
        public boolean isStartElement() {
            return delegate.isStartElement();
        }

        @Override
        public boolean isEndElement() {
            return delegate.isEndElement();
        }

        @Override
        public boolean isCharacters() {
            return delegate.isCharacters();
        }

        @Override
        public boolean isWhiteSpace() {
            return delegate.isWhiteSpace();
        }

        @Override
        public String getAttributeValue(String namespaceURI, String localName) {
            return delegate.getAttributeValue(namespaceURI, localName);
        }

        @Override
        public int getAttributeCount() {
            return delegate.getAttributeCount();
        }

        @Override
        public QName getAttributeName(int index) {
            return delegate.getAttributeName(index);
        }

        @Override
        public String getAttributeNamespace(int index) {
            return delegate.getAttributeNamespace(index);
        }

        @Override
        public String getAttributeLocalName(int index) {
            return delegate.getAttributeLocalName(index);
        }

        @Override
        public String getAttributePrefix(int index) {
            return delegate.getAttributePrefix(index);
        }

        @Override
        public String getAttributeType(int index) {
            return delegate.getAttributeType(index);
        }

        @Override
        public String getAttributeValue(int index) {
            return delegate.getAttributeValue(index);
        }

        @Override
        public boolean isAttributeSpecified(int index) {
            return delegate.isAttributeSpecified(index);
        }

        @Override
        public int getNamespaceCount() {
            return delegate.getNamespaceCount();
        }

        @Override
        public String getNamespacePrefix(int index) {
            return delegate.getNamespacePrefix(index);
        }

        @Override
        public String getNamespaceURI(int index) {
            return delegate.getNamespaceURI(index);
        }

        @Override
        public NamespaceContext getNamespaceContext() {
            return delegate.getNamespaceContext();
        }

        @Override
        public int getEventType() {
            return delegate.getEventType();
        }

        @Override
        public String getText() {
            return delegate.getText();
        }

        @Override
        public char[] getTextCharacters() {
            return delegate.getTextCharacters();
        }

        @Override
        public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
            return delegate.getTextCharacters(sourceStart, target, targetStart, length);
        }

        @Override
        public int getTextStart() {
            return delegate.getTextStart();
        }

        @Override
        public int getTextLength() {
            return delegate.getTextLength();
        }

        @Override
        public String getEncoding() {
            return delegate.getEncoding();
        }

        @Override
        public boolean hasText() {
            return delegate.hasText();
        }

        @Override
        public Location getLocation() {
            return delegate.getLocation();
        }

        @Override
        public QName getName() {
            return delegate.getName();
        }

        @Override
        public String getLocalName() {
            return delegate.getLocalName();
        }

        @Override
        public boolean hasName() {
            return delegate.hasName();
        }

        @Override
        public String getNamespaceURI() {
            return delegate.getNamespaceURI();
        }

        @Override
        public String getPrefix() {
            return delegate.getPrefix();
        }

        @Override
        public String getVersion() {
            return delegate.getVersion();
        }

        @Override
        public boolean isStandalone() {
            return delegate.isStandalone();
        }

        @Override
        public boolean standaloneSet() {
            return delegate.standaloneSet();
        }

        @Override
        public String getCharacterEncodingScheme() {
            return delegate.getCharacterEncodingScheme();
        }

        @Override
        public String getPITarget() {
            return delegate.getPITarget();
        }

        @Override
        public String getPIData() {
            return delegate.getPIData();
        }
    }
}
