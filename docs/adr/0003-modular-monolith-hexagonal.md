# ADR 0003: Modular monolith, hexagonal per bounded context

- **Status**: Accepted
- **Date**: 2026-10-30

## Context

The `features/<name>` layout (controller, dto, service per feature) does not say what is domain, what is use case and what is adapter, and nothing stops a feature from reaching into another one. A first fork (a budgeting app) ended up with calculators and validators in `models/`, services receiving and returning web DTOs, and repositories returning the domain directly. Projects built from this template get several bounded contexts whose rules evolve separately.

## Decision

- **Modular monolith** with Spring Modulith: one top-level module per bounded context under `com.template`, plus technical modules (`auth`, `shared`) with no domain. A module's base package is its public API, its sub-packages are internal. Modules talk through that public API or application events (`ApplicationEventPublisher` + `@ApplicationModuleListener`). `ModularityTest` rejects cycles and use of another module's internals.
- **Hexagonal per bounded context**: `domain/<aggregate>/{model,logic,port}`, `application/`, `adapters/{web,persistence}`. Dependencies point inward, enforced by `HexagonalArchitectureTest` (ArchUnit). Only `application/` calls a port: `logic/` does not depend on `port/`.
- **Anemic aggregates, functional style**: `model/` holds data and value types, `logic/` holds pure functions. Parsing returns a sealed result (`<Aggregate>Parse.Valid | Invalid`), the application layer returns sealed results, no exceptions.
- **Value objects** are `@JvmInline value class` (e.g. `Amount`, `ItemName`, `shared/UserId`), never `typealias`. A value object with invariants has a private constructor: a value is built by a checked factory returning `Parsed.Ok(value)` or `Parsed.Refused(code)`, or, for stored data only, by `reconstituer` (only the persistence adapter may call it, enforced by `HexagonalArchitectureTest`). No check in `init`, so a stored value that today's rules would reject can still be read, and all errors are reported at once. Computed values that may leave the bounds of an entered one (sums, negative differences) get their own unbounded value class, never a raw primitive.
- **Dedicated shapes per layer**: web DTOs and persistence records are primitives only, enums included (`String`), and are mapped to and from the domain. The web mapper reads enum values into the draft and returns a sealed reading result: blank or absent becomes `null` (a required-field error from the domain), an unknown value makes the whole request invalid. No exception, no custom Jackson deserializer.
- **Parse, don't validate**: the web adapter copies the request into a nullable draft without checking anything. A pure domain function `parse<Aggregate>(draft)` turns the draft into a valid aggregate or reports every error at once, the required-field errors included. Past the parse, code only handles valid types (no `!!`, no exception, no Arrow). Every rule lives in the domain, so it holds whatever path builds the aggregate.
- **Strict JSON**: floats are not accepted as integers, scalars are not coerced (`"123"` is not a number, `123` is not a string, see `shared/JacksonConfig`), trailing tokens are rejected, and stack traces are never included in error bodies.
- `GlobalExceptionHandler` and its three unused exceptions are deleted. Under `com.template` it would be component-scanned, and its catch-all would turn Spring MVC errors into 500s (ADR 0001). Exception handlers are scoped to their module with `@RestControllerAdvice(assignableTypes = ...)`.

## Consequences

- More files per feature (model, logic, DTO, record, mappers) in exchange for a domain testable without Spring.
- A new bounded context follows the checklist in `.claude/rules/back-architecture.md` and must keep both architecture tests green.
- Domain words use the project's business language, roles stay in English (`getX`, `parseX`, `XRepository`).
