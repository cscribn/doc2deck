package com.appfire.presentation.extraction;

import com.appfire.presentation.model.BlockType;
import com.appfire.presentation.model.ContentBlock;
import com.appfire.presentation.model.DocumentContent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DocxExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(DocxExtractor.class);

    public DocumentContent extract(Path docxPath) throws IOException {
        try (InputStream input = Files.newInputStream(docxPath);
                XWPFDocument document = new XWPFDocument(input)) {
            List<ContentBlock> blocks = new ArrayList<>();
            int index = 0;
            for (IBodyElement element : document.getBodyElements()) {
                index = appendElement(blocks, element, index);
            }
            String summary = buildFlatSummary(blocks);
            LOG.info("Extracted {} content blocks from {}", blocks.size(), docxPath);
            return new DocumentContent(blocks, summary);
        }
    }

    private int appendElement(List<ContentBlock> blocks, IBodyElement element, int index) {
        if (element instanceof XWPFParagraph paragraph) {
            return appendParagraph(blocks, paragraph, index);
        }
        if (element instanceof XWPFTable table) {
            return appendTable(blocks, table, index);
        }
        return index;
    }

    private int appendParagraph(List<ContentBlock> blocks, XWPFParagraph paragraph, int index) {
        String text = paragraph.getText().trim();
        if (text.isEmpty()) {
            return index;
        }
        if (paragraph.getNumID() != null) {
            blocks.add(new ContentBlock(index++, BlockType.LIST, List.of(text)));
            return index;
        }
        int level = paragraph.getStyle() != null && paragraph.getStyle().startsWith("Heading")
                ? parseHeadingLevel(paragraph.getStyle())
                : 0;
        if (level > 0) {
            blocks.add(new ContentBlock(index++, BlockType.HEADING, level, text));
        } else {
            blocks.add(new ContentBlock(index++, BlockType.PARAGRAPH, text));
        }
        return index;
    }

    private int appendTable(List<ContentBlock> blocks, XWPFTable table, int index) {
        List<String> rows = new ArrayList<>();
        for (XWPFTableRow row : table.getRows()) {
            List<String> cells = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                cells.add(cell.getText().trim());
            }
            rows.add(String.join(" | ", cells));
        }
        blocks.add(new ContentBlock(index++, BlockType.TABLE, rows));
        return index;
    }

    private int parseHeadingLevel(String style) {
        for (int i = 1; i <= 6; i++) {
            if (style.equalsIgnoreCase("Heading" + i)) {
                return i;
            }
        }
        return 1;
    }

    private String buildFlatSummary(List<ContentBlock> blocks) {
        StringBuilder builder = new StringBuilder();
        for (ContentBlock block : blocks) {
            appendBlockText(builder, block);
        }
        return builder.toString().trim();
    }

    private void appendBlockText(StringBuilder builder, ContentBlock block) {
        switch (block.type()) {
            case HEADING -> builder.append("#".repeat(block.headingLevel()))
                    .append(" ")
                    .append(block.text())
                    .append("\n");
            case PARAGRAPH -> builder.append(block.text()).append("\n");
            case LIST -> block.items().forEach(item -> builder.append("- ").append(item).append("\n"));
            case TABLE -> block.items().forEach(row -> builder.append(row).append("\n"));
        }
    }
}
