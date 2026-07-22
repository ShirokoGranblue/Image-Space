# Domain Docs

This repository uses a single-context domain documentation layout. These rules tell the engineering skills how to consume that documentation while exploring the codebase.

## Before exploring, read these

- Read `CONTEXT.md` at the repository root when it exists.
- Read the ADRs under `docs/adr/` that affect the area being changed.

If either location does not exist, proceed silently. Do not flag its absence or suggest creating placeholder documents upfront. The `/domain-modeling` skill, including when reached through `/grill-with-docs` or `/improve-codebase-architecture`, creates these documents lazily when domain terms or architectural decisions are actually resolved.

## File structure

The selected single-context layout is:

```text
/
├── CONTEXT.md
├── docs/
│   └── adr/
├── backend/
├── frontend/
├── r2-proxy/
└── deploy/
```

`CONTEXT.md` describes the shared product and domain vocabulary across the backend, frontend, Worker, and deployment boundaries. `docs/adr/` contains repository-wide architectural decisions.

Do not introduce a root `CONTEXT-MAP.md` or per-module context files unless the repository later adopts genuinely independent bounded contexts and this configuration is intentionally changed to multi-context.

## Use the glossary's vocabulary

When output names a domain concept in an issue title, refactor proposal, hypothesis, test name, or implementation plan, use the term defined in `CONTEXT.md`. Do not drift to synonyms that the glossary explicitly avoids.

If a needed concept is not present in the glossary, first reconsider whether the output is inventing language the project does not use. If the concept represents a real gap, record it for `/domain-modeling`.

## Flag ADR conflicts

If proposed work contradicts an existing ADR, surface the conflict explicitly rather than silently overriding the decision. For example:

> Contradicts ADR-0007 — worth reopening because the underlying deployment constraint has changed.
