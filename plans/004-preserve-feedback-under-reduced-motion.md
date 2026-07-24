# 004 — Preserve feedback under reduced motion

- **Status**: DONE
- **Commit**: f8dcedf7
- **Severity**: MEDIUM
- **Category**: Accessibility
- **Estimated scope**: 8 files, about 90 lines

## Problem

The global reduced-motion rule collapses every animation and transition to
`0.01ms`:

```css
/* frontend/src/styles/tokens.css:293-301 — current */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
  }
}
```

Several components then remove transitions entirely:

```css
/* frontend/src/components/square/SquareImageDrawer.vue:60 — current */
@media (prefers-reduced-motion: reduce) {
  .drawer-slide-enter-active,
  .drawer-slide-leave-active { transition: none; }
}

/* frontend/src/components/gallery/GalleryItem.vue:424-426 — current */
@media (prefers-reduced-motion: reduce) {
  .card-img { transition: none; }
  .image-placeholder::after { animation: none; }
}
```

Reduced motion should remove vestibular movement, not erase every opacity,
color, focus, loading, and state-confirmation cue.

## Target

Remove the universal duration override. Keep only global scroll behavior:

```css
/* frontend/src/styles/tokens.css — target */
@media (prefers-reduced-motion: reduce) {
  html {
    scroll-behavior: auto;
  }
}
```

Use component-level reduced variants:

```css
/* target pattern for an entering surface */
@media (prefers-reduced-motion: reduce) {
  .surface-enter-active,
  .surface-leave-active {
    transition: opacity 200ms ease;
  }

  .surface-enter-from,
  .surface-leave-to {
    opacity: 0;
    transform: none;
  }
}
```

Exact behavior by surface:

- `AstralEnvironment`: preserve the existing JavaScript pause when
  `prefers-reduced-motion` matches.
- navigation, square drawer, notification drawer, and viewer overlay: remove
  translation/scale; keep `opacity 200ms ease`.
- image reveal from plan 008: remove scale; keep `opacity 200ms ease`.
- button press from plan 002: remove scale; keep color/border feedback.
- shimmer and decorative progress motion: stop motion and show a static
  placeholder or status line.
- keyboard focus and error/success colors: never suppress them.

## Repo conventions to follow

- Reduced-motion CSS is currently colocated with the component it modifies.
  Keep component-specific overrides there.
- The global token file owns only the cross-app scroll rule.
- `frontend/src/components/astral/AstralEnvironment.vue:68` already creates a
  `matchMedia('(prefers-reduced-motion: reduce)')` query and pauses the frame
  loop. Do not replace that working implementation.
- Use `200ms ease` for fade-only reduced variants, exactly as prescribed by the
  audit rules.

## Steps

1. Execute plans 001–003 and 007 first so the target motion selectors, tokens,
   and continuous-loading rules exist.
2. In `frontend/src/styles/tokens.css`, delete the universal `*` duration and
   iteration override. Leave a global `html { scroll-behavior: auto; }` rule
   inside the media query.
3. In `frontend/src/components/NavBar.vue`, replace `transition:none` with a
   fade-only `200ms ease` transition for the mobile menu. Force the reduced
   enter/leave transform to `none`.
4. In `frontend/src/components/square/SquareImageDrawer.vue`, keep the
   backdrop fade at `200ms ease`, set panel transition to `none`, and override
   both responsive enter/leave transforms to `none`.
5. In `frontend/src/components/NotificationDrawer.vue`, keep a `200ms ease`
   opacity transition and remove the `translateX(100%)` state under reduced
   motion.
6. In `frontend/src/components/ImageViewer.vue`, keep the viewer overlay fade
   at `200ms ease`; disable image zoom interpolation and the moving loading
   mark. Preserve visible loading text and controls.
7. In `frontend/src/components/gallery/GalleryGrid.vue` and
   `frontend/src/components/gallery/GalleryItem.vue`, keep static skeletons.
   If plan 008 is present, retain fade-only image entry and remove all entry
   translation/scale.
8. In `frontend/src/components/ui/BaseButton.vue`,
   `frontend/src/components/ui/BaseIconButton.vue`, and
   `frontend/src/style.css`, add reduced-motion overrides that prevent active
   scaling while leaving color transitions and native focus feedback intact.
9. Add `frontend/src/__tests__/MotionAccessibility.test.ts`. Read the relevant
   sources as text and assert:
   - no global `transition-duration: 0.01ms`,
   - no global `animation-duration: 0.01ms`,
   - square drawer and notification drawer define fade-only reduced variants,
   - image/grid decorative movement is disabled,
   - the Astral JavaScript media-query guard remains present.

## Boundaries

- Do NOT interpret reduced motion as no feedback.
- Do NOT remove loading labels, `aria-live`, focus outlines, pressed states, or
  error/success colors.
- Do NOT modify the Astral canvas rendering algorithm beyond preserving its
  existing pause behavior.
- Do NOT add a user preference toggle; this plan responds to the operating
  system preference only.
- Do NOT add a global `transition:none !important` fallback.
- If plans 002, 003, or 008 are not present, apply the rule only to selectors
  that exist and record which dependent selector was skipped.

## Verification

- **Mechanical**:
  - From `frontend/`, run
    `npm.cmd test -- src/__tests__/MotionAccessibility.test.ts src/__tests__/SquareImageDrawer.test.ts src/__tests__/ImageViewer.test.ts src/__tests__/NavBar.test.ts`.
  - Run `npm.cmd run check`.
  - Expected result: all commands exit 0 and the global CSS contains no
    universal near-zero transition override.
- **Feel check**:
  - In DevTools Rendering, emulate `prefers-reduced-motion: reduce`.
  - Open the mobile menu, square drawer, notification drawer, and viewer.
    Surfaces should fade without moving.
  - Load a gallery. Skeletons must remain legible but static; completed images
    may fade in without scaling.
  - Press buttons. Colors must still respond, but controls must not shrink.
  - Confirm the Astral background stops flowing and no continuous decorative
    motion remains.
- **Done when**: reduced-motion users retain clear state feedback without
  spatial movement, no global rule suppresses all transitions, and targeted
  plus full checks pass.
