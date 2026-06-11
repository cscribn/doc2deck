package com.appfire.presentation.model;

import org.apache.poi.sl.usermodel.PictureData;

public record ResolvedImage(
        int slideIndex,
        byte[] data,
        PictureData.PictureType pictureType) {
}
