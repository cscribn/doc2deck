package com.appfire.presentation;

import java.io.IOException;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TestFixtureExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) throws IOException {
        TestFixtureGenerator.ensureFixtures();
    }
}
