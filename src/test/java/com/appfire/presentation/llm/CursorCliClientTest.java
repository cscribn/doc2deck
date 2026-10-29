package com.appfire.presentation.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appfire.presentation.config.AppConfig;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CursorCliClientTest {

    @Test
    void buildCommandIncludesAskModeTrustAndWorkspace() {
        AppConfig config = Mockito.mock(AppConfig.class);
        Mockito.when(config.cursorCliPath()).thenReturn("agent");
        Mockito.when(config.cursorWorkspace()).thenReturn(Path.of("/tmp/project"));
        Mockito.when(config.cursorModel()).thenReturn("");

        Path promptFile = Path.of("/tmp/project/.cache/llm/prompt-test.md");
        var command = CursorCliClient.buildCommand(config, promptFile);

        assertEquals("agent", command.get(0));
        assertTrue(command.contains("-p"));
        assertTrue(command.contains("--trust"));
        assertTrue(command.contains("--mode=ask"));
        assertTrue(command.contains("--workspace"));
        assertEquals("/tmp/project", command.get(command.indexOf("--workspace") + 1));
        assertTrue(command.stream().anyMatch(part -> part.contains("prompt-test.md")));
    }

    @Test
    void buildCommandAddsModelWhenConfigured() {
        AppConfig config = Mockito.mock(AppConfig.class);
        Mockito.when(config.cursorCliPath()).thenReturn("agent");
        Mockito.when(config.cursorWorkspace()).thenReturn(Path.of("/tmp/project"));
        Mockito.when(config.cursorModel()).thenReturn("gpt-5");

        var command = CursorCliClient.buildCommand(config, Path.of("prompt.md"));

        assertTrue(command.contains("--model"));
        assertEquals("gpt-5", command.get(command.indexOf("--model") + 1));
    }
}
