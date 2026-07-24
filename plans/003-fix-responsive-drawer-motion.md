# 003 — Match drawer motion to its responsive edge

- **Status**: DONE
- **Commit**: f8dcedf7
- **Severity**: MEDIUM
- **Category**: Physicality & origin
- **Estimated scope**: 3 files, about 45 lines

## Problem

The square preview is a right-side drawer on larger viewports and a bottom
sheet below `480px`, but every viewport uses the same horizontal motion:

```css
/* frontend/src/components/square/SquareImageDrawer.vue:55-59 — current */
.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: opacity var(--duration-overlay) var(--ease-standard);
}

.drawer-slide-enter-active .image-drawer,
.drawer-slide-leave-active .image-drawer {
  transition: transform var(--duration-overlay) var(--ease-standard);
}

.drawer-slide-enter-from,
.drawer-slide-leave-to { opacity: 0; }

.drawer-slide-enter-from .image-drawer,
.drawer-slide-leave-to .image-drawer { transform: translateX(28px); }

@media (max-width:479px) {
  .image-drawer {
    top: auto;
    right: 0;
    bottom: 0;
    left: 0;
    /* ... */
  }
}
```

On a phone, a bottom-attached surface drifting sideways breaks the spatial
explanation of where it came from.

## Target

Use the physical edge of the surface as its origin:

```css
/* target */
.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: opacity var(--duration-overlay) var(--ease-out);
}

.drawer-slide-enter-active .image-drawer,
.drawer-slide-leave-active .image-drawer {
  transition: transform var(--duration-overlay) var(--ease-drawer);
}

.drawer-slide-enter-from,
.drawer-slide-leave-to {
  opacity: 0;
}

.drawer-slide-enter-from .image-drawer,
.drawer-slide-leave-to .image-drawer {
  transform: translateX(100%);
}

@media (max-width: 479px) {
  .drawer-slide-enter-from .image-drawer,
  .drawer-slide-leave-to .image-drawer {
    transform: translateY(100%);
  }
}
```

Both entry and exit stay within the existing `240ms` overlay budget. The
backdrop fades while the surface moves. CSS transitions remain interruptible
when a user rapidly opens and closes the drawer.

## Repo conventions to follow

- Keep the current Vue `<transition name="drawer-slide">` and Teleport
  structure:

```vue
<!-- frontend/src/components/square/SquareImageDrawer.vue:2-4 — current -->
<Teleport to="body">
  <transition name="drawer-slide">
    <div v-if="visible && image" class="drawer-backdrop">
```

- Preserve the existing `479px` bottom-sheet breakpoint.
- Consume `--ease-out` and `--ease-drawer` from plan 001.
- Preserve the current focus-on-open watcher and Escape handling.

## Steps

1. Execute plan 001 first.
2. In `frontend/src/components/square/SquareImageDrawer.vue`, split backdrop
   opacity easing from surface movement easing exactly as shown in Target.
3. Replace the desktop `translateX(28px)` start/end state with
   `translateX(100%)`.
4. Add the `translateY(100%)` override inside the existing
   `@media (max-width:479px)` block. Do not add a second phone breakpoint.
5. Keep reduced-motion behavior structurally separate; plan 004 will replace
   the current blanket `transition:none` rule with a fade-only variant.
6. Extend `frontend/src/__tests__/ImageSquareContract.test.ts` or
   `frontend/src/__tests__/SquareImageDrawer.test.ts` to assert:
   - desktop horizontal origin,
   - phone vertical origin,
   - use of `--ease-drawer`,
   - preserved Teleport, focus, Escape, and close behavior.

## Boundaries

- Do NOT change drawer size, placement, padding, z-index, or breakpoint.
- Do NOT add drag-to-dismiss; that would require velocity and friction design
  outside this plan.
- Do NOT add keyframes. This interaction must remain interruptible.
- Do NOT change drawer content, click behavior, focus management, or route
  behavior.
- Do NOT animate layout properties.
- If plan 001 is absent or the responsive layout no longer becomes a bottom
  sheet at `479px`, STOP and report drift.

## Verification

- **Mechanical**:
  - From `frontend/`, run
    `npm.cmd test -- src/__tests__/SquareImageDrawer.test.ts src/__tests__/ImageSquareContract.test.ts`.
  - Run `npm.cmd run check`.
  - Expected result: all commands exit 0 and existing focus/Teleport contracts
    remain intact.
- **Feel check**:
  - At `1440×900`, open the square preview and confirm it travels from the
    right edge, not from the center.
  - At `390×844`, confirm the bottom sheet travels vertically from the bottom.
  - Rapidly open and close it several times; the surface must retarget from its
    current position without restarting from zero or flashing.
  - In DevTools Animations, use 10% speed and verify the backdrop only fades
    while the panel follows its edge.
  - After plan 004, emulate `prefers-reduced-motion` and confirm the surface
    does not translate but the backdrop still fades.
- **Done when**: each responsive shape enters from its attached edge, rapid
  reversal is smooth, accessibility/focus behavior is unchanged, and all
  checks pass.
