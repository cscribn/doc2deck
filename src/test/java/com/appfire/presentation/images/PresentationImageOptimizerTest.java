package com.appfire.presentation.images;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.junit.jupiter.api.Test;

class PresentationImageOptimizerTest {

    @Test
    void optimizesEmbeddedPicturesWithoutChangingDimensions() throws Exception {
        byte[] jpeg = ImageTestSupport.createLargeJpeg();
        byte[] png = ImageTestSupport.createTransparentPng();

        try (XMLSlideShow slideShow = new XMLSlideShow()) {
            int basePictureCount = slideShow.getPictureData().size();
            XSLFSlide slide = slideShow.createSlide();
            slide.createPicture(slideShow.addPicture(jpeg, PictureData.PictureType.JPEG));
            slide.createPicture(slideShow.addPicture(png, PictureData.PictureType.PNG));

            List<XSLFPictureData> pictures = slideShow.getPictureData();
            for (XSLFPictureData picture : pictures) {
                picture.getData();
            }
            List<Integer> originalSizes = sizes(pictures, basePictureCount);
            List<Dimension> originalDimensions = dimensions(pictures, basePictureCount);

            new PresentationImageOptimizer(true, 0.8f).optimize(slideShow);

            assertEquals(pictures.size(), slideShow.getPictureData().size());
            for (int index = basePictureCount; index < pictures.size(); index++) {
                int optimizedIndex = index - basePictureCount;
                assertTrue(
                        pictures.get(index).getData().length <= originalSizes.get(optimizedIndex),
                        "picture " + index + " should not grow");
                assertEquals(
                        originalDimensions.get(optimizedIndex),
                        pictures.get(index).getImageDimension());
            }
        }
    }

    private List<Integer> sizes(List<XSLFPictureData> pictures, int startIndex) {
        List<Integer> sizes = new ArrayList<>();
        for (int index = startIndex; index < pictures.size(); index++) {
            sizes.add(pictures.get(index).getData().length);
        }
        return sizes;
    }

    private List<Dimension> dimensions(List<XSLFPictureData> pictures, int startIndex) {
        List<Dimension> dimensions = new ArrayList<>();
        for (int index = startIndex; index < pictures.size(); index++) {
            dimensions.add(pictures.get(index).getImageDimension());
        }
        return dimensions;
    }
}
