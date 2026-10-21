---
name: new-feature
description: Build a feature already defined by /new-spec - delegate tickets to back-end and front-end agents, loop with the user's manual tests, refactor, review and QA. Run in a fresh context window.
disable-model-invocation: true
argument-hint: ".scratch/<feature>"
---

# New feature

Build the package that `/new-spec` produced. The argument is the feature directory, which holds `spec.md` and `issues/NN-*.md`. The design doc, if any, is `docs/design/<feature>.md`. If the directory, the spec or the tickets are missing, or the spec isn't `Status: approved`, stop and tell the user to run `/new-spec`.

You are the orchestrator: you delegate, and you don't write feature code yourself. Give agents **context pointers** (ticket path, spec path, design doc path), never restated content. Every agent report is input for you to check against the ticket, not a fact.

## Relay pattern (subagent questions)

Subagents cannot ask the user anything. When an agent ends with `QUESTIONS FOR USER:`:
1. Put the questions to the user with `AskUserQuestion` (recommended answer first).
2. Resume **the same agent** with `SendMessage`, passing the answers verbatim.
3. Never answer on the user's behalf.

## Step 4 - Code (backend-developer, frontend-developer)

The tickets are a task graph. The **frontier** is every ticket with `Status: ready-for-agent` whose `Blocked by` tickets are all `done`.

For each frontier ticket, call `backend-developer` for its **Back** section and `frontend-developer` for its **Front** section, in parallel: the DTO contract in the ticket is their shared interface. Each agent builds test-first at the ticket's **Test seams** (`mattpocock-skills:tdd`). Independent frontier tickets may run in parallel too, as long as they don't touch the same files.

When both sides report: check the reports against the ticket's acceptance criteria, tick the ones that are met, and set `Status: done`. Recompute the frontier and continue until every ticket is done. If an agent reports a contract mismatch, fix the ticket's DTO contract first, then resume both agents.

## Step 5 - User test loop

**STOP.** Tell the user what to test: the spec's acceptance criteria, plus how to run the app (see `README.md`). Wait for their report.

For each problem they report, delegate the fix to the right dev agent with a pointer to the ticket. If a bug resists a first fix, have the agent invoke `mattpocock-skills:diagnosing-bugs` (feedback loop first, then fix with a regression test). Loop until the user explicitly says they are satisfied.

## Step 6 - Refactor (code-refactor-master)

Call `code-refactor-master` on the changes since the merge-base with `main`, pointing it to the spec. It must preserve behavior and leave every test green.

## Step 7 - Review (code-architecture-reviewer + code-review)

In parallel:
- call `code-architecture-reviewer` on the branch diff against `main`;
- invoke `mattpocock-skills:code-review` yourself with fixed point `main`. Its Spec axis reads `<feature dir>/spec.md`.

Show the user both reports, kept separate. Ask which findings to fix, delegate those to the dev agents, and rerun the affected tests.

## Step 8 - QA (qa-tester)

Call `qa-tester` to test every route the feature added or changed: auth, status codes, error cases, the DTO contracts in the tickets. Defects go back to step 4 (the dev agent fixes them with a regression test), then QA reruns on the fixed routes.

## End

Summarize: the tickets done, the checks run and their results, and anything left open. If the user wants a PR, write its body with `mattpocock-skills:pr`. Suggest they run `/retro` in this session to improve the environment for the next feature.
