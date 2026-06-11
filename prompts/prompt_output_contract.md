INSTRUCTIONS:
1. Read the DOCX content blocks and summary.
2. Fill every required presentation key with grounded content.
3. Apply TEDx-inspired voice: engaging, slightly sensational, professionally witty; every claim still anchored to DOCX blocks.
4. Write slide fragments only: no complete sentences, no essay-style prose. See SLIDE COPY FORMAT above.
5. Cite source block indices in sourceRefs for each key when possible.
6. Return JSON only. No markdown fences. No commentary.

Template keys found: {{TEMPLATE_KEYS}}

JSON schema:
{
  "keys": {
    "shortProjectDescription": "string",
    "problem1": "string",
    "problem2": "string",
    "persona": "string",
    "currentSolution1": "string",
    "currentSolution2": "string",
    "scale": "string",
    "whyNow": "string",
    "ifUnsolved": "string",
    "architectureApproach": "string",
    "approachReasons": "string",
    "personaBetterment": "string",
    "appfireBetterment": "string",
    "estimatedImpact": "string",
    "currentState": "string",
    "sprintsToDeliver": "string",
    "nonDevCosts": "string or omit if not applicable",
    "problemSolvingImg": "string",
    "architectureApproachImg": "string",
    "valueImpactImg": "string"
  },
  "sourceRefs": { "problem1": [0, 2], "architectureApproach": [5] },
  "warnings": ["string"]
}
