INSTRUCTIONS (execute in order):
1. Read DOCX content blocks and summary (source material above).
2. Plan {{SLIDES_MIN}}-{{SLIDES_MAX}} slides grouped by milestone indices from the outline.
3. Assign each slide a layout from the catalog; fill copy within hard word limits from the outline.
4. Apply voice/style guidance; anchor claims to DOCX blocks in sourceRefs.
5. Record block indices in sourceRefs using keys like `slides.0.title`, `slides.2.bullets.1`.
6. Put caveats in warnings.
7. Return JSON only matching the schema below. No markdown fences. No commentary before or after JSON.

{{HTML_DECK_OUTLINE}}

{{HTML_DECK_LAYOUTS}}

OUTPUT FORMAT AND CONSTRAINTS:

Deliverable body (JSON only):
{
  "deckTitle": "string",
  "slides": [
    {
      "layout": "hero|standard|split|closing",
      "milestone": 0,
      "title": "string",
      "kicker": "optional string",
      "headline": "hero only",
      "subline": "optional semicolon-separated beats",
      "bullets": [{ "icon": "pen", "text": "fragment" }],
      "imageQuery": "split only",
      "notes": ["speaker note"],
      "ai": "optional agent label"
    }
  ],
  "sourceRefs": { "slides.0.headline": [0, 2] },
  "warnings": ["string"]
}

Constraints:
- First slide should use layout hero; last slide should use layout closing.
- bullets: omit or empty only when layout allows (hero may have 0-5).
- imageQuery: required for split layout; 2-5 words; omit for other layouts.
- Do not emit HTML, markdown, URLs, citations, slide numbers, or emojis in copy fields.
