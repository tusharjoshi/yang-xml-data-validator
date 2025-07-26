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
        replacementsMap.put("regular expression \\'\\[0-9\\]\\[0-9\\]\\'", "XX");
        return replacementsMap;
    }
}
