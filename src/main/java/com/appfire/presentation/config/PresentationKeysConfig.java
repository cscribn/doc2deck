package com.appfire.presentation.config;

import com.appfire.presentation.model.PresentationKeys;
import java.util.Map;

public final class PresentationKeysConfig {

    static final String HEADER_KEY = "presentationKeys.header";

    private final String header;
    private final Map<String, String> instructions;

    public PresentationKeysConfig(String header, Map<String, String> instructions) {
        this.header = header;
        this.instructions = Map.copyOf(instructions);
    }

    public String header() {
        return header;
    }

    public Map<String, String> instructions() {
        return instructions;
    }

    public String formatForPrompt() {
        StringBuilder prompt = new StringBuilder(header.trim()).append("\n\n");
        for (String keyName : PresentationKeys.textKeys()) {
            String instruction = instructions.get(keyName);
            if (instruction == null || instruction.isBlank()) {
                continue;
            }
            prompt.append(keyName).append(" - ").append(instruction.trim()).append('\n');
        }
        return prompt.toString().stripTrailing();
    }
}
