package com.appfire.presentation.model;

import java.util.Map;
import org.apache.poi.sl.usermodel.PictureData;

public record ImageKeyPlan(Map<String, ResolvedImageKey> images) {

    public ImageKeyPlan {
        if (images == null) {
            images = Map.of();
        }
    }

    public record ResolvedImageKey(String key, byte[] data, PictureData.PictureType pictureType) {
    }
}
