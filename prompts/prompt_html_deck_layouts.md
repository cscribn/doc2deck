# HTML deck layouts

Allowed `layout` values on each slide object. Pick the layout that best fits the narrative beat; vary layouts across the deck.

## hero
Opening slide. Required: `headline`, `title` (may match headline). Optional: `kicker`, `subline` (semicolon-separated beats), `bullets` (0-5 icon chips for milestone preview).

## standard
Most content slides. Required: `title`, `bullets` (2-4 items). Optional: `kicker`.

## split
Text plus visual. Required: `title`, `bullets` (2-3 items), `imageQuery` (2-5 words, uncluttered stock photo). Optional: `kicker`.

## closing
Final slide. Required: `title`, `bullets` (2-3 takeaway chips). Optional: `subline` as a short CTA line (semicolon-separated beats).

## bullets
Each bullet: `{ "icon": "<name>", "text": "<fragment>" }`. Icon must be one of:
pen, brief, users, chart, target, check, sparkles, flag, trend, doc, shield, star, zap, gauge, building, link, search, crown, sliders, cpu, eye, lock, book, layers, sprout, msg, folder, clip, ticket, grid, alert, scale, rank, cut, loop, x, plus, minus, up, down, right, tie, user, branch, frame, pr, checkc, q, down, building

## milestone
Integer index matching the outline milestones list (0 = first milestone label).

## ai
Optional string label on a slide (e.g. "1") to mark AI-related beats in navigation; omit when not applicable.

## notes
Array of speaker note strings (not shown on slide; opened with N key). Provide 1-4 notes per slide with fuller context for the live talk.
