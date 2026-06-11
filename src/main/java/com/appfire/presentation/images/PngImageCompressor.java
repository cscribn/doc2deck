package com.appfire.presentation.images;

import com.xqlee.image.png.PngCompressor;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class PngImageCompressor {

    public byte[] compress(byte[] original) {
        if (original == null || original.length == 0) {
            return original;
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
            if (source == null) {
                return original;
            }
            int width = source.getWidth();
            int height = source.getHeight();
            boolean hasTransparency = hasTransparentPixels(source);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PngCompressor.compress(new ByteArrayInputStream(original), output);
            byte[] compressed = output.toByteArray();

            BufferedImage result = ImageIO.read(new ByteArrayInputStream(compressed));
            if (result == null || result.getWidth() != width || result.getHeight() != height) {
                return original;
            }
            if (hasTransparency && !alphaPreserved(source, result)) {
                return original;
            }
            if (compressed.length >= original.length) {
                return original;
            }
            return compressed;
        } catch (IOException e) {
            return original;
        }
    }

    private boolean hasTransparentPixels(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((image.getRGB(x, y) >>> 24) < 255) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean alphaPreserved(BufferedImage source, BufferedImage result) {
        int width = source.getWidth();
        int height = source.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sourceAlpha = source.getRGB(x, y) >>> 24;
                if (sourceAlpha >= 255) {
                    continue;
                }
                int resultAlpha = result.getRGB(x, y) >>> 24;
                if (resultAlpha >= 255) {
                    return false;
                }
            }
        }
        return true;
    }
}
