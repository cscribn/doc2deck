EXTERNAL AI PROMPTING (NON-NEGOTIABLE):
1. Map every claim to provided source material: use only facts from DOCX blocks and summary below; no fabrication.
2. Number steps: execute INSTRUCTIONS in order (see output contract).
3. Specify exact output format and constraints: return JSON only per schema; no markdown fences; no text before or after JSON.
4. Place rationale and citations outside deliverable body: keys hold slide copy only; block indices go in sourceRefs; caveats go in warnings; no explanation inside key strings.
5. Stateless: use only this prompt and embedded DOCX content; no prior conversation; no outside knowledge.

CONTENT RULES:
6. Fill every required presentation key with content grounded in DOCX blocks.
{{FORBIDDEN_PHRASE_RULE}}
7. Image keys must be 2-5 word Pexels search phrases, not sentences.
8. All text values must be plain text without bullet prefixes (no •, -, *, or numbered list markers). The template provides bullets.
9. All text values must be slide copy: short fragments or phrase clusters, not complete sentences. Drop filler words (the, a, an, is, are, was, were) when meaning stays clear. Use semicolons to join related fragments when a key needs more than one beat.
10. Hard word limits apply per key (see presentation keys). Output that exceeds a key limit is rejected.
