package com.appfire.presentation.model;

import java.util.List;

public record ContentBlock(
        int index,
        BlockType type,
        int headingLevel,
        String text,
        List<String> items) {

    public ContentBlock(int index, BlockType type, String text) {
        this(index, type, 0, text, List.of());
    }

    public ContentBlock(int index, BlockType type, int headingLevel, String text) {
        this(index, type, headingLevel, text, List.of());
    }

    public ContentBlock(int index, BlockType type, List<String> items) {
        this(index, type, 0, "", items);
    }
}
