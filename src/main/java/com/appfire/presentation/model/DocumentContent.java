package com.appfire.presentation.model;

import java.util.List;

public record DocumentContent(List<ContentBlock> blocks, String flatSummary) {
}
