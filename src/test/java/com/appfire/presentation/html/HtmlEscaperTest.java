package com.appfire.presentation.html;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HtmlEscaperTest {

    @Test
    void escapesHtmlSpecialCharacters() {
        assertEquals("&lt;script&gt;", HtmlEscaper.text("<script>"));
        assertEquals("a &amp; b", HtmlEscaper.text("a & b"));
        assertEquals("&quot;x&quot;", HtmlEscaper.attr("\"x\""));
    }

    @Test
    void handlesNullAsEmpty() {
        assertEquals("", HtmlEscaper.text(null));
    }
}
