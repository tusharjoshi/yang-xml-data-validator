package com.example;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * XMLStreamReader that tracks the XPath of elements
 */
public class XPathXMLStreamReader implements XMLStreamReader {
    private final XMLStreamReader delegate;
    private final Stack<ElementInfo> elementStack = new Stack<>();
    private final Stack<Map<String, Integer>> elementCountStack = new Stack<>();
    private String currentXPath = "/";
    private final Map<String, String> xpathMap = new HashMap<>();
    private int elementCounter = 0; // Counter to ensure unique keys
    
    private static class ElementInfo {
        String name;
        int index;
        boolean hasMultipleOccurrences;
        
        ElementInfo(String name, int index, boolean hasMultipleOccurrences) {
            this.name = name;
            this.index = index;
            this.hasMultipleOccurrences = hasMultipleOccurrences;
        }
    }

    public XPathXMLStreamReader(XMLStreamReader delegate) {
        this.delegate = delegate;
        // Initialize with root level element count map
        elementCountStack.push(new HashMap<>());
    }

    public Map<String, String> getXpathMap() {
        return xpathMap;
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
        return delegate.nextTag();
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

    private void updateXPath(int event) {
        switch (event) {
            case XMLStreamConstants.START_ELEMENT:
                String elementName = delegate.getLocalName();
                
                // Get current level element counts
                Map<String, Integer> currentCounts = elementCountStack.peek();
                
                // Update count for this element
                int count = currentCounts.getOrDefault(elementName, 0);
                currentCounts.put(elementName, count + 1);
                
                // If this is the second occurrence, we need to retroactively update previous entries
                if (count == 1) {
                    // This is the second occurrence, so now we know there are multiple
                    updatePreviousXPathsForElement(elementName);
                }
                
                // Determine if this element has multiple occurrences
                boolean hasMultiple = count > 0; // If count > 0, we've seen this element before
                
                // Push element info to the stack
                elementStack.push(new ElementInfo(elementName, count, hasMultiple));
                
                // Create new level for child elements
                elementCountStack.push(new HashMap<>());
                break;
                
            case XMLStreamConstants.END_ELEMENT:
                if (!elementStack.isEmpty()) {
                    elementStack.pop();
                    elementCountStack.pop();
                }
                break;
        }
        buildXPath();
        
        // Create a unique key: lineNumber + elementCounter + elementName (for START_ELEMENT only)
        if (event == XMLStreamConstants.START_ELEMENT) {
            elementCounter++;
            String elementName = delegate.getLocalName();
            Integer lineNumber = delegate.getLocation().getLineNumber();
            String uniqueKey = lineNumber + ":" + elementCounter + ":" + elementName;
            xpathMap.put(uniqueKey, currentXPath);
        }
    }
    
    private void updatePreviousXPathsForElement(String elementName) {
        // When we encounter the second occurrence of an element, 
        // retroactively update all previous XPath entries to add [0] to the first occurrence
        String currentPath = getCurrentPathWithoutIndices();
        
        for (Map.Entry<String, String> entry : xpathMap.entrySet()) {
            String xpath = entry.getValue();
            if (xpath.contains(currentPath + elementName)) {
                // Check if this xpath refers to the first occurrence (without index)
                String pattern = currentPath + elementName;
                if (xpath.equals(pattern) || xpath.startsWith(pattern + "/")) {
                    // Replace the element name with element name + [0]
                    String updatedXPath = xpath.replace(pattern, pattern + "[0]");
                    xpathMap.put(entry.getKey(), updatedXPath);
                }
            }
        }
    }
    
    private String getCurrentPathWithoutIndices() {
        if (elementStack.isEmpty()) {
            return "/";
        } else {
            StringBuilder xpath = new StringBuilder("/");
            for (int i = 0; i < elementStack.size(); i++) {
                if (i > 0) {
                    xpath.append("/");
                }
                ElementInfo element = elementStack.get(i);
                xpath.append(element.name);
                // Don't add indices here, we want the raw path
            }
            return xpath.toString() + "/";
        }
    }

    private void buildXPath() {
        if (elementStack.isEmpty()) {
            currentXPath = "/";
        } else {
            StringBuilder xpath = new StringBuilder("/");
            for (int i = 0; i < elementStack.size(); i++) {
                if (i > 0) {
                    xpath.append("/");
                }
                ElementInfo element = elementStack.get(i);
                xpath.append(element.name);
                
                // Add index if this element has multiple occurrences OR if it's the current element being processed
                // and we know it will have siblings (count > 0)
                if (element.hasMultipleOccurrences || element.index > 0) {
                    xpath.append("[").append(element.index).append("]");
                }
            }
            currentXPath = xpath.toString();
        }
    }
}
