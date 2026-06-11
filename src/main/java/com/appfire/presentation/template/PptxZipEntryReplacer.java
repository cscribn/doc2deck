package com.appfire.presentation.template;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

final class PptxZipEntryReplacer {

    private PptxZipEntryReplacer() {
    }

    static void replaceEntry(Path pptxPath, String entryName, byte[] content) throws IOException {
        Path tempPath = Files.createTempFile(
                pptxPath.getParent() != null ? pptxPath.getParent() : Path.of("."),
                "pptx-entry-",
                ".zip");
        boolean replaced = false;
        byte[] buffer = new byte[8192];
        try (ZipFile zipFile = new ZipFile(pptxPath.toFile());
                ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(tempPath));
                InputStream replacement = new java.io.ByteArrayInputStream(content)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                ZipEntry copy = new ZipEntry(entry.getName());
                output.putNextEntry(copy);
                if (entryName.equals(entry.getName())) {
                    replacement.transferTo(output);
                    replaced = true;
                } else {
                    try (InputStream input = zipFile.getInputStream(entry)) {
                        int read;
                        while ((read = input.read(buffer)) != -1) {
                            output.write(buffer, 0, read);
                        }
                    }
                }
                output.closeEntry();
            }
        }
        if (!replaced) {
            Files.deleteIfExists(tempPath);
            throw new IOException("Zip entry not found: " + entryName);
        }
        Files.move(tempPath, pptxPath, StandardCopyOption.REPLACE_EXISTING);
    }
}
