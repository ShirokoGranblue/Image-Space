# 005 — Replace the synthetic loading meter

- **Status**: DONE
- **Commit**: f8dcedf7
- **Severity**: MEDIUM
- **Category**: Purpose & frequency / Performance
- **Estimated scope**: 2 files, about 35 lines

## Problem

The Home workspace presents fixed values as if they were loading progress:

```vue
<!-- frontend/src/views/Home.vue:47-53 — current -->
<div class="meter-card">
  <div class="meter-top">
    <span>加载状态</span>
    <strong>{{ loading ? '加载中' : '已就绪' }}</strong>
  </div>
  <div class="meter">
    <span :style="{ width: loading ? '38%' : '74%' }"></span>
  </div>
</div>
```

The values are not backed by progress data. The corresponding transition also
animates `width`, which triggers layout and paint:

```css
/* frontend/src/views/Home.vue:558 — current */
.meter span {
  display: block;
  height: 100%;
  background: var(--color-vermilion);
  transition: width var(--duration-overlay) var(--ease-standard);
}
```

The motion is both semantically misleading and less efficient than a
transform-only indicator.

## Target

Keep the existing visible labels. Replace the synthetic percentages with an
indeterminate loading class:

```vue
<!-- target -->
<div class="meter" :class="{ 'is-loading': loading }" aria-hidden="true">
  <span></span>
</div>
```

Use only `transform` and `opacity`:

```css
/* target */
.meter span {
  display: block;
  width: 100%;
  height: 100%;
  background: var(--color-vermilion);
  transform-origin: left;
  transform: scaleX(1);
  opacity: 0.82;
}

.meter.is-loading span {
  width: 35%;
  transform-origin: center;
  animation: meter-sweep 1s linear infinite;
}

@keyframes meter-sweep {
  from { transform: translateX(-100%); }
  to { transform: translateX(300%); }
}

@media (prefers-reduced-motion: reduce) {
  .meter.is-loading span {
    width: 100%;
    animation: none;
    opacity: 0.55;
    transform: none;
  }
}
```

The adjacent text remains the accessible state. The bar is decorative and
therefore `aria-hidden`.

## Repo conventions to follow

- Keep the status card in the existing Home left rail.
- Reuse `--color-vermilion` and the existing loading boolean.
- Continuous movement uses `linear`.
- Reduced motion retains a static visual state and the existing visible text.

## Steps

1. In `frontend/src/views/Home.vue`, remove the inline `width` style and bind
   the existing `loading` boolean to the `is-loading` class exactly as shown.
2. Replace the width transition in the scoped style with the transform-only
   target CSS and `meter-sweep` keyframes.
3. Replace the current reduced-motion `transition:none` rule for the meter
   with the static reduced target.
4. Extend `frontend/src/__tests__/HomeSelectionLayout.test.ts` with raw-source
   assertions that:
   - the meter no longer binds `width`,
   - the loading class is driven by `loading`,
   - `meter-sweep` uses `linear`,
   - the keyframes animate only `transform`,
   - the reduced variant has no animation.

## Boundaries

- Do NOT invent or request percentage progress from the backend.
- Do NOT change the visible “加载中” or “已就绪” copy.
- Do NOT animate width, left, margin, or any other layout property.
- Do NOT change data fetching or the `loading` lifecycle.
- Do NOT add a spinner dependency.
- If the Home loading state becomes determinate before execution, STOP and
  replace this plan with a real progress design rather than preserving the
  indeterminate sweep.

## Verification

- **Mechanical**:
  - From `frontend/`, run
    `npm.cmd test -- src/__tests__/HomeSelectionLayout.test.ts`.
  - Run `npm.cmd run check`.
  - Expected result: all commands exit 0 and a source search for
    `transition: width` in `Home.vue` returns no match.
- **Feel check**:
  - Throttle the image-list request and open Home.
  - While loading, the bar should sweep at constant speed without implying a
    percentage. When ready, it should become a stable full status line.
  - In Performance tools, confirm the sweep updates compositor transforms and
    does not repeatedly trigger layout.
  - At 10% playback speed, verify there is no acceleration/deceleration.
  - Under reduced motion, verify the bar is static and the adjacent text still
    communicates state.
- **Done when**: the meter no longer claims synthetic progress, its motion is
  transform-only and linear, and all checks pass.
