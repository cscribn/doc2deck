package com.appfire.presentation.extraction;

import com.appfire.presentation.model.ImagePosition;
import com.appfire.presentation.model.LayoutBlueprint;
import com.appfire.presentation.model.LayoutCatalog;
import com.appfire.presentation.model.MediaLayoutSpec;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

final class MediaLayoutDetector {

    private static final double MIN_MEDIA_WIDTH_RATIO = 0.25;
    private static final double MIN_MEDIA_HEIGHT_RATIO = 0.35;

    private MediaLayoutDetector() {
    }

    static List<MediaLayoutSpec> detect(List<LayoutBlueprint> layouts, XSLFSlideLayout[] slideLayouts) {
        List<MediaLayoutSpec> specs = new ArrayList<>();
        for (XSLFSlideLayout layout : slideLayouts) {
            specs.addAll(detectForLayout(layout));
        }
        return deduplicate(specs);
    }

    private static List<MediaLayoutSpec> detectForLayout(XSLFSlideLayout layout) {
        List<MediaLayoutSpec> specs = new ArrayList<>();
        String name = layout.getName() != null ? layout.getName() : "unknown";
        double slideW = layout.getSlideShow().getPageSize().getWidth();
        double slideH = layout.getSlideShow().getPageSize().getHeight();

        Rectangle2D title = null;
        Rectangle2D body = null;
        for (XSLFShape shape : layout.getShapes()) {
            if (!(shape instanceof XSLFTextShape textShape)) {
                continue;
            }
            Placeholder ph = textShape.getPlaceholder();
            if (ph == Placeholder.TITLE || ph == Placeholder.CENTERED_TITLE) {
                title = shape.getAnchor();
            } else if (ph == Placeholder.BODY || ph == Placeholder.CONTENT) {
                body = shape.getAnchor();
            }
        }

        if (title != null && body != null && title.getX() + title.getWidth() <= body.getX() + 1) {
            specs.add(new MediaLayoutSpec(
                    name, ImagePosition.RIGHT,
                    body.getX(), body.getY(), body.getWidth(), body.getHeight(),
                    title.getX(), title.getY(), title.getWidth(), title.getHeight(), true));
            specs.add(mirrorLeft(name, title, body));
        } else if (title != null && body != null && body.getX() + body.getWidth() <= title.getX() + 1) {
            specs.add(new MediaLayoutSpec(
                    name, ImagePosition.LEFT,
                    body.getX(), body.getY(), body.getWidth(), body.getHeight(),
                    title.getX(), title.getY(), title.getWidth(), title.getHeight(), true));
        } else if (title != null && title.getWidth() < slideW * 0.75) {
            double mediaX = title.getX() + title.getWidth() + 10;
            double mediaW = slideW - mediaX - title.getX();
            if (mediaW >= slideW * MIN_MEDIA_WIDTH_RATIO) {
                specs.add(new MediaLayoutSpec(
                        name, ImagePosition.RIGHT,
                        mediaX, title.getY(), mediaW, title.getHeight(),
                        title.getX(), title.getY(), title.getWidth(), title.getHeight(), true));
                specs.add(new MediaLayoutSpec(
                        name, ImagePosition.LEFT,
                        title.getX(), title.getY(), title.getWidth(), title.getHeight(),
                        mediaX, title.getY(), mediaW, title.getHeight(), true));
            }
        } else if (title != null && title.getY() > slideH * 0.15) {
            specs.add(new MediaLayoutSpec(
                    name, ImagePosition.TOP,
                    title.getX(), 0, title.getWidth(), title.getY() - 10,
                    title.getX(), title.getY(), title.getWidth(), title.getHeight(), true));
        }

        return specs;
    }

    private static MediaLayoutSpec mirrorLeft(String name, Rectangle2D title, Rectangle2D body) {
        return new MediaLayoutSpec(
                name, ImagePosition.LEFT,
                title.getX(), title.getY(), title.getWidth(), title.getHeight(),
                body.getX(), body.getY(), body.getWidth(), body.getHeight(), true);
    }

    private static List<MediaLayoutSpec> deduplicate(List<MediaLayoutSpec> specs) {
        List<MediaLayoutSpec> unique = new ArrayList<>();
        for (MediaLayoutSpec spec : specs) {
            boolean exists = unique.stream().anyMatch(u ->
                    u.layoutName().equals(spec.layoutName()) && u.imagePosition() == spec.imagePosition());
            if (!exists && spec.mediaWidth() > 0 && spec.mediaHeight() > 0) {
                unique.add(spec);
            }
        }
        return unique;
    }
}
