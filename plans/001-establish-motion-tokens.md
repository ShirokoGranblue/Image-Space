# 001 — Establish semantic motion tokens

- **Status**: DONE
- **Commit**: f8dcedf7
- **Severity**: MEDIUM
- **Category**: Easing & duration / Cohesion & tokens
- **Estimated scope**: 4 files, about 35 lines

## Problem

The frontend has a useful duration scale but only one easing curve, so entry,
exit, on-screen movement, drawers, and hover feedback all share the same
physical character:

```css
/* frontend/src/styles/tokens.css:110 — current */
--duration-instant: 80ms;
--duration-fast: 140ms;
--duration-standard: 180ms;
--duration-overlay: 240ms;
--ease-standard: cubic-bezier(0.2, 0.8, 0.2, 1);
```

Two components also bypass the duration tokens:

```css
/* frontend/src/components/ImageUpload.vue:296 — current */
transition: border-color 0.2s ease, background 0.2s ease;

/* frontend/src/components/TagInput.vue:172 — current */
transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
```

This makes motion technically present but semantically flat. A drawer should
not move like a color hover, and an entering surface should not use the same
curve as an element already moving on screen.

## Target

Keep the existing duration scale. Add the three exact semantic curves below
and retain `--ease-standard` as a compatibility alias while plans are executed
incrementally:

```css
/* frontend/src/styles/tokens.css — target */
--duration-instant: 80ms;
--duration-fast: 140ms;
--duration-standard: 180ms;
--duration-overlay: 240ms;

--ease-out: cubic-bezier(0.23, 1, 0.32, 1);
--ease-in-out: cubic-bezier(0.77, 0, 0.175, 1);
--ease-drawer: cubic-bezier(0.32, 0.72, 0, 1);
--ease-standard: var(--ease-out);
```

Use the tokens by purpose:

- enter and exit: `var(--ease-out)`
- movement or morphing already on screen: `var(--ease-in-out)`
- side drawers and bottom sheets: `var(--ease-drawer)`
- hover and color-only changes: CSS `ease`
- continuous motion: `linear`

Replace both hard-coded `0.2s` declarations with
`var(--duration-standard)`. Their color-only easing remains `ease`.

## Repo conventions to follow

- Design tokens already live in `frontend/src/styles/tokens.css`; do not add a
  second token file.
- Existing component styles consume tokens with `var(...)`, for example:

```css
/* frontend/src/components/NavBar.vue:300 — current exemplar */
.slide-down-enter-active,
.slide-down-leave-active {
  transition:
    opacity var(--duration-standard) var(--ease-standard),
    transform var(--duration-standard) var(--ease-standard);
}
```

- Preserve `--ease-standard` until every existing consumer is migrated by a
  selected plan. Removing it in this plan would create a sweeping diff.

## Steps

1. Update `frontend/src/styles/tokens.css` with `--ease-out`,
   `--ease-in-out`, and `--ease-drawer`, using the exact values in the Target
   section. Change `--ease-standard` to the compatibility alias.
2. In `frontend/src/components/ImageUpload.vue`, replace both `0.2s`
   durations in the upload-area transition with
   `var(--duration-standard)`.
3. In `frontend/src/components/TagInput.vue`, replace all three `0.2s`
   durations in the tag-input transition with
   `var(--duration-standard)`.
4. Extend `frontend/src/__tests__/StageSixFoundation.test.ts` to assert the
   three exact curve tokens and the compatibility alias. Also assert that
   `ImageUpload.vue` and `TagInput.vue` no longer contain `0.2s ease`.

## Boundaries

- Do NOT change colors, spacing, radii, typography, or existing duration
  values.
- Do NOT bulk-replace every `--ease-standard` consumer in this plan.
- Do NOT add an animation library or JavaScript motion helper.
- Do NOT remove the compatibility alias.
- If the current token block differs from the excerpt stamped at `f8dcedf7`,
  STOP and report the drift instead of inventing a new scale.

## Verification

- **Mechanical**:
  - From `frontend/`, run
    `npm.cmd test -- src/__tests__/StageSixFoundation.test.ts`.
  - Run `npm.cmd run lint`.
  - Run `npm.cmd run typecheck`.
  - Run `npm.cmd run build`.
  - Expected result: all commands exit 0; the production CSS contains the
    three semantic curves and no component references an undefined token.
- **Feel check**:
  - This foundation plan should not materially change the current feel because
    `--ease-standard` remains an alias.
  - Open the login page, mobile navigation, and square drawer and confirm no
    transition disappears before downstream plans migrate individual roles.
  - In DevTools, inspect computed styles and verify the alias resolves to
    `cubic-bezier(0.23, 1, 0.32, 1)`.
  - Toggle `prefers-reduced-motion`; this plan must not alter the current
    behavior. Plan 004 owns that behavior.
- **Done when**: semantic curves exist once in the global token source,
  hard-coded `0.2s` durations are removed from the two scoped components, and
  all targeted checks pass.
