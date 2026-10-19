package com.appfire.presentation.template;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;

final class TemplateDesignLinter {

    private static final int MAX_FONT_FAMILIES = 3;
    private static final int MAX_USED_LAYOUTS = 3;
    private static final int MAX_NON_GREY_COLORS = 6;
    private static final Pattern SRGB_COLOR = Pattern.compile("srgbClr val=\"([0-9A-Fa-f]{6})\"");
    private static final Pattern LATIN_TYPEFACE = Pattern.compile("<a:latin typeface=\"([^\"]+)\"");

    private TemplateDesignLinter() {
    }

    static List<String> lint(XMLSlideShow slideShow) throws Exception {
        List<String> warnings = new ArrayList<>();
        OPCPackage pkg = slideShow.getPackage();

        Set<String> fonts = fontFamilies(pkg);
        if (fonts.size() > MAX_FONT_FAMILIES) {
            warnings.add("Template uses " + fonts.size() + " font families " + fonts
                    + "; best practice is 1-" + MAX_FONT_FAMILIES + " (sans-serif body).");
        }

        Map<String, String> layouts = usedLayoutNamesByPart(slideShow);
        if (layouts.size() > MAX_USED_LAYOUTS) {
            warnings.add("Template slides use " + layouts.size() + " layouts " + layouts.values()
                    + "; best practice is 2-" + MAX_USED_LAYOUTS + " (title + blank).");
        }

        Set<String> colors = nonGreyColors(pkg);
        if (colors.size() > MAX_NON_GREY_COLORS) {
            warnings.add("Template uses " + colors.size() + " non-grey RGB colors " + colors
                    + "; best practice is max " + MAX_NON_GREY_COLORS + " plus black, white, greys.");
        }
        return warnings;
    }

    private static boolean isDesignPart(String partName) {
        return partName.startsWith("/ppt/slides/")
                || partName.startsWith("/ppt/slideLayouts/")
                || partName.startsWith("/ppt/slideMasters/");
    }

    private static Set<String> fontFamilies(OPCPackage pkg) throws Exception {
        Set<String> fonts = new TreeSet<>();
        for (String xml : designPartXml(pkg)) {
            Matcher matcher = LATIN_TYPEFACE.matcher(xml);
            while (matcher.find()) {
                String font = matcher.group(1);
                if (!font.isBlank() && !font.startsWith("+")) {
                    fonts.add(font);
                }
            }
        }
        return fonts;
    }

    private static Map<String, String> usedLayoutNamesByPart(XMLSlideShow slideShow) {
        Map<String, String> layoutNamesByPart = new TreeMap<>();
        for (XSLFSlide slide : slideShow.getSlides()) {
            XSLFSlideLayout layout = slide.getSlideLayout();
            if (layout != null) {
                layoutNamesByPart.put(layout.getPackagePart().getPartName().getName(), layout.getName());
            }
        }
        return layoutNamesByPart;
    }

    private static Set<String> nonGreyColors(OPCPackage pkg) throws Exception {
        Set<String> colors = new TreeSet<>();
        for (String xml : designPartXml(pkg)) {
            Matcher matcher = SRGB_COLOR.matcher(xml);
            while (matcher.find()) {
                String hex = matcher.group(1).toUpperCase(Locale.ROOT);
                if (!isGrey(hex)) {
                    colors.add(hex);
                }
            }
        }
        return colors;
    }

    private static List<String> designPartXml(OPCPackage pkg) throws Exception {
        List<String> xml = new ArrayList<>();
        for (PackagePart part : pkg.getParts()) {
            if (isDesignPart(part.getPartName().getName())) {
                xml.add(ReferencedFontCollector.readPart(part));
            }
        }
        return xml;
    }

    private static boolean isGrey(String hex) {
        return hex.substring(0, 2).equals(hex.substring(2, 4)) && hex.substring(2, 4).equals(hex.substring(4, 6));
    }
}
