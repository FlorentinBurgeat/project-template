# Tension signals

Leads to confirm by reading both sides. None is proof on its own.

## Structural (found by search)

- A BC imports a type from another BC's base package beyond a deliberate public API or SPI.
- Two BCs define entities or value classes with the same or near names and similar fields (`Customer` in both, `Money` and `Amount`).
- A BC holds a raw `String`, `UUID` or `Long` named after another BC's concept (`customerId`, `orderRef`, `productCode`) with no value class owned by the using BC.
- The same enum or code list, or the same validation constants (length, regex, bounds), is copied in two BCs.
- A table, record or migration of one BC mentions another BC's table, or a Flyway location is shared.
- A `kernel*` module that contains a port, persistence, Spring or Exposed, or imports a BC.
- An `acl*` module that holds business rules, or a BC that depends on an ACL.
- A kernel whose elements differ in meaning or invariants between its BCs.

## Behavioural (found by reading)

- A rule changed in one BC forces a change in the other.
- A BC must know another BC's lifecycle states to do its own work.
- Events between BCs carry the other BC's full model instead of facts.
- The two BCs disagree on the same word (same name, different meaning) or use two words for one notion.
- A BC reaches the other's data through its public API in a loop to rebuild a concept locally.

## Reading the result

- Disagreement on meaning: split the notion, do not share it.
- Same meaning, one BC only reads: ACL inside the reading BC.
- Same meaning, same rules, both write: candidate for a shared kernel, but check duplication first.
- Rules from both sides needed to translate: external ACL, and treat it as a third model in the making.
