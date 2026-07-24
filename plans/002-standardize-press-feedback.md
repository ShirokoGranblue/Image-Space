# 002 — Standardize press feedback

- **Status**: DONE
- **Commit**: f8dcedf7
- **Severity**: MEDIUM
- **Category**: Physicality & origin
- **Estimated scope**: 6 files, about 70 lines

## Problem

The two base controls apply an instantaneous one-pixel translation on press,
but their transitions omit `transform`:

```css
/* frontend/src/components/ui/BaseButton.vue:48,62 — current */
transition:
  background-color var(--duration-fast) var(--ease-standard),
  border-color var(--duration-fast) var(--ease-standard),
  color var(--duration-fast) var(--ease-standard);

.base-button:active:not(:disabled) {
  transform: translateY(1px);
}
```

```css
/* frontend/src/components/ui/BaseIconButton.vue:44,53 — current */
transition:
  background-color var(--duration-fast) var(--ease-standard),
  border-color var(--duration-fast) var(--ease-standard),
  color var(--duration-fast) var(--ease-standard);

.base-icon-button:active:not(:disabled) {
  transform: translateY(1px);
}
```

High-traffic custom controls in the image workspace and square have color
states but no physical press response:

```css
/* frontend/src/components/home/HomeToolbar.vue:27 — current */
.select-all,.primary-command,.filter-chip {
  min-height: var(--control-height-md);
  /* ... */
  cursor: pointer;
}
```

The result is inconsistent: some controls jump, some only recolor, and most
Element Plus buttons have a third behavior.

## Target

Use a subtle, interruptible press response everywhere covered by this plan:

```css
/* target pattern */
.control {
  transition:
    transform var(--duration-fast) var(--ease-out),
    background-color var(--duration-fast) ease,
    border-color var(--duration-fast) ease,
    color var(--duration-fast) ease;
}

.control:active:not(:disabled) {
  transform: scale(0.97);
}
```

The exact scale is `0.97`; the exact duration is the existing `140ms`
`--duration-fast`; the exact transform easing is
`cubic-bezier(0.23, 1, 0.32, 1)` through `--ease-out`.

Apply the pattern to:

- `BaseButton`
- `BaseIconButton`
- enabled Element Plus `.el-button`
- Home toolbar `.select-all`, `.primary-command`, and `.filter-chip`
- square hero sort buttons and active-filter buttons

Color-only hover changes continue to use `ease`. Do not add hover movement.

## Repo conventions to follow

- Shared Element Plus integration lives in `frontend/src/style.css`.
- Component-specific custom controls stay in their existing scoped style
  blocks; do not create a generic JavaScript button wrapper.
- Disabled controls already use native `:disabled` or Element Plus
  `.is-disabled`; preserve those selectors.
- The duration and curve come from plan 001 and
  `frontend/src/styles/tokens.css`.

## Steps

1. Execute plan 001 first. Confirm `--ease-out` exists.
2. In `frontend/src/components/ui/BaseButton.vue`, add `transform` to the
   transition list, change color-property easings to `ease`, and replace
   `translateY(1px)` with `scale(0.97)`.
3. Apply the same change in
   `frontend/src/components/ui/BaseIconButton.vue`.
4. In `frontend/src/style.css`, add a narrowly scoped enabled Element Plus
   press rule:

   ```css
   .el-button:not(.is-disabled) {
     transition: transform var(--duration-fast) var(--ease-out);
   }

   .el-button:not(.is-disabled):active {
     transform: scale(0.97);
   }
   ```

   Do not overwrite Element Plus background, border, or loading transitions.
5. In `frontend/src/components/home/HomeToolbar.vue`, add the target
   transition and active transform to the three existing custom control
   classes.
6. In `frontend/src/components/square/SquareHeroFilters.vue`, add the target
   transition and active transform to `.sort-segment button` and
   `.active-filters button`.
7. Extend `frontend/src/__tests__/DesignFoundation.test.ts` with raw-source or
   mounted-style assertions for `scale(0.97)`, transform transition coverage,
   and the disabled guard. Add a raw-source assertion for the two page-level
   components to `frontend/src/__tests__/StageSixFoundation.test.ts`.

## Boundaries

- Do NOT animate `top`, `left`, margin, padding, width, or height.
- Do NOT add bounce, overshoot, or a scale lower than `0.97`.
- Do NOT move controls on hover.
- Do NOT change click handlers, loading behavior, disabled behavior, or
  accessible names.
- Do NOT add press feedback to drag surfaces, image canvases, slider handles,
  or text inputs.
- If plan 001 has not been applied, STOP rather than hard-coding the curve.

## Verification

- **Mechanical**:
  - From `frontend/`, run
    `npm.cmd test -- src/__tests__/DesignFoundation.test.ts src/__tests__/StageSixFoundation.test.ts`.
  - Run `npm.cmd run check`.
  - Expected result: all commands exit 0; no disabled control emits clicks in
    existing tests.
- **Feel check**:
  - Test login submit, Home upload, Home visibility filters, square sorting,
    and an icon button using mouse, touch emulation, Enter, and Space.
  - The control should compress slightly and retarget smoothly if released
    before `140ms`; it must never jump vertically.
  - Hold a control in its pressed state and confirm text and icon remain crisp.
  - In DevTools Animations, play at 10% speed and verify the scale origin is
    the control center.
  - Toggle `prefers-reduced-motion`; after plan 004, color feedback remains but
    the scale response is removed.
- **Done when**: the covered controls share one subtle press behavior, disabled
  controls never transform, keyboard activation remains immediate, and the
  targeted plus full frontend checks pass.
