package com.appfire.presentation.config;

import com.appfire.presentation.images.ImageAcquisitionService;
import com.appfire.presentation.images.PresentationImageOptimizer;
import com.appfire.presentation.template.EmbeddedFontCleaner;
import com.appfire.presentation.template.PptxLayoutNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppBeansConfiguration {

    @Bean
    AppConfig appConfig() {
        return AppConfig.load();
    }

    @Bean
    PresentationKeysConfig presentationKeysConfig(AppConfig appConfig) {
        return PresentationKeysConfigLoader.load(appConfig.presentationKeysPath());
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    PptxLayoutNormalizer pptxLayoutNormalizer(AppConfig appConfig, PresentationKeysConfig keysConfig) {
        return new PptxLayoutNormalizer(
                appConfig.layoutNormalizeEnabled(),
                appConfig.layoutSkipTextFitSlideIndices(),
                keysConfig.imageKeyNames());
    }

    @Bean
    PresentationImageOptimizer presentationImageOptimizer(AppConfig appConfig) {
        return new PresentationImageOptimizer(
                appConfig.imageOptimizationEnabled(), appConfig.imageJpegQuality());
    }

    @Bean
    EmbeddedFontCleaner embeddedFontCleaner(AppConfig appConfig) {
        return new EmbeddedFontCleaner(appConfig.fontCleanupEnabled());
    }

    @Bean
    ImageAcquisitionService imageAcquisitionService(AppConfig appConfig, ObjectMapper objectMapper) {
        return new ImageAcquisitionService(
                appConfig.pexelsApiKey(), appConfig.imageCacheDir(), objectMapper);
    }
}
