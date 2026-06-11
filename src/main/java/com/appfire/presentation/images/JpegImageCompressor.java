package com.appfire.presentation.images;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public final class JpegImageCompressor {

    public byte[] compress(byte[] original, float quality) {
        if (original == null || original.length == 0) {
            return original;
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(original));
            if (image == null) {
                return original;
            }
            int width = image.getWidth();
            int height = image.getHeight();
            byte[] compressed = encodeJpeg(image, quality);
            if (!dimensionsMatch(compressed, width, height)) {
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

    private byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG ImageWriter available");
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(quality);
            writer.write(null, new IIOImage(image, null, null), params);
            writer.dispose();
            return output.toByteArray();
        }
    }

    private boolean dimensionsMatch(byte[] data, int width, int height) throws IOException {
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(data));
        return decoded != null && decoded.getWidth() == width && decoded.getHeight() == height;
    }
}
