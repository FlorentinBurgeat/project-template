# Spec template

Adapted from mattpocock-skills `to-spec`. Use the `GLOSSARY.md` vocabulary and respect ADRs in `docs/adr/`. No file paths or code snippets (they go stale), except a prototype snippet that encodes a decision more precisely than prose (state machine, schema, type shape); trim it and say it came from a prototype.

```markdown
# <Feature title>

**Status:** draft | approved

## Problem Statement
The problem the user faces, from the user's perspective.

## Solution
The solution, from the user's perspective.

## User Stories
A long, numbered list covering every aspect of the feature.
1. As a <actor>, I want <feature>, so that <benefit>

## Acceptance Criteria
Numbered so tickets and reviews can cite them (AC-1, AC-2...). Each one observable from outside the system.
- **AC-1**: Given <context>, when <action>, then <observable result>

## Implementation Decisions
Modules built or modified, interfaces that change, schema changes, API contracts, architectural decisions, specific interactions, technical clarifications.

## Testing Decisions
- What a good test is here: external behavior only, not implementation details.
- **Test seams** agreed with the user: the highest public boundary per module, each with one line on what it catches and what it misses. Prefer existing seams; the fewer the better.
- Prior art: similar tests already in the codebase.

## Out of Scope
What this spec deliberately does not cover.

## Further Notes
```
