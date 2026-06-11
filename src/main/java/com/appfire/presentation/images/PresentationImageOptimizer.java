package com.appfire.presentation.images;

import java.io.IOException;
import java.util.List;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PresentationImageOptimizer {

    private static final Logger LOG = LoggerFactory.getLogger(PresentationImageOptimizer.class);

    private final boolean enabled;
    private final float jpegQuality;
    private final JpegImageCompressor jpegCompressor;
    private final PngImageCompressor pngCompressor;

    public PresentationImageOptimizer(boolean enabled, float jpegQuality) {
        this(enabled, jpegQuality, new JpegImageCompressor(), new PngImageCompressor());
    }

    PresentationImageOptimizer(
            boolean enabled,
            float jpegQuality,
            JpegImageCompressor jpegCompressor,
            PngImageCompressor pngCompressor) {
        this.enabled = enabled;
        this.jpegQuality = jpegQuality;
        this.jpegCompressor = jpegCompressor;
        this.pngCompressor = pngCompressor;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void optimize(XMLSlideShow slideShow) {
        if (!enabled) {
            return;
        }
        List<XSLFPictureData> pictures = slideShow.getPictureData();
        for (int index = 0; index < pictures.size(); index++) {
            optimizePicture(pictures.get(index), index);
        }
    }

    private void optimizePicture(XSLFPictureData picture, int index) {
        try {
            byte[] original = picture.getData();
            PictureData.PictureType type = picture.getType();
            byte[] compressed = compressByType(original, type);
            if (compressed.length < original.length) {
                EmbeddedPictureDataReplacer.replace(picture, compressed);
                LOG.info(
                        "Compressed picture {} ({}): {} -> {} bytes",
                        index,
                        type,
                        original.length,
                        compressed.length);
            }
        } catch (IOException e) {
            LOG.warn(
                    "Skipping picture {} compression. Resolution: verify image format and re-run ./gradlew run",
                    index,
                    e);
        }
    }

    private byte[] compressByType(byte[] original, PictureData.PictureType type) {
        if (type == PictureData.PictureType.JPEG) {
            return jpegCompressor.compress(original, jpegQuality);
        }
        if (type == PictureData.PictureType.PNG) {
            return pngCompressor.compress(original);
        }
        return original;
    }
}
