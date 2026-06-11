package com.appfire.presentation.images;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class PngImageCompressorTest {

    private final PngImageCompressor compressor = new PngImageCompressor();

    @Test
    void compressesPngPreservesDimensionsAndAlpha() throws Exception {
        byte[] original = ImageTestSupport.createTransparentPng();
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));

        byte[] compressed = compressor.compress(original);

        assertTrue(compressed.length <= original.length, "compressed PNG should not grow");
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(compressed));
        assertEquals(source.getWidth(), result.getWidth());
        assertEquals(source.getHeight(), result.getHeight());
        assertTrue(hasSemiTransparentPixel(result), "alpha channel should be preserved");
    }

    private boolean hasSemiTransparentPixel(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (alpha > 0 && alpha < 255) {
                    return true;
                }
            }
        }
        return false;
    }
}
