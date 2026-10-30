# HTML deck layouts

Allowed `layout` values on each slide object. Pick the layout that best fits the narrative beat; **vary layouts across the deck** (avoid using the same layout three times in a row; use at least four distinct content layouts among middle slides when the deck has enough slides).

## hero
Opening slide. Required: `headline`, `title` (may match headline). Optional: `kicker`, `subline` (semicolon-separated beats), `bullets` (0-5 icon chips for milestone preview).

## standard
Short bullet beats. Required: `title`, `bullets` (2-4 items). Optional: `kicker`. Use when a simple chip row is enough.

## split
Text plus visual. Required: `title`, `bullets` (2-3 items), `imageQuery` (2-5 words, uncluttered stock photo). Optional: `kicker`, `imageSide` (`left` or `right`, default right), `ai` (shows AI orb on slide). Photo occupies a full-height column beside copy.

## journey
Process overview or timeline. Required: `title`, `bullets` (3-5 items; each bullet is a step label with icon). Optional: `kicker`, `ai` (orb on the road). Best for “how it flows” beats. Step labels render under the milestone icons on the road.

## doc
Structured evidence or form-like content. Required: `title`, `bullets` (2-4 items as rows). Optional: `kicker`. Best for templates, checklists, or document-centric steps.

## cards
Proof or theme tiles that fill the slide. Required: `title`, `bullets` (2-4 items; each becomes a card). Optional: `kicker`. Best for outcomes, pillars, or parallel points.

## list
Vertical checklist with progress bars. Required: `title`, `bullets` (2-4 items). Optional: `kicker`. Best for ordered steps or criteria.

## closing
Final slide. Required: `title`, `bullets` (2-3 takeaway chips). Optional: `subline` as a short CTA line (semicolon-separated beats).

## bullets
Each bullet: `{ "icon": "<name>", "text": "<fragment>" }`. Icon must be one of:
pen, brief, users, chart, target, check, sparkles, flag, trend, doc, shield, star, zap, gauge, building, link, search, crown, sliders, cpu, eye, lock, book, layers, sprout, msg, folder, clip, ticket, grid, alert, scale, rank, cut, loop, x, plus, minus, up, down, right, tie, user, branch, frame, pr, checkc, q, down, building

## milestone
Integer index matching the outline milestones list (0 = first milestone label).

## ai
Optional string label on a slide (e.g. "Coach") to mark AI-related beats in navigation and on journey/split slides; omit when not applicable.

## notes
Array of speaker note strings (not shown on slide; opened with N key). Provide 1-4 notes per slide with fuller context for the live talk.
