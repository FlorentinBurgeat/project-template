---
paths:
  - "back/src/**"
---

# Bounded contexts: isolation, shared kernel, ACL

ADR 0005 (persistence) and ADR 0006 (kernel, ACL). Read them before creating or changing a module boundary.

## Persistence isolation

- One PostgreSQL schema and one role per BC, owner of its schema and with no privilege elsewhere. One Flyway (`db/migration/<bc>/`), one `DataSource`, one Exposed `Database` per BC, internal to the module and used by `adapters/persistence` only.
- No FK, `JOIN`, view or trigger across schemas; no transaction across BCs. Cross-BC data goes through a public API or an application event.
- A reference to a concept of another BC is a value class owned by the using BC, never a `String` or `UUID`.

## Sharing a model between two BCs

Ask in this order, stop at the first yes:

1. Can each BC hold its own copy, specialized? Do that.
2. Does one BC just need data or behaviour from the other? A port in its domain and an adapter in its own `adapters/` that calls the other BC's public API and translates (an ACL inside the BC). This is the usual case.
3. Is the notion truly identical (meaning, types, invariants, rules) in both? Then a shared kernel `kernel<A><B>`.
4. Does the translation need rules from both sides (strong coupling)? Then SPIs defined by each BC and implemented by an external ACL `acl<A>to<B>`.

## Shared kernel

- Separate top-level module, scoped to the BCs that share aggregates (a different pair gets a different kernel).
- `model/` and `logic/` only: pure Kotlin, no port, no persistence, no Spring or Exposed. Depends on no BC.
- Every element has the same meaning, structure, types, invariants and rules for all its users. A BC-specific variant is specialized in the BC, not added to the kernel.
- Transitional: its README lists the BCs it serves, why duplication was not enough, and what is left to extract. Aim to empty it; leftovers are a new BC candidate.

## External ACL (strong coupling only)

- Separate top-level module. Each BC declares its SPI in its domain and exposes it as a named interface; the ACL implements them.
- BC -> own SPI only. ACL -> both BCs. No BC depends on the ACL.
- Business rules from both sides in the ACL are a sign of a third model: handle it with `model-mitosis`.

## No pollution

- Never import another BC's types, and never carry another BC's concept as a primitive (`customerId: String` where `customerId` belongs to another BC). Review for it; run the `model-mitosis` agent to find it.

## Model mitosis

- Only the `model-mitosis` agent (Opus, high effort) analyses and performs it. It returns a plan and a proposed split first, without editing any file. Never apply a mitosis without the user's approval of that plan, and never outside the agent.
