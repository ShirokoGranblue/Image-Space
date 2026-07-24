# 006 — Calm notification list hover

- **Status**: TODO
- **Commit**: f8dcedf7
- **Severity**: MEDIUM
- **Category**: Purpose & frequency / Accessibility
- **Estimated scope**: 2 files, about 25 lines

## Problem

Every notification row moves left on hover, and the transform is not limited
to a device with real hover:

```css
/* frontend/src/components/NotificationDrawer.vue:248-272 — current */
.notification-item {
  /* ... */
  transition:
    background-color var(--duration-fast) var(--ease-standard),
    border-left-color var(--duration-fast) var(--ease-standard),
    transform var(--duration-fast) var(--ease-standard);
}

.notification-item:hover {
  background: var(--color-surface-2);
  border-left-color: var(--color-vermilion);
  transform: translateX(-4px);
}

.notification-item.unread:hover {
  border-left-color: var(--color-vermilion);
  transform: translateX(-4px);
}
```

Notifications are a high-frequency list. Repeated row movement adds noise, and
touch browsers can retain a false hover state after a tap.

## Target

Keep state feedback but remove positional movement:

```css
/* target */
.notification-item {
  transition:
    background-color var(--duration-fast) ease,
    border-left-color var(--duration-fast) ease;
}

.notification-item:hover {
  background: var(--color-surface-2);
  border-left-color: var(--color-vermilion);
}

.notification-item.unread:hover {
  background: rgba(17, 26, 53, 0.04);
  border-left-color: var(--color-vermilion);
}
```

The row does not translate on any pointer type. The border and background are
enough to identify hover and unread state.

## Repo conventions to follow

- Preserve the existing unread background and left-border language.
- Use the existing `--duration-fast` token.
- Color-only hover changes use CSS `ease`.
- Keep list DOM, selection checkboxes, routing, fetch behavior, and drawer
  transition unchanged.

## Steps

1. In `frontend/src/components/NotificationDrawer.vue`, remove `transform`
   from `.notification-item` transition.
2. Remove `transform: translateX(-4px)` from both hover rules.
3. Change the remaining color-property easing from `--ease-standard` to
   `ease`.
4. Extend `frontend/src/__tests__/ProfileNotificationTypography.test.ts` or
   `frontend/src/__tests__/NotificationDrawer.test.ts` with a raw-source
   assertion that notification-item hover contains no transform while
   background and border feedback remain.

## Boundaries

- Do NOT alter notification data fetching, marking as read, deletion, routing,
  or selection.
- Do NOT remove hover/focus state entirely.
- Do NOT add a replacement scale, bounce, or child-image movement.
- Do NOT change the drawer entry/exit animation in this plan.
- Do NOT change unread colors or typography.

## Verification

- **Mechanical**:
  - From `frontend/`, run
    `npm.cmd test -- src/__tests__/NotificationDrawer.test.ts src/__tests__/ProfileNotificationTypography.test.ts`.
  - Run `npm.cmd run check`.
  - Expected result: all commands exit 0; source search shows no
    `translateX(-4px)` in `NotificationDrawer.vue`.
- **Feel check**:
  - Open a drawer with at least ten notifications and move the pointer rapidly
    down the list. Rows must remain spatially stable.
  - Verify hover and unread states remain distinguishable by color and border.
  - In mobile emulation, tap rows and return to the drawer; no row should
    remain displaced.
  - Toggle reduced motion and confirm the same stable behavior.
- **Done when**: notification rows no longer move, state feedback remains
  clear, touch behavior has no sticky displacement, and all checks pass.
