# 008 — Reveal gallery content without teleporting

- **Status**: DONE
- **Commit**: f8dcedf7
- **Severity**: LOW
- **Category**: Missed opportunity / State indication
- **Estimated scope**: 4 files, about 100 lines

## Problem

The gallery swaps its initial states directly:

```vue
<!-- frontend/src/components/gallery/GalleryGrid.vue:2-29 — current -->
<section class="gallery-grid-shell" :aria-busy="loading ? 'true' : 'false'">
  <div v-if="loading && items.length === 0" class="gallery-grid gallery-grid--loading">
    <!-- skeletons -->
  </div>
  <ErrorState v-else-if="error" />
  <EmptyState v-else-if="items.length === 0" />
  <div v-else class="gallery-grid" :data-density="density">
    <!-- items -->
  </div>
</section>
```

Each card tracks `loading → loaded`, but the state is not represented in the
image class:

```vue
<!-- frontend/src/components/gallery/GalleryItem.vue:25-39 — current -->
<span v-if="imageStatus === 'loading'" class="image-placeholder" />
<img
  v-if="imageStatus !== 'error'"
  class="card-img"
  :class="`fit-${fit}`"
  @load="handleImgLoad"
  @error="handleImgError"
/>
```

```js
// frontend/src/components/gallery/GalleryItem.vue:149-150 — current
function handleImgLoad() {
  imageStatus.value = 'loaded'
}
```

Although `.card-img` declares opacity and transform transitions at line 218,
neither property changes when the image becomes loaded. Images and result
batches therefore appear abruptly.

## Target

Bind the loaded state explicitly:

```vue
<!-- GalleryItem target -->
<img
  v-if="imageStatus !== 'error'"
  class="card-img"
  :class="[
    `fit-${fit}`,
    { 'is-loaded': imageStatus === 'loaded' },
  ]"
/>
```

```css
/* GalleryItem target */
.card-img {
  opacity: 0;
  transform: scale(0.97);
  transition:
    opacity var(--duration-standard) var(--ease-out),
    transform var(--duration-standard) var(--ease-out);
}

.card-img.is-loaded {
  opacity: 1;
  transform: none;
}

@media (hover: hover) and (pointer: fine) {
  .gallery-item:hover .card-img.is-loaded {
    transform: scale(1.01);
  }
}

@media (prefers-reduced-motion: reduce) {
  .card-img {
    transform: none;
    transition: opacity 200ms ease;
  }
}
```

For result batches, replace the content-grid `div` with a Vue
`TransitionGroup` and wrap each keyed slot in one stable grid cell. Use:

```css
.gallery-list-enter-active {
  transition:
    opacity var(--duration-overlay) var(--ease-out),
    transform var(--duration-overlay) var(--ease-out);
  transition-delay: var(--gallery-entry-delay, 0ms);
}

.gallery-list-enter-from {
  opacity: 0;
  transform: translateY(12px) scale(0.97);
}

.gallery-list-move {
  transition: transform var(--duration-standard) var(--ease-in-out);
}
```

Set `--gallery-entry-delay` to `0ms`, `40ms`, `80ms`, and `120ms` for the first
four entries; cap every later entry at `120ms`. The delay is decorative and
must not disable pointer events.

Use a separate `180ms` opacity-only transition for skeleton/error/empty/content
container swaps. It may use `mode="out-in"` because it only governs initial,
empty, and error surfaces; populated filter updates remain mounted and use the
list transition.

## Repo conventions to follow

- `GalleryGrid` owns loading, error, empty, and list state.
- `GalleryItem` owns per-image load state.
- Item identity is already `item.uuid || item.id || index`; retain this key
  expression.
- Existing aspect-ratio skeletons and eager-loading limits must remain intact.
- Consume plan 001 motion tokens; consume plan 004 reduced-motion behavior.

## Steps

1. Execute plans 001, 004, and 007 first.
2. In `frontend/src/components/gallery/GalleryItem.vue`, bind `is-loaded`
   exactly to `imageStatus === 'loaded'`.
3. Replace the current card-image transition with the Target rules. Update the
   existing fine-pointer hover selector so it applies only to `.is-loaded`.
4. In `frontend/src/components/gallery/GalleryGrid.vue`, wrap the four
   top-level state branches in a named Vue transition with stable keys
   (`loading`, `error`, `empty`, `content`).
5. Replace only the populated grid branch with a `TransitionGroup`. Wrap each
   repeated slot in a keyed `.gallery-entry` cell and bind:

   ```js
   {
     '--gallery-entry-delay': `${Math.min(index, 3) * 40}ms`,
   }
   ```

   Keep the current slot props and fallback `GalleryItem` API unchanged.
6. Add `.gallery-entry { min-width: 0; }` so the wrapper preserves current grid
   sizing. Add the exact enter/move rules from Target. Do not animate leave
   position or make leaving cells absolute.
7. Add reduced variants: state and image opacity may fade for `200ms ease`;
   item translation/scale and move transitions must be absent.
8. Extend `frontend/src/__tests__/GalleryGrid.test.ts` to cover stable keys,
   preserved slot props, loading/error/empty branches, and the capped delay.
9. Extend `frontend/src/__tests__/ImageCard.test.ts` to assert the image is not
   `is-loaded` before `load`, becomes loaded after the event, and resets when
   the URL changes.

## Boundaries

- Do NOT animate every scroll exposure; entries animate only when mounted as a
  new result batch.
- Do NOT block click or keyboard interaction during stagger delays.
- Do NOT exceed `240ms` per entry or `120ms` maximum stagger delay.
- Do NOT use blur, layout-property animation, JavaScript timers, or an
  IntersectionObserver.
- Do NOT change image loading priority, URLs, aspect-ratio behavior, router
  behavior, or item actions.
- Do NOT animate failed images from `scale(0)`.

## Verification

- **Mechanical**:
  - From `frontend/`, run
    `npm.cmd test -- src/__tests__/GalleryGrid.test.ts src/__tests__/ImageCard.test.ts`.
  - Run `npm.cmd run check`.
  - Expected result: all commands exit 0 and existing eager/lazy loading tests
    remain unchanged.
- **Feel check**:
  - Throttle image requests. Skeletons should hand off to cards without a hard
    flash; each image should resolve from `scale(0.97)` and opacity 0.
  - Change a square filter. New cards should enter in four short waves while
    already-present cards remain interactive.
  - Rapidly change filters. Transitions must retarget without leaving ghost
    cards or restarting the whole page.
  - At 10% speed, verify no card starts from `scale(0)` and delays cap after the
    fourth item.
  - Under reduced motion, confirm images and states only fade and the grid does
    not translate or scale.
- **Done when**: image completion, initial state replacement, and new result
  batches are visually continuous without delaying interaction, and all checks
  pass.
