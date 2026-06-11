package com.appfire.presentation.rehydration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class BulletFormatter {

    private static final Pattern BULLET_PREFIX = Pattern.compile(
            "^[\\s\\u2022\\u2023\\u25E6\\u2043\\-*·]+|\\d+[.)]\\s*");

    private BulletFormatter() {
    }

    public record BulletLine(String text, int indentLevel) {
    }

    public static List<BulletLine> parseBullets(List<String> bullets) {
        List<BulletLine> lines = new ArrayList<>();
        if (bullets == null) {
            return lines;
        }
        for (String bullet : bullets) {
            if (bullet == null || bullet.isBlank()) {
                continue;
            }
            parseBulletItem(bullet, lines);
        }
        return lines;
    }

    private static void parseBulletItem(String bullet, List<BulletLine> lines) {
        String[] parts = bullet.split("\n");
        for (int i = 0; i < parts.length; i++) {
            String line = parts[i];
            if (line.isBlank()) {
                continue;
            }
            int indent = isSubBulletLine(line) ? 1 : 0;
            String sanitized = sanitizeBulletLine(line);
            if (!sanitized.isBlank()) {
                lines.add(new BulletLine(sanitized, indent));
            }
        }
    }

    private static boolean isSubBulletLine(String line) {
        String trimmed = line.stripLeading();
        return line.length() - trimmed.length() >= 2
                || BULLET_PREFIX.matcher(trimmed).find();
    }

    public static String sanitizeBulletLine(String line) {
        if (line == null) {
            return "";
        }
        String trimmed = line.strip();
        String previous;
        String current = trimmed;
        do {
            previous = current;
            current = BULLET_PREFIX.matcher(current).replaceFirst("").strip();
        } while (!current.equals(previous) && !current.isEmpty());
        return current;
    }
}
