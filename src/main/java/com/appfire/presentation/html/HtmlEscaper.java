package com.appfire.presentation.html;

public final class HtmlEscaper {

    private HtmlEscaper() {
    }

    public static String text(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public static String attr(String raw) {
        return text(raw);
    }
}
