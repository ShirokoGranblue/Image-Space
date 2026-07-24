# 007 — Make continuous loading motion linear

- **Status**: DONE
- **Commit**: f8dcedf7
- **Severity**: LOW
- **Category**: Easing & duration / Cohesion & tokens
- **Estimated scope**: 5 files, about 35 lines

## Problem

The two gallery loading surfaces use the UI ease-out curve for a continuous
sweep and duplicate the same keyframes:

```css
/* frontend/src/components/gallery/GalleryGrid.vue:125-144 — current */
.gallery-skeleton::after {
  /* ... */
  transform: translateX(-100%);
  animation: grid-shimmer 1.2s var(--ease-standard) infinite;
}

@keyframes grid-shimmer {
  to { transform: translateX(100%); }
}
```

```css
/* frontend/src/components/gallery/GalleryItem.vue:242-248,420-422 — current */
.image-placeholder::after {
  /* ... */
  transform: translateX(-100%);
  animation: gallery-shimmer 1.2s var(--ease-standard) infinite;
}

@keyframes gallery-shimmer {
  to { transform: translateX(100%); }
}
```

The viewer loading track also accelerates and decelerates forever:

```css
/* frontend/src/components/ImageViewer.vue:506,513 — current */
animation: viewer-load 1s ease-in-out infinite alternate;

@keyframes viewer-load {
  from { transform: translateX(-100%); }
  to { transform: translateX(200%); }
}
```

Constant progress motion should be linear. Repeated easing makes the indicators
look like they hesitate at every edge.

## Target

Define one global gallery sweep:

```css
/* frontend/src/styles/tokens.css — target */
@keyframes media-shimmer {
  from { transform: translateX(-100%); }
  to { transform: translateX(100%); }
}
```

Use it in both gallery components:

```css
animation: media-shimmer 1.2s linear infinite;
```

Remove the two scoped gallery keyframe definitions. Keep the viewer's current
geometry and one-second period, but make it unidirectional and linear:

```css
.viewer-loading-mark::after {
  animation: viewer-load 1s linear infinite;
}

@keyframes viewer-load {
  from { transform: translateX(-100%); }
  to { transform: translateX(200%); }
}
```

Do not use `alternate`. Under reduced motion, all three moving indicators are
static.

## Repo conventions to follow

- Global design primitives live in `frontend/src/styles/tokens.css`.
- Component-specific geometry stays in each scoped component.
- Preserve existing `1.2s` gallery and `1s` viewer durations; this plan changes
  the curve and duplicate definition, not cadence.
- Preserve transform-only animation and existing static reduced-motion rules.

## Steps

1. Add the exact `media-shimmer` keyframes to
   `frontend/src/styles/tokens.css`, outside `:root`.
2. In `frontend/src/components/gallery/GalleryGrid.vue`, switch the animation
   to `media-shimmer 1.2s linear infinite` and remove `grid-shimmer`.
3. In `frontend/src/components/gallery/GalleryItem.vue`, switch to the same
   animation and remove `gallery-shimmer`.
4. In `frontend/src/components/ImageViewer.vue`, replace
   `ease-in-out infinite alternate` with `linear infinite`. Keep the current
   viewer keyframe endpoints.
5. Extend `frontend/src/__tests__/StageSixFoundation.test.ts` to assert the
   global keyframe is defined once, both gallery components use it with
   `linear`, local duplicate names are absent, and the viewer no longer uses
   `alternate`.

## Boundaries

- Do NOT increase animation duration or add blur.
- Do NOT animate background position, width, or left.
- Do NOT merge the viewer geometry into the gallery sweep; its pseudo-element
  is half-width and intentionally uses a different endpoint.
- Do NOT re-enable movement under reduced motion.
- Do NOT change loading state logic or placeholder dimensions.

## Verification

- **Mechanical**:
  - From `frontend/`, run
    `npm.cmd test -- src/__tests__/StageSixFoundation.test.ts src/__tests__/GalleryGrid.test.ts src/__tests__/ImageViewer.test.ts`.
  - Run `npm.cmd run check`.
  - Expected result: all commands exit 0; `grid-shimmer` and
    `gallery-shimmer` have no source matches.
- **Feel check**:
  - Throttle image loading and compare the grid skeleton, card placeholder, and
    viewer loading track.
  - At 10% playback speed, each sweep must move at constant velocity and wrap
    without reversing direction.
  - Verify no layout or paint-heavy property changes in the Performance panel.
  - Under reduced motion, verify all three indicators remain visible but
    static.
- **Done when**: continuous loading movement is linear, the gallery keyframe is
  defined once, the viewer no longer alternates, and all checks pass.
