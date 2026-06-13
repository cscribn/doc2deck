package com.appfire.presentation.model;

public record KeyDefinition(
        String name,
        String instruction,
        int maxWords,
        boolean optional,
        KeyType type) {

    public boolean isImage() {
        return type == KeyType.IMAGE;
    }

    public boolean isText() {
        return type == KeyType.TEXT;
    }
}
