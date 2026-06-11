INSTRUCTIONS:
1. Map DOCX content onto the template slides.
2. Use action replace for existing slides, append for new slides, skip to keep unchanged.
3. Cite source block indices in sourceRefs for each slide.
4. Return JSON only. No markdown fences. No commentary.

Template slide count: {{SLIDE_COUNT}}
JSON schema:
{
  "slides": [
    {
      "slideIndex": 0,
      "action": "replace",
      "title": "string",
      "layoutName": "string from LAYOUT CATALOG",
      "contentStyle": "bullets|body|titleOnly|twoColumn",
      "includeImage": false,
      "imageQuery": "string or null",
      "bullets": ["plain text, no bullet prefix"],
      "leftBullets": ["col1"],
      "rightBullets": ["col2"],
      "bodyText": "string or null",
      "notes": "string or null",
      "sourceRefs": [0],
      "isMetaSlide": false,
      "imagePosition": "left|right|top|none"
    }
  ],
  "warnings": ["string"]
}
