package com.appfire.presentation.rehydration;

import com.appfire.presentation.model.ContentStyle;
import com.appfire.presentation.model.ExtractedPresentation;
import com.appfire.presentation.model.GenerationResponse;
import com.appfire.presentation.model.ImagePlan;
import com.appfire.presentation.model.MediaLayoutSpec;
import com.appfire.presentation.model.ResolvedImage;
import com.appfire.presentation.model.ShapeBlueprint;
import com.appfire.presentation.model.SlideAction;
import com.appfire.presentation.model.SlideBlueprint;
import com.appfire.presentation.model.SlideDirective;
import com.appfire.presentation.rehydration.LayoutResolver.ResolvedLayout;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PptxRehydrator {

    private static final Logger LOG = LoggerFactory.getLogger(PptxRehydrator.class);

    private final LayoutResolver layoutResolver;

    public PptxRehydrator(LayoutResolver layoutResolver) {
        this.layoutResolver = layoutResolver;
    }

    public void rehydrate(
            ExtractedPresentation extracted,
            GenerationResponse response,
            ImagePlan imagePlan,
            Path outputPath) throws IOException {
        XMLSlideShow slideShow = extracted.slideShow();
        for (SlideDirective directive : response.slides()) {
            applyDirective(slideShow, extracted, directive, imagePlan);
        }
        writeOutput(slideShow, outputPath);
    }

    private void applyDirective(
            XMLSlideShow slideShow,
            ExtractedPresentation extracted,
            SlideDirective directive,
            ImagePlan imagePlan) {
        if (directive.action() == SlideAction.SKIP) {
            return;
        }
        ResolvedLayout resolved = layoutResolver.resolve(directive);
        XSLFSlide slide = resolveSlide(slideShow, directive, resolved);
        if (slide == null) {
            LOG.error("Could not resolve slide for index {}. Resolution: check template layouts.",
                    directive.slideIndex());
            return;
        }
        SlideBlueprint blueprint = findBlueprint(extracted, directive.slideIndex());
        MediaLayoutSpec mediaSpec = resolved.mediaSpec();
        applyContent(slide, directive, blueprint, mediaSpec);
        insertImage(slideShow, slide, directive, imagePlan, mediaSpec);
    }

    private XSLFSlide resolveSlide(
            XMLSlideShow slideShow,
            SlideDirective directive,
            ResolvedLayout resolved) {
        String layoutName = resolved.layout() != null ? resolved.layout().name() : directive.layoutName();
        if (directive.action() == SlideAction.APPEND) {
            return createSlideWithLayoutName(slideShow, layoutName);
        }
        return replaceSlideWithLayoutName(slideShow, directive, layoutName);
    }

    private XSLFSlide replaceSlideWithLayoutName(
            XMLSlideShow slideShow,
            SlideDirective directive,
            String layoutName) {
        List<XSLFSlide> slides = slideShow.getSlides();
        if (directive.slideIndex() < 0 || directive.slideIndex() >= slides.size()) {
            return null;
        }
        XSLFSlide current = slides.get(directive.slideIndex());
        XSLFSlideLayout desired = findLayoutByName(slideShow, layoutName);
        if (desired == null || sameLayout(current, desired)) {
            return current;
        }
        XSLFSlide replacement = slideShow.createSlide(desired);
        slideShow.removeSlide(directive.slideIndex());
        slideShow.setSlideOrder(replacement, directive.slideIndex());
        return replacement;
    }

    private boolean sameLayout(XSLFSlide slide, XSLFSlideLayout layout) {
        if (slide.getSlideLayout() == null || layout == null) {
            return false;
        }
        String current = slide.getSlideLayout().getName();
        String desired = layout.getName();
        return current != null && desired != null && current.equalsIgnoreCase(desired);
    }

    private XSLFSlide createSlideWithLayoutName(XMLSlideShow slideShow, String layoutName) {
        XSLFSlideLayout layout = findLayoutByName(slideShow, layoutName);
        if (layout == null) {
            LOG.error("No layout '{}' found for append. Resolution: check template master.", layoutName);
            return null;
        }
        return slideShow.createSlide(layout);
    }

    private XSLFSlideLayout findLayoutByName(XMLSlideShow slideShow, String name) {
        if (slideShow.getSlideMasters().isEmpty() || name == null || name.isBlank()) {
            return fallbackLayout(slideShow);
        }
        for (XSLFSlideLayout layout : slideShow.getSlideMasters().get(0).getSlideLayouts()) {
            if (layout.getName() != null && layout.getName().equalsIgnoreCase(name)) {
                return layout;
            }
        }
        return fallbackLayout(slideShow);
    }

    private XSLFSlideLayout fallbackLayout(XMLSlideShow slideShow) {
        XSLFSlideLayout[] layouts = slideShow.getSlideMasters().get(0).getSlideLayouts();
        return layouts.length > 1 ? layouts[1] : layouts[0];
    }

    private SlideBlueprint findBlueprint(ExtractedPresentation extracted, int slideIndex) {
        return extracted.blueprint().slides().stream()
                .filter(s -> s.slideIndex() == slideIndex)
                .findFirst()
                .orElse(null);
    }

    private void applyContent(
            XSLFSlide slide,
            SlideDirective directive,
            SlideBlueprint blueprint,
            MediaLayoutSpec mediaSpec) {
        if (mediaSpec != null && mediaSpec.hasMediaRegion()) {
            applySplitLayoutContent(slide, directive, blueprint, mediaSpec);
            return;
        }

        setPlaceholderText(slide, Placeholder.TITLE, directive.title(), blueprint, true);
        if (directive.contentStyle() == ContentStyle.TITLE_ONLY) {
            clearBodyPlaceholder(slide);
            return;
        }
        if (directive.contentStyle() == ContentStyle.TWO_COLUMN) {
            applyTwoColumnContent(slide, directive, blueprint);
            return;
        }
        if (directive.contentStyle() == ContentStyle.BODY
                || (directive.bodyText() != null && !directive.bodyText().isBlank())) {
            setPlainBodyText(slide, directive.bodyText(), blueprint);
            return;
        }
        setBulletContent(slide, directive.bullets(), blueprint);
    }

    private void applySplitLayoutContent(
            XSLFSlide slide,
            SlideDirective directive,
            SlideBlueprint blueprint,
            MediaLayoutSpec mediaSpec) {
        clearBodyPlaceholder(slide);
        Optional<XSLFTextShape> textShape = findTextShape(slide, Placeholder.TITLE, blueprint, true);
        if (textShape.isEmpty()) {
            textShape = findLargestTextShapeInRegion(slide, mediaSpec);
        }
        if (textShape.isEmpty()) {
            LOG.error("Slide {} has no text region for media layout.", slide.getSlideNumber() - 1);
            return;
        }

        XSLFTextShape shape = textShape.get();
        shape.setAnchor(new Rectangle2D.Double(
                mediaSpec.textX(), mediaSpec.textY(),
                mediaSpec.textWidth(), mediaSpec.textHeight()));

        if (directive.contentStyle() == ContentStyle.BODY
                || (directive.bodyText() != null && !directive.bodyText().isBlank())) {
            writePlainText(shape, directive.title() + "\n\n" + directive.bodyText(), blueprint, false);
            return;
        }
        writeTitleAndBullets(shape, directive.title(), directive.bullets(), blueprint);
    }

    private Optional<XSLFTextShape> findLargestTextShapeInRegion(XSLFSlide slide, MediaLayoutSpec spec) {
        XSLFTextShape best = null;
        double bestArea = 0;
        for (var shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                Rectangle2D anchor = textShape.getAnchor();
                double area = anchor.getWidth() * anchor.getHeight();
                if (area > bestArea) {
                    bestArea = area;
                    best = textShape;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private void writeTitleAndBullets(
            XSLFTextShape textShape,
            String title,
            List<String> bullets,
            SlideBlueprint blueprint) {
        textShape.clearText();
        XSLFTextParagraph titleParagraph = textShape.addNewTextParagraph();
        XSLFTextRun titleRun = titleParagraph.addNewTextRun();
        titleRun.setText(title);
        titleRun.setBold(true);
        applyStyle(titleRun, blueprint, "TITLE", true);

        List<BulletFormatter.BulletLine> lines = BulletFormatter.parseBullets(bullets);
        for (BulletFormatter.BulletLine line : lines) {
            XSLFTextParagraph paragraph = textShape.addNewTextParagraph();
            paragraph.setBullet(true);
            paragraph.setIndentLevel(line.indentLevel());
            XSLFTextRun run = paragraph.addNewTextRun();
            run.setText(line.text());
            applyStyle(run, blueprint, "BODY", false);
        }
    }

    private void applyTwoColumnContent(XSLFSlide slide, SlideDirective directive, SlideBlueprint blueprint) {
        List<XSLFTextShape> bodyShapes = findBodyPlaceholders(slide);
        if (bodyShapes.size() >= 2) {
            setBulletContent(bodyShapes.get(0), directive.leftBullets(), blueprint);
            setBulletContent(bodyShapes.get(1), directive.rightBullets(), blueprint);
            return;
        }
        if (!bodyShapes.isEmpty()) {
            List<String> combined = new ArrayList<>(directive.leftBullets());
            combined.addAll(directive.rightBullets());
            setBulletContent(bodyShapes.get(0), combined, blueprint);
        }
    }

    private List<XSLFTextShape> findBodyPlaceholders(XSLFSlide slide) {
        List<XSLFTextShape> bodies = new ArrayList<>();
        for (var shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                Placeholder ph = textShape.getPlaceholder();
                if (ph == Placeholder.BODY || ph == Placeholder.CONTENT) {
                    bodies.add(textShape);
                }
            }
        }
        return bodies;
    }

    private void clearBodyPlaceholder(XSLFSlide slide) {
        findBodyPlaceholders(slide).forEach(XSLFTextShape::clearText);
    }

    private void setPlainBodyText(XSLFSlide slide, String bodyText, SlideBlueprint blueprint) {
        if (bodyText == null || bodyText.isBlank()) {
            return;
        }
        findTextShape(slide, Placeholder.BODY, blueprint, false)
                .ifPresent(shape -> writePlainText(shape, bodyText, blueprint, false));
    }

    private void setBulletContent(XSLFSlide slide, List<String> bullets, SlideBlueprint blueprint) {
        findTextShape(slide, Placeholder.BODY, blueprint, false)
                .ifPresent(shape -> setBulletContent(shape, bullets, blueprint));
    }

    private void setBulletContent(XSLFTextShape textShape, List<String> bullets, SlideBlueprint blueprint) {
        List<BulletFormatter.BulletLine> lines = BulletFormatter.parseBullets(bullets);
        if (lines.isEmpty()) {
            textShape.clearText();
            return;
        }
        textShape.clearText();
        for (BulletFormatter.BulletLine line : lines) {
            XSLFTextParagraph paragraph = textShape.addNewTextParagraph();
            paragraph.setBullet(true);
            paragraph.setIndentLevel(line.indentLevel());
            XSLFTextRun run = paragraph.addNewTextRun();
            run.setText(line.text());
            applyStyle(run, blueprint, "BODY", false);
        }
    }

    private void writePlainText(
            XSLFTextShape textShape,
            String text,
            SlideBlueprint blueprint,
            boolean isTitle) {
        textShape.clearText();
        XSLFTextParagraph paragraph = textShape.addNewTextParagraph();
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(text);
        applyStyle(run, blueprint, isTitle ? "TITLE" : "BODY", isTitle);
    }

    private void setPlaceholderText(
            XSLFSlide slide,
            Placeholder placeholder,
            String text,
            SlideBlueprint blueprint,
            boolean isTitle) {
        findTextShape(slide, placeholder, blueprint, isTitle)
                .ifPresent(shape -> writePlainText(shape, text, blueprint, isTitle));
    }

    private void insertImage(
            XMLSlideShow slideShow,
            XSLFSlide slide,
            SlideDirective directive,
            ImagePlan imagePlan,
            MediaLayoutSpec mediaSpec) {
        if (imagePlan == null) {
            return;
        }
        ResolvedImage image = imagePlan.forSlide(directive.slideIndex());
        if (image == null || image.data() == null || image.data().length == 0) {
            return;
        }

        Rectangle2D anchor = resolveMediaAnchor(slide, mediaSpec);
        XSLFPictureData pictureData = slideShow.addPicture(image.data(), image.pictureType());
        XSLFPictureShape picture = slide.createPicture(pictureData);
        picture.setAnchor(anchor);
        LOG.debug("Inserted image on slide {} at [{}, {} {}x{}]",
                directive.slideIndex(), anchor.getX(), anchor.getY(),
                anchor.getWidth(), anchor.getHeight());
    }

    private Rectangle2D resolveMediaAnchor(XSLFSlide slide, MediaLayoutSpec mediaSpec) {
        if (mediaSpec != null && mediaSpec.hasMediaRegion()) {
            return new Rectangle2D.Double(
                    mediaSpec.mediaX(), mediaSpec.mediaY(),
                    mediaSpec.mediaWidth(), mediaSpec.mediaHeight());
        }
        for (var shape : slide.getSlideLayout().getShapes()) {
            if (shape.getPlaceholder() == Placeholder.PICTURE) {
                return shape.getAnchor();
            }
        }
        LOG.warn("No media region for slide {}. Resolution: use a split media layout from the template.",
                slide.getSlideNumber() - 1);
        double width = slide.getSlideShow().getPageSize().getWidth();
        double height = slide.getSlideShow().getPageSize().getHeight();
        return new Rectangle2D.Double(width * 0.55, height * 0.15, width * 0.4, height * 0.7);
    }

    private Optional<XSLFTextShape> findTextShape(
            XSLFSlide slide,
            Placeholder placeholder,
            SlideBlueprint blueprint,
            boolean isTitle) {
        Optional<XSLFTextShape> direct = findPlaceholder(slide, placeholder);
        if (direct.isPresent()) {
            return direct;
        }
        return findByPosition(slide, isTitle);
    }

    private Optional<XSLFTextShape> findPlaceholder(XSLFSlide slide, Placeholder placeholder) {
        if (slide.getPlaceholder(placeholder) instanceof XSLFTextShape direct) {
            return Optional.of(direct);
        }
        for (var shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape
                    && textShape.getPlaceholder() == placeholder) {
                return Optional.of(textShape);
            }
        }
        return Optional.empty();
    }

    private Optional<XSLFTextShape> findByPosition(XSLFSlide slide, boolean isTitle) {
        List<XSLFTextShape> textShapes = slide.getShapes().stream()
                .filter(XSLFTextShape.class::isInstance)
                .map(XSLFTextShape.class::cast)
                .toList();
        if (textShapes.isEmpty()) {
            return Optional.empty();
        }
        int index = isTitle ? 0 : Math.min(1, textShapes.size() - 1);
        return Optional.of(textShapes.get(index));
    }

    private void applyStyle(XSLFTextRun run, SlideBlueprint blueprint, String placeholderType, boolean isTitle) {
        ShapeBlueprint shapeStyle = findShapeStyle(blueprint, placeholderType);
        if (shapeStyle == null) {
            return;
        }
        if (shapeStyle.fontFamily() != null) {
            run.setFontFamily(shapeStyle.fontFamily());
        }
        if (shapeStyle.fontSizePt() != null) {
            run.setFontSize(shapeStyle.fontSizePt());
        }
        if (shapeStyle.colorHex() != null) {
            run.setFontColor(parseColor(shapeStyle.colorHex()));
        }
        if (isTitle && shapeStyle.fontSizePt() == null) {
            run.setFontSize(32.0);
        }
        if (!isTitle && shapeStyle.fontSizePt() == null) {
            run.setFontSize(18.0);
        }
    }

    private ShapeBlueprint findShapeStyle(SlideBlueprint blueprint, String placeholderType) {
        if (blueprint == null) {
            return null;
        }
        return blueprint.shapes().stream()
                .filter(s -> s.placeholderType().equals(placeholderType))
                .findFirst()
                .orElse(blueprint.shapes().isEmpty() ? null : blueprint.shapes().get(0));
    }

    private Color parseColor(String hex) {
        String normalized = hex.startsWith("#") ? hex.substring(1) : hex;
        int rgb = Integer.parseInt(normalized, 16);
        return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    private void writeOutput(XMLSlideShow slideShow, Path outputPath) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream out = Files.newOutputStream(outputPath)) {
            slideShow.write(out);
        }
        LOG.info("Wrote presentation to {}", outputPath);
    }
}
