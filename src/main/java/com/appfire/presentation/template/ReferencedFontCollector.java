package com.appfire.presentation.template;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.util.IOUtils;

final class ReferencedFontCollector {

    private static final Pattern TYPEFACE = Pattern.compile("typeface=\"([^\"]+)\"");

    private ReferencedFontCollector() {
    }

    static Set<String> collect(OPCPackage pkg) throws Exception {
        Set<String> fonts = new HashSet<>();
        for (PackagePart part : pkg.getParts()) {
            if (!isContentPart(part.getPartName().getName())) {
                continue;
            }
            collectFromXml(readPart(part), fonts);
        }
        return fonts;
    }

    private static boolean isContentPart(String partName) {
        return partName.startsWith("/ppt/slides/")
                || partName.startsWith("/ppt/slideMasters/")
                || partName.startsWith("/ppt/slideLayouts/")
                || partName.startsWith("/ppt/notesMasters/")
                || partName.startsWith("/ppt/notesSlides/")
                || partName.startsWith("/ppt/theme/");
    }

    private static void collectFromXml(String xml, Set<String> fonts) {
        Matcher matcher = TYPEFACE.matcher(xml);
        while (matcher.find()) {
            fonts.add(matcher.group(1));
        }
    }

    private static String readPart(PackagePart part) throws IOException {
        try (InputStream input = part.getInputStream()) {
            return new String(IOUtils.toByteArray(input), StandardCharsets.UTF_8);
        }
    }
}
