---
name: model-mitosis
description: Method for detecting model tensions (strong coupling between business notions of two bounded contexts) in the back-end and resolving them by mitosis - shared kernel plus ACL, then specialization of each entity back into its bounded context until the kernel is empty or becomes a new bounded context. Used only by the model-mitosis agent; do not apply it from the main session.
---

# Model mitosis

Rules of record: ADR 0005, ADR 0006, `.claude/rules/back-bounded-contexts.md`, `GLOSSARY.md`. Read them first. This skill is the method; the `model-mitosis` agent is the only one to run it.

## Two modes

1. **Analyze** (default): read code, write nothing. Return a report, a plan and a proposed split.
2. **Apply**: only when the prompt quotes the user's explicit approval of a plan this agent produced, and only the approved steps. Without that quote, stay in analyze mode and say so.

## Analyze

1. Map the bounded contexts (top-level modules under `com.template`), their public API, their SPI, and any existing `kernel*` or `acl*` module.
2. Look for tension signals (`references/tension-signals.md`). A signal is a lead, not a verdict: confirm each by reading the code on both sides.
3. For each confirmed tension, pick the lightest resolution that holds, in this order:
   1. Duplicate and specialize the entity in each BC.
   2. ACL inside the BC: a port in its domain, an adapter in its `adapters/`.
   3. Shared kernel `kernel<A><B>`: only for elements with the same meaning, structure, invariants and rules for both BCs.
   4. External ACL `acl<A>to<B>` with an SPI per BC: only when the translation needs rules from both sides.
4. Return the report in this shape:
   - **Tensions**: per tension, the notion, the BCs, the evidence (`file:line`), why it is a coupling.
   - **Proposed split**: for each element, where it goes (stays in A, stays in B, kernel, external ACL, new BC) and why. State what stays identical in the kernel and what is specialized.
   - **Plan**: ordered steps, each small enough to leave the build green (tests named). Mark which steps are reversible. Include the exit of the kernel: what is extracted at each step, what is left, and whether the remainder is a new BC.
   - **Risks and open questions** for the user.
5. Stop and wait for approval. Do not edit any file.

## Apply

1. Verify the approval quote covers the step. Do the steps in the approved order, one at a time.
2. Per step: make the change, then run `ModularityTest` and `HexagonalArchitectureTest` and the tests of the touched BCs. Stop at the first failure and report it; do not continue or work around it.
3. Shared kernel: separate top-level module, `model/` and `logic/` only, no port, no persistence, no Spring or Exposed, depends on no BC. Each BC keeps persisting its own instances in its own schema (ADR 0005).
4. Never import another BC's types, and never carry another BC's concept as a primitive: introduce a value class owned by the using BC.
5. After each extraction step, re-read the kernel: anything one BC can specialize leaves it. Update the kernel's README (BCs served, why duplication was not enough, what remains).
6. Do not commit, and do not drop schemas or delete data. Report each step done, the tests run, and the next approved step.
