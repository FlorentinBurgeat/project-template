# Glossary

Domain and architecture vocabulary of the template. Business terms of a fork go here too.

**Bounded context (BC)**: a part of the system with its own model, language and rules, implemented as one top-level Spring Modulith module under `com.template`. It owns its data: one PostgreSQL schema and one role (ADR 0005).

**Aggregate**: a group of entities and value objects changed as one unit, persisted by the BC that owns it. Anemic in this project: data in `model/`, behaviour in `logic/`.

**Shared kernel**: a separate, transitional module holding the types and pure rules that two BCs truly share, identical in meaning, structure, invariants and rules for both. No persistence. Scoped to the BCs that share it (ADR 0006).

**Anti-corruption layer (ACL)**: code that translates another BC's model into the BC's own. Usually a port in the BC's domain plus an adapter in its `adapters/` calling the other BC's public API.

**External ACL**: for strong coupling only, a separate module. Each BC defines a service provider interface (SPI) in its domain; the external ACL implements them and may hold rules from both sides.

**Service provider interface (SPI)**: an interface a BC's domain defines and exposes for an outside module (the ACL) to implement.

**Model tension**: a strong coupling between business notions of two BCs, for example one BC importing the other's types, or carrying its concepts as primitives. Found by the `model-mitosis` agent.

**Model mitosis**: the iterative process of resolving a model tension: move the linked entities to a shared kernel with an ACL, then specialize them back into their BC until the kernel is empty, or becomes a new BC.
