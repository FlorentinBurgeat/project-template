---
name: new-spec
description: Define a feature end to end before any code - Product Owner interview, spec with acceptance criteria, UX design, technical tickets and DTOs. First half of the feature workflow; /new-feature builds it in a fresh window.
disable-model-invocation: true
argument-hint: "feature idea"
---

# New spec

Turn a feature idea into an approved, buildable package under `.scratch/<feature>/`:
`spec.md`, `docs/design/<feature>.md` (if the feature has UI) and `issues/NN-*.md`.

Run every step in **this one context window**: the interview, spec, design and tickets build on the same thinking. Don't `/compact` before the end. Pick a kebab-case `<feature>` slug at step 1 and use it everywhere.

## Relay pattern (subagent questions)

Subagents cannot ask the user anything. When an agent you call ends with `QUESTIONS FOR USER:` or `AWAITING USER APPROVAL`:
1. Put its questions to the user with `AskUserQuestion` (recommended answer first), or show its summary and ask for approval.
2. Resume **the same agent** with `SendMessage` (its context is kept), passing the answers or approval verbatim.
3. Repeat until the agent finishes without questions. Never answer on the user's behalf.

## Step 1 - Product Owner (main thread)

You are the Product Owner. Invoke the Skill tool with `mattpocock-skills:grilling` and `mattpocock-skills:domain-modeling` (together they are `/grill-with-docs`). Grill the user on the idea until every question is settled, keeping `GLOSSARY.md` and `docs/adr/` current as terms and hard-to-reverse decisions emerge.

Optional detours, only when the grilling needs them:
- a fact outside the codebase (library behavior, API limits) → `mattpocock-skills:research`;
- a business rule or state model that can't be settled on paper → `mattpocock-skills:prototype` (LOGIC branch).

Then write `.scratch/<feature>/spec.md` from [references/spec-template.md](references/spec-template.md). Synthesize; don't re-interview. Propose the test seams and confirm them with the user.

**STOP**: show the spec and get explicit approval. Set `Status: approved`.

## Step 2 - UX/UI (ux-ui-designer agent)

Skip if the feature has no UI, and say so.

Call `ux-ui-designer` with pointers only: the spec path and the target file `docs/design/<feature>.md`. Run the relay pattern until the user approves the design, then resume the agent once more so it writes the implementation specs.

If the user (or the designer) can't settle the design on paper, offer a throwaway UI prototype: call `frontend-developer` to run `mattpocock-skills:prototype` (UI branch) on a `prototype/<feature>` branch, driven by the design doc. Feed the verdict back to the designer, then return to the main branch.

## Step 3 - Technical plan (plan-reviewer agent)

Call `plan-reviewer` with pointers: spec, design doc (if any), [references/ticket-template.md](references/ticket-template.md) and the target directory `.scratch/<feature>/issues/`. Ask it to:
- surface technical difficulties and risks against the current codebase;
- define the DTO contracts first;
- write one ticket per file following the template, with blocking edges and the agreed test seams.

Run the relay pattern. Then present the breakdown to the user as a numbered list (title, blocked by, what it delivers) and ask: is the granularity right, are the blocking edges right, should any ticket be merged or split? Resume the agent with the changes until the user approves.

## End

Tell the user the package is ready and print exactly:

```
/clear
/new-feature .scratch/<feature>
```
