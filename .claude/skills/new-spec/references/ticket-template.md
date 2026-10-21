# Ticket template

Adapted from mattpocock-skills `to-tickets` (local tracker). One file per ticket: `.scratch/<feature>/issues/<NN>-<slug>.md`, numbered from `01` in dependency order (blockers first).

Slicing rules:
- Each ticket is a **tracer bullet**: a narrow but complete path through every layer it needs (migration, API, UI, tests), demoable or verifiable on its own. Not a horizontal slice of one layer.
- Each ticket fits in one fresh context window.
- Prefactoring ("make the change easy, then make the easy change") comes first, as its own ticket.
- A wide mechanical refactor is sequenced expand → migrate in batches → contract instead of sliced.
- No file paths or code snippets in prose. DTO shapes are the exception: they are the contract.

```markdown
# <NN>: <Ticket title>

**Status:** ready-for-agent | in-progress | done

**Blocked by:** <NN titles>, or "None (can start immediately)"

**Spec:** ../spec.md (covers AC-1, AC-3) · **Design:** docs/design/<feature>.md (if any)

## What to build
The end-to-end behavior this ticket makes work, from the user's perspective.

## DTO contract
Request/response shapes, endpoint, status codes and error cases. Written before Back and Front start, so both can work in parallel.

## Back
Back-end subtasks (backend-developer). "None" if not applicable.

## Front
Front-end subtasks (frontend-developer). "None" if not applicable.

## Test seams
The agreed seams this ticket is tested at (from the spec's Testing Decisions). Tests are written only here.

## Acceptance criteria
- [ ] Criterion 1 (cite the spec AC it proves)
- [ ] Criterion 2
```
