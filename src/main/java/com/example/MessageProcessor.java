package com.example;

import java.util.HashMap;
import java.util.Map;

public class MessageProcessor {
    private static Map<String, String> replacementsMap = createReplacementsMap();

    public static String processMessage(String message) {
        for (Map.Entry<String, String> entry : replacementsMap.entrySet()) {
            message = message.replaceAll(entry.getKey(), entry.getValue());
        }
        return message;
    }

    private static Map<String, String> createReplacementsMap() {
        replacementsMap = new HashMap<>();
        
        // Value validation patterns
        replacementsMap.put("regular expression \\'\\[0-9\\]\\[0-9\\]\\'", "XX");
        replacementsMap.put("Value '.*' does not match .*", "Invalid value format");
        
        // Element/Content validation patterns - more specific patterns first
        replacementsMap.put(
            "ParseError at \\[row,col\\]:\\[\\d+,\\d+\\]\\s*Message: Element content can not contain child START_ELEMENT when using Typed Access methods",
            "Invalid element content - expected simple text value but found nested XML elements"
        );
        
        replacementsMap.put(
            "Element content can not contain child START_ELEMENT when using Typed Access methods", 
            "Invalid element content - expected simple value but found nested elements"
        );
        
        // Schema validation patterns
        replacementsMap.put(
            "Schema for node with name .* and namespace .* does not exist in parent .*",
            "Unknown element - not defined in the YANG schema"
        );
        
        replacementsMap.put(
            "Schema node with name .* was not found under .*",
            "Element not found in schema definition"
        );
        
        // Duplicate element patterns
        replacementsMap.put(
            "Duplicate element \".*\" in namespace \".*\" with parent \".*\" in XML input",
            "Duplicate element found - only one instance allowed"
        );
        
        // Namespace patterns
        replacementsMap.put(
            "Failed to convert namespace .*",
            "Invalid XML namespace"
        );
        
        replacementsMap.put(
            "Choose suitable module name for element .*:",
            "Ambiguous element name - multiple modules define this element"
        );
        
        // Mount point patterns
        replacementsMap.put(
            "Mount point .* not attached",
            "Mount point configuration missing"
        );
        
        replacementsMap.put(
            "Unhandled mount-aware schema .*",
            "Unsupported mount point schema type"
        );
        
        // Type conversion patterns
        replacementsMap.put(
            "Unexpected value while expecting a .*",
            "Invalid data type - value does not match expected type"
        );
        
        // XML parsing patterns - more specific first
        replacementsMap.put(
            "ParseError at \\[row,col\\]:\\[\\d+,\\d+\\]\\s*Message: .*",
            "XML structure error - invalid element nesting or content"
        );
        
        replacementsMap.put(
            "Unable to read anyxml value",
            "Failed to parse XML content"
        );
        
        // Value assignment patterns
        replacementsMap.put(
            "Node '.*' has already set its value to '.*'",
            "Duplicate value assignment - element value already set"
        );
        
        // Transformer patterns
        replacementsMap.put(
            "No TransformerFactory supporting StAXResult found",
            "XML processing configuration error"
        );
        
        // JSON parsing patterns (if used)
        replacementsMap.put(
            "Failed parsing JSON source: .* to Json",
            "JSON parsing failed"
        );
        
        // General patterns
        replacementsMap.put(
            "Illegal parent node .*",
            "Invalid parent element for this context"
        );
        
        replacementsMap.put(
            "Unexpected parent .*",
            "Element found in unexpected location"
        );
        
        // Codec patterns
        replacementsMap.put(
            "Codec for .* is not available",
            "Data type codec not available"
        );
        
        return replacementsMap;
    }
}
