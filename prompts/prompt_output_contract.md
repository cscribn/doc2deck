INSTRUCTIONS (execute in order):
1. Read DOCX content blocks and summary (source material above).
2. For each presentation key, identify supporting DOCX block indices; plan sourceRefs entries.
3. Fill every required presentation key with grounded slide fragments (voice/style and presentation keys sections).
4. Apply TEDx-inspired voice; anchor every claim to DOCX blocks.
5. Fill problemSolvingImg, architectureApproachImg, valueImpactImg per IMAGE KEYS rules.
6. Record block indices in sourceRefs (not inside key values). Put caveats in warnings.
7. Return JSON only matching the schema below. No markdown fences. No commentary before or after JSON.

OUTPUT FORMAT AND CONSTRAINTS:
Template keys found: {{TEMPLATE_KEYS}}

Deliverable body (JSON only):
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

Constraints:
- keys: slide copy fragments only; no citations or rationale.
- sourceRefs: DOCX block indices per key when possible; outside keys object.
- warnings: optional caveats about missing or weak source backing; outside keys object.
