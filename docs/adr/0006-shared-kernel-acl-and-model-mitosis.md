# ADR 0006: Shared kernel, anti-corruption layer and model mitosis

- **Status**: Accepted
- **Date**: 2026-10-30

## Context

Two bounded contexts (BC) sometimes use the same business notion. Sharing it carelessly couples their models: types leak from one BC to the other, or a concept of one BC hides in the other behind a primitive (a `String` that is in fact the other BC's identifier). This coupling is invisible to import-based tests.

## Decision

- **Duplicate and specialize first.** Before sharing anything, check whether each BC can hold its own copy of the entity, specialized to its needs. This is the usual answer.
- **Then an ACL inside the BC, in most cases.** The BC defines a port in its domain for what it needs from the other BC, and an adapter in its own `adapters/` implements it by calling the other BC's public API and translating to the BC's own model. This is enough for most couplings and needs no extra module.
- **Shared kernel, only when duplication is not enough.** It is a separate top-level module (`com.template.kernel<A><B>`), never a package inside a BC, and is scoped to the BCs that share aggregates. If BCs C and D need one too, it is a different kernel.
- **Everything in a kernel has the same meaning, the same structure and types, the same invariants and the same business rules in every BC that uses it.** If one BC needs a different rule or a different field, that element does not belong in the kernel: specialize it in the BC.
- **A kernel has no persistence.** It holds types and pure rules only (`model/`, `logic/`), no port, no Spring, no Exposed. Each BC persists its own instances in its own schema (ADR 0005). When two BCs store the same instance, one BC is the source of truth and the other copy is kept up to date by application events.
- **A kernel depends on no BC.** A BC may depend on the kernels it is declared to share.
- **No pollution.** A BC does not import another BC's types, and does not use another BC's concept through a primitive: an identifier or a code from another domain goes through a value class owned by the using BC (or by the kernel).
- **External ACL, only for strong coupling**: a separate top-level module (`com.template.acl<A>to<B>`). When the coupling is strong enough that the translation needs rules from both sides, which an ACL inside one BC cannot hold, each BC declares a **service provider interface (SPI)** in its domain, exposed as a named interface, and the ACL implements the SPIs of both. The BCs depend on their own SPI only; the ACL depends on both BCs, and no BC depends on the ACL.
- **A kernel is transitional.** The aim is to empty it by extracting and specializing its elements into the BCs, one at a time. Whatever cannot be specialized is a candidate for a new BC. An ACL that carries business rules from both sides is a third model in the making and gets the same treatment.
- **Detection and mitosis are done by one agent, not by a test.** The `model-mitosis` skill describes how to find model tensions (strong coupling between business notions), create a kernel and its ACL, move the linked entities and specialize them step by step. The `model-mitosis` agent (Opus, high effort) is the only one to use it. It first returns a plan and a proposed split without touching any file. A mitosis is applied only after the user has approved that plan, and only by the agent.

## Consequences

- A kernel or an ACL is a visible, named module with an owner and an exit plan: its README lists the BCs it serves, why duplication was not enough, and what remains to extract.
- The hidden-primitive rule cannot be checked by `ModularityTest` or `HexagonalArchitectureTest`; it relies on review and on the `model-mitosis` agent.
- An exposed SPI is the one deliberate exception to "a module only uses another module's base package"; it is declared explicitly and nothing else is exposed this way.
