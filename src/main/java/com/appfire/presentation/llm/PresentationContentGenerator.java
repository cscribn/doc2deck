package com.appfire.presentation.llm;

import com.appfire.presentation.model.HtmlDeckContentResponse;
import com.appfire.presentation.model.PresentationContentResponse;
import java.io.IOException;

public interface PresentationContentGenerator {

    PresentationContentResponse generate(String prompt) throws IOException;

    HtmlDeckContentResponse generateHtmlDeck(String prompt) throws IOException;
}
