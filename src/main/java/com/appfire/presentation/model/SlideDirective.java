package com.appfire.presentation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SlideDirective(
        int slideIndex,
        SlideAction action,
        String title,
        List<String> bullets,
        String bodyText,
        String notes,
        List<Integer> sourceRefs,
        Double titleFontSizePt,
        Double bodyFontSizePt,
        String layoutName,
        ContentStyle contentStyle,
        Boolean includeImage,
        String imageQuery,
        List<String> leftBullets,
        List<String> rightBullets,
        Boolean isMetaSlide,
        ImagePosition imagePosition) {

    public SlideDirective {
        if (bullets == null) {
            bullets = List.of();
        }
        if (sourceRefs == null) {
            sourceRefs = List.of();
        }
        if (leftBullets == null) {
            leftBullets = List.of();
        }
        if (rightBullets == null) {
            rightBullets = List.of();
        }
        if (contentStyle == null) {
            contentStyle = ContentStyle.BULLETS;
        }
        if (includeImage == null) {
            includeImage = false;
        }
        if (imagePosition == null) {
            imagePosition = ImagePosition.NONE;
        }
    }

    public SlideDirective withAction(SlideAction newAction) {
        return new SlideDirective(
                slideIndex, newAction, title, bullets, bodyText, notes, sourceRefs,
                titleFontSizePt, bodyFontSizePt, layoutName, contentStyle, includeImage,
                imageQuery, leftBullets, rightBullets, isMetaSlide, imagePosition);
    }

    public SlideDirective withoutImage() {
        return new SlideDirective(
                slideIndex, action, title, bullets, bodyText, notes, sourceRefs,
                titleFontSizePt, bodyFontSizePt, layoutName, contentStyle, false,
                imageQuery, leftBullets, rightBullets, isMetaSlide, ImagePosition.NONE);
    }

    public SlideDirective withImage(String newImageQuery, String newLayoutName, ImagePosition position) {
        return new SlideDirective(
                slideIndex, action, title, bullets, bodyText, notes, sourceRefs,
                titleFontSizePt, bodyFontSizePt,
                newLayoutName != null ? newLayoutName : layoutName,
                contentStyle, true, newImageQuery,
                leftBullets, rightBullets, isMetaSlide,
                position != null ? position : ImagePosition.RIGHT);
    }
}
