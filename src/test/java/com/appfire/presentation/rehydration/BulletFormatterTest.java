package com.appfire.presentation.rehydration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BulletFormatterTest {

    @Test
    void stripsLeadingBulletCharacters() {
        assertEquals("Sub point", BulletFormatter.sanitizeBulletLine("  • Sub point"));
        assertEquals("Item", BulletFormatter.sanitizeBulletLine("- Item"));
        assertEquals("Item", BulletFormatter.sanitizeBulletLine("* Item"));
        assertEquals("Item", BulletFormatter.sanitizeBulletLine("1. Item"));
    }

    @Test
    void parsesSubBulletsWithIndent() {
        List<BulletFormatter.BulletLine> lines = BulletFormatter.parseBullets(
                List.of("Main point\n  • Sub one\n  • Sub two"));
        assertEquals(3, lines.size());
        assertEquals(0, lines.get(0).indentLevel());
        assertEquals(1, lines.get(1).indentLevel());
        assertEquals("Sub one", lines.get(1).text());
    }

    @Test
    void removesDuplicateBulletPrefixFromGeminiOutput() {
        List<BulletFormatter.BulletLine> lines = BulletFormatter.parseBullets(
                List.of("• Already prefixed"));
        assertEquals(1, lines.size());
        assertEquals("Already prefixed", lines.get(0).text());
    }
}
