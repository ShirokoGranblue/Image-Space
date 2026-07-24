# 009 — Stage editorial page entrances

- **Status**: TODO
- **Commit**: f8dcedf7
- **Severity**: LOW
- **Category**: Missed opportunity / Cohesion & tokens
- **Estimated scope**: 7 files, about 95 lines

## Problem

The app renders route content directly:

```vue
<!-- frontend/src/App.vue:7-9 — current -->
<AppShell :variant="shellVariant" width="full">
  <router-view />
</AppShell>
```

The main user pages have strong editorial layers but mount all of them at once:

```vue
<!-- frontend/src/components/auth/AuthLayout.vue:3-20 — current -->
<div class="auth-layout" :class="`auth-layout--${formWidth}`">
  <aside class="auth-aside"><slot name="aside" /></aside>
  <main class="auth-main">
    <div class="auth-main__inner">
      <header class="auth-header"><slot name="header" /></header>
      <section class="auth-body"><slot /></section>
      <footer class="auth-footer"><slot name="footer" /></footer>
    </div>
  </main>
</div>
```

Home and Square similarly mount their hero, command bar, and filter strip with
no relationship between layers. The static hierarchy is excellent, but the
first frame feels assembled rather than revealed.

## Target

Use a subtle route fade on user routes only:

```vue
<!-- App.vue target -->
<router-view v-slot="{ Component, route: resolvedRoute }">
  <Transition :name="routeTransitionName" mode="out-in">
    <component :is="Component" :key="resolvedRoute.path" />
  </Transition>
</router-view>
```

```js
// App.vue target
const routeTransitionName = computed(() =>
  route.path.startsWith('/admin/') ? '' : 'route-page'
)
```

```css
/* App.vue target */
.route-page-enter-active {
  transition: opacity var(--duration-fast) var(--ease-out);
}

.route-page-leave-active {
  transition: opacity var(--duration-instant) var(--ease-out);
}

.route-page-enter-from,
.route-page-leave-to {
  opacity: 0;
}
```

Key by `resolvedRoute.path`, not `fullPath`, so changing search/filter query
parameters does not replay the page entrance.

Define one global section reveal:

```css
/* frontend/src/styles/tokens.css — target */
@keyframes editorial-reveal {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
}

@keyframes editorial-fade {
  from { opacity: 0; }
}
```

Apply `editorial-reveal 240ms var(--ease-out) both` with these exact delays:

- auth aside `0ms`; auth main `40ms`
- Home main head `0ms`; command panel `40ms`; filter strip `80ms`
- Square hero `0ms`; command panel `40ms`; initial active-filter area `80ms`

Under reduced motion, replace each reveal with
`editorial-fade 200ms ease both`.

## Repo conventions to follow

- `App.vue` already computes the shell variant from the active route.
- Admin routes already use `route.path.startsWith('/admin/')`; reuse that
  boundary.
- Page-specific section selectors stay in their existing scoped styles:
  `AuthLayout.vue`, `HomeToolbar.vue`, and `SquareHeroFilters.vue`.
- Global keyframes live beside tokens in `frontend/src/styles/tokens.css`.
- Vue transitions need no new dependency.

## Steps

1. Execute plans 001 and 004 first.
2. In `frontend/src/App.vue`, replace the direct router view with the scoped
   slot, Transition, and dynamic component shown in Target.
3. Add `routeTransitionName` using the existing `route` computed context. Use
   an empty transition name for `/admin/**`.
4. Add the exact route fade CSS to `App.vue`. Do not add route translation,
   blur, or scale.
5. Add the two global keyframes to
   `frontend/src/styles/tokens.css`.
6. In `frontend/src/components/auth/AuthLayout.vue`, animate `.auth-aside` and
   `.auth-main` once on mount with delays `0ms` and `40ms`.
7. In `frontend/src/components/home/HomeToolbar.vue`, animate `.main-head`,
   `.command-panel`, and `.filter-strip` once with delays `0ms`, `40ms`, and
   `80ms`.
8. In `frontend/src/components/square/SquareHeroFilters.vue`, animate
   `.square-hero` and `.square-command` with delays `0ms` and `40ms`. Apply the
   `80ms` delay only to an active-filter block present on initial mount;
   plan 010 owns later filter-chip state changes.
9. Add reduced-motion overrides to all three scoped components using the exact
   fade target.
10. Add `frontend/src/__tests__/AppMotionContract.test.ts` to assert:
    - RouterView uses the scoped slot,
    - the key is `resolvedRoute.path`, not `fullPath`,
    - admin routes select an empty transition name,
    - section delays are exactly `0/40/80ms`,
    - reduced motion replaces translation with opacity-only fade.

## Boundaries

- Do NOT animate the admin audit route.
- Do NOT key route content by query or hash.
- Do NOT animate route layout, nav position, or the Astral canvas.
- Do NOT use a leave duration longer than `80ms` or an enter duration longer
  than `240ms`.
- Do NOT disable interaction while section animations run.
- Do NOT replay section entrances on scroll, focus, filter changes, or browser
  resize.
- Do NOT add per-page JavaScript timers.

## Verification

- **Mechanical**:
  - From `frontend/`, run
    `npm.cmd test -- src/__tests__/AppMotionContract.test.ts src/__tests__/router.test.ts src/__tests__/AuthPages.test.ts`.
  - Run `npm.cmd run check`.
  - Expected result: all commands exit 0 and existing route guards/redirects
    remain unchanged.
- **Feel check**:
  - Navigate among login, square, Home, detail, and profile. The old page
    should disappear quickly; the new page should fade in without a white or
    dark flash.
  - On login, Home, and Square, observe a restrained `0/40/80ms` editorial
    sequence. Controls must be clickable immediately.
  - Change only a Square query filter. The entire page entrance must not replay.
  - Open `/admin/audit-log`; it must appear immediately without the user-site
    entrance.
  - At 10% speed, verify the route layer only changes opacity and section
    movement is exactly 12px.
  - Under reduced motion, verify all section translation disappears and only a
    200ms fade remains.
- **Done when**: user-route changes and first-mount page layers feel composed,
  query changes and admin remain immediate, and all checks pass.
