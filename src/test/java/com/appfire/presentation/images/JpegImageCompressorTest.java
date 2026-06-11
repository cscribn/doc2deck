package com.appfire.presentation.images;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class JpegImageCompressorTest {

    private final JpegImageCompressor compressor = new JpegImageCompressor();

    @Test
    void compressesJpegAndPreservesDimensions() throws Exception {
        byte[] original = ImageTestSupport.createLargeJpeg();

        byte[] compressed = compressor.compress(original, 0.8f);

        assertTrue(compressed.length < original.length, "compressed JPEG should be smaller");
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(original));
        BufferedImage compressedImage = ImageIO.read(new ByteArrayInputStream(compressed));
        assertEquals(originalImage.getWidth(), compressedImage.getWidth());
        assertEquals(originalImage.getHeight(), compressedImage.getHeight());
    }
}
