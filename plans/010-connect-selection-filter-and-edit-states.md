# 010 — Connect selection, filter, and edit states

- **Status**: DONE
- **Commit**: f8dcedf7
- **Severity**: LOW
- **Category**: Missed opportunity / State indication / Interruptibility
- **Estimated scope**: 8 files, about 150 lines

## Problem

Several important mode changes mount and unmount without spatial or state
continuity.

The Home batch queue is a single abrupt conditional:

```vue
<!-- frontend/src/components/home/HomeBatchQueue.vue:1 — current -->
<footer v-if="visible" class="batch-queue">
  <!-- selected thumbnails and actions -->
</footer>
```

The gallery selection mark changes geometry immediately:

```css
/* frontend/src/components/gallery/GalleryItem.vue:293-305 — current */
.select-mark {
  width: 12px;
  height: 12px;
  border: 1px solid currentColor;
}

.select-toggle.checked .select-mark {
  width: 7px;
  height: 12px;
  border: 0;
  border-right: 2px solid var(--color-text-inverse);
  border-bottom: 2px solid var(--color-text-inverse);
  transform: rotate(45deg) translate(-1px, -1px);
}
```

Square active filters and profile view/edit mode also switch through `v-if`
without a transition:

```vue
<!-- frontend/src/components/square/SquareHeroFilters.vue:29 — current -->
<div v-if="hasActiveFilters" class="active-filters">

<!-- frontend/src/components/profile/ProfileHeader.vue:14,26 — current -->
<template v-if="!editing">
  <!-- profile view -->
</template>
<div v-else class="profile-edit">
  <!-- edit form -->
</div>
```

## Target

### Batch queue

Wrap the existing footer with a Vue transition:

```vue
<Transition name="batch-queue">
  <footer v-if="visible" class="batch-queue">
    <!-- unchanged content -->
  </footer>
</Transition>
```

```css
.batch-queue-enter-active,
.batch-queue-leave-active {
  transition:
    opacity var(--duration-standard) var(--ease-out),
    transform var(--duration-standard) var(--ease-out);
}

.batch-queue-enter-from,
.batch-queue-leave-to {
  opacity: 0;
  transform: translateY(12px);
}
```

### Selection mark

Keep a constant `12px × 12px` box and draw the check with a pseudo-element so
no width or height changes:

```css
.select-mark {
  position: relative;
  width: 12px;
  height: 12px;
  border: 1px solid currentColor;
  transition:
    border-color var(--duration-fast) ease,
    background-color var(--duration-fast) ease;
}

.select-mark::after {
  content: '';
  position: absolute;
  width: 5px;
  height: 9px;
  top: 0;
  left: 3px;
  border-right: 2px solid var(--color-text-inverse);
  border-bottom: 2px solid var(--color-text-inverse);
  opacity: 0;
  transform: rotate(45deg) scale(0.7);
  transform-origin: center;
  transition:
    opacity var(--duration-fast) var(--ease-out),
    transform var(--duration-fast) var(--ease-out);
}

.select-toggle.checked .select-mark::after {
  opacity: 1;
  transform: rotate(45deg) scale(1);
}
```

### Filter chips

Use a `TransitionGroup` for the current active-filter buttons. Give each button
a stable key (`keyword`, `category`, `tag`, `sort`, `clear`). Use
`opacity + scale(0.97)` for `140ms var(--ease-out)`. Do not animate their
positions when wrapping to a new line.

### Profile mode

Make the current view content a single `.profile-view` root and transition
between keyed `view` and `edit` roots using `mode="out-in"`:

```css
.profile-mode-enter-active,
.profile-mode-leave-active {
  transition:
    opacity var(--duration-standard) var(--ease-out),
    transform var(--duration-standard) var(--ease-out);
}

.profile-mode-enter-from,
.profile-mode-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
```

Under reduced motion, all four interactions retain `opacity 200ms ease` and
remove translation/scale.

## Repo conventions to follow

- State stays owned by the existing parents. Do not add a motion store.
- The batch queue receives `visible`; it does not decide selection.
- Gallery selection remains an accessible `aria-pressed` button.
- Square filter values and clear events remain unchanged.
- Profile edit fields, validation, save, and cancel behavior remain unchanged.
- Consume plan 001 tokens and plan 004 reduced-motion behavior.

## Steps

1. Execute plans 001, 002, 004, and 008 first.
2. Expand `frontend/src/components/home/HomeBatchQueue.vue` from its minified
   form only as needed to add the Transition and target CSS. Do not reformat
   unrelated logic.
3. In `frontend/src/components/gallery/GalleryItem.vue`, replace the
   geometry-changing checked mark with the fixed-size pseudo-element target.
   Preserve the button, `aria-pressed`, colors, and 40/44px hit targets.
4. Add `box-shadow` to the gallery-item transition so the existing selected
   inset outline eases over `140ms`; do not animate border width.
5. In `frontend/src/components/home/HomeToolbar.vue`, wrap the conditional
   destructive filter chip in a named transition using the same
   `140ms opacity + scale(0.97)` pattern.
6. In `frontend/src/components/square/SquareHeroFilters.vue`, replace the
   active-filter button container with a `TransitionGroup` and add stable keys
   to each conditional button. Preserve emitted events and accessible text.
7. In `frontend/src/components/profile/ProfileHeader.vue`, wrap view and edit
   roots in a named Transition. Add only the `.profile-view` wrapper required
   for a single transition child.
8. Add exact reduced-motion overrides to the four components.
9. Extend:
   - `frontend/src/__tests__/HomeBatchQueue.test.ts` for transition visibility
     and preserved actions,
   - `frontend/src/__tests__/ImageCard.test.ts` for selected accessibility and
     constant mark geometry,
   - `frontend/src/__tests__/ImageSquareContract.test.ts` for stable filter
     keys/events,
   - `frontend/src/__tests__/ProfileNotificationTypography.test.ts` for keyed
     profile-mode roots and unchanged typography.

## Boundaries

- Do NOT animate `grid-template-columns`, height, width, margin, or padding.
- Do NOT animate the Home inspector column; its responsive grid reflow remains
  instantaneous because a correct FLIP implementation would exceed this plan.
- Do NOT move active-filter chips between wrapped rows.
- Do NOT use keyframes for rapidly reversible batch/profile/filter states.
- Do NOT change selection, filtering, profile validation, save, delete, or
  routing logic.
- Do NOT add a dependency or a JavaScript timeout.
- Do NOT alter keyboard focus order when wrappers are added.

## Verification

- **Mechanical**:
  - From `frontend/`, run
    `npm.cmd test -- src/__tests__/HomeBatchQueue.test.ts src/__tests__/ImageCard.test.ts src/__tests__/ImageSquareContract.test.ts src/__tests__/ProfileNotificationTypography.test.ts`.
  - Run `npm.cmd run check`.
  - Expected result: all commands exit 0; all existing emitted-event and
    accessibility contracts remain intact.
- **Feel check**:
  - Select and deselect cards rapidly. The check should draw smoothly inside a
    fixed box, and the batch queue should retarget from its current position.
  - Toggle multiple Square filters. Chips should appear/disappear locally
    without replaying the entire page or sliding between wrapped rows.
  - Enter and cancel profile edit mode repeatedly. The transition must remain
    under `180ms` per phase, preserve focus, and never overlap two editable
    forms.
  - At 10% speed, verify no selection dimension changes and no layout property
    is animated.
  - Under reduced motion, verify all four interactions use opacity only.
- **Done when**: the selected/unselected, filtered/unfiltered, and
  viewing/editing modes explain their state changes without layout animation
  or altered behavior, and all checks pass.
