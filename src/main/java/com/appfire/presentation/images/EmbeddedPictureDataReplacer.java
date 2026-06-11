package com.appfire.presentation.images;

import java.io.IOException;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.internal.MemoryPackagePart;
import org.apache.poi.xslf.usermodel.XSLFPictureData;

final class EmbeddedPictureDataReplacer {

    private EmbeddedPictureDataReplacer() {
    }

    static void replace(XSLFPictureData picture, byte[] data) throws IOException {
        PackagePart part = picture.getPackagePart();
        if (part instanceof MemoryPackagePart memoryPart) {
            memoryPart.clear();
        }
        picture.setData(data);
    }
}
