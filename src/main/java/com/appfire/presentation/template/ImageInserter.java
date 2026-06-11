package com.appfire.presentation.template;

import com.appfire.presentation.images.PresentationImageOptimizer;
import com.appfire.presentation.model.ImageKeyPlan;
import com.appfire.presentation.model.ImageKeyPlan.ResolvedImageKey;
import com.appfire.presentation.model.PresentationKeys;
import com.appfire.presentation.model.TemplateScanResult;
import com.appfire.presentation.model.TemplateScanResult.ImageKeyAnchor;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ImageInserter {

    private static final Logger LOG = LoggerFactory.getLogger(ImageInserter.class);

    private final PresentationImageOptimizer optimizer;
    private final EmbeddedFontCleaner fontCleaner;

    public ImageInserter(PresentationImageOptimizer optimizer, EmbeddedFontCleaner fontCleaner) {
        this.optimizer = optimizer;
        this.fontCleaner = fontCleaner;
    }

    public void insert(Path pptxPath, ImageKeyPlan imagePlan, TemplateScanResult scan, Path outputPath)
            throws IOException {
        Map<String, ImageKeyAnchor> anchorsByKey = mapAnchors(scan.imageAnchors());
        try (InputStream input = Files.newInputStream(pptxPath);
                XMLSlideShow slideShow = new XMLSlideShow(input)) {

            for (String key : PresentationKeys.imageKeys()) {
                ResolvedImageKey image = imagePlan.images().get(key);
                if (image == null) {
                    LOG.warn("No image acquired for key '{}'. Resolution: set PEXELS_API_KEY or check query.",
                            key);
                    continue;
                }
                ImageKeyAnchor anchor = anchorsByKey.get(key);
                if (anchor == null) {
                    anchor = findAnchorInDeck(slideShow, key);
                }
                if (anchor == null) {
                    LOG.error("Image key '${}' not found in template. Resolution: add placeholder to template.pptx",
                            key);
                    continue;
                }
                insertAtAnchor(slideShow, anchor, image);
            }

            if (optimizer.isEnabled()) {
                optimizer.optimize(slideShow);
            }
            writeOutput(slideShow, outputPath);
            fontCleaner.clean(outputPath);
        }
    }

    private Map<String, ImageKeyAnchor> mapAnchors(List<ImageKeyAnchor> anchors) {
        Map<String, ImageKeyAnchor> map = new HashMap<>();
        for (ImageKeyAnchor anchor : anchors) {
            map.putIfAbsent(anchor.key(), anchor);
        }
        return map;
    }

    private ImageKeyAnchor findAnchorInDeck(XMLSlideShow slideShow, String key) {
        String token = "${" + key + "}";
        List<XSLFSlide> slides = slideShow.getSlides();
        for (int slideIndex = 0; slideIndex < slides.size(); slideIndex++) {
            XSLFSlide slide = slides.get(slideIndex);
            for (XSLFShape shape : slide.getShapes()) {
                ImageKeyAnchor found = findInShape(shape, key, token, slideIndex);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private ImageKeyAnchor findInShape(XSLFShape shape, String key, String token, int slideIndex) {
        if (shape instanceof XSLFTextShape textShape) {
            String text = textShape.getText();
            if (text != null && text.contains(token)) {
                return new ImageKeyAnchor(key, slideIndex, textShape.getShapeId(), textShape.getAnchor());
            }
        } else if (shape instanceof XSLFTable table) {
            for (XSLFTableRow row : table.getRows()) {
                for (XSLFTableCell cell : row.getCells()) {
                    String text = cell.getText();
                    if (text != null && text.contains(token)) {
                        return new ImageKeyAnchor(key, slideIndex, cell.getShapeId(), cell.getAnchor());
                    }
                }
            }
        }
        return null;
    }

    private void insertAtAnchor(XMLSlideShow slideShow, ImageKeyAnchor anchor, ResolvedImageKey image) {
        List<XSLFSlide> slides = slideShow.getSlides();
        if (anchor.slideIndex() < 0 || anchor.slideIndex() >= slides.size()) {
            LOG.error("Invalid slide index {} for image key '{}'", anchor.slideIndex(), anchor.key());
            return;
        }
        XSLFSlide slide = slides.get(anchor.slideIndex());
        XSLFShape target = findShapeById(slide, anchor.shapeId());
        if (target == null) {
            LOG.error("Shape {} not found on slide {} for image key '{}'",
                    anchor.shapeId(), anchor.slideIndex(), anchor.key());
            return;
        }

        Rectangle2D bounds = anchor.anchor() != null ? anchor.anchor() : target.getAnchor();
        clearPlaceholderText(target, anchor.key());

        XSLFPictureShape picture = slide.createPicture(
                slideShow.addPicture(image.data(), image.pictureType()));
        picture.setAnchor(bounds);
        LOG.info("Inserted image for key '{}' on slide {}", anchor.key(), anchor.slideIndex());
    }

    private XSLFShape findShapeById(XSLFSlide slide, int shapeId) {
        for (XSLFShape shape : slide.getShapes()) {
            if (shape.getShapeId() == shapeId) {
                return shape;
            }
            if (shape instanceof XSLFTable table) {
                for (XSLFTableRow row : table.getRows()) {
                    for (XSLFTableCell cell : row.getCells()) {
                        if (cell.getShapeId() == shapeId) {
                            return cell;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void clearPlaceholderText(XSLFShape shape, String key) {
        String token = "${" + key + "}";
        if (shape instanceof XSLFTextShape textShape) {
            String text = textShape.getText();
            if (text != null && text.contains(token)) {
                textShape.clearText();
            }
        }
    }

    private void writeOutput(XMLSlideShow slideShow, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent() != null ? outputPath.getParent() : Path.of("."));
        try (OutputStream output = Files.newOutputStream(outputPath)) {
            slideShow.write(output);
        }
    }
}
