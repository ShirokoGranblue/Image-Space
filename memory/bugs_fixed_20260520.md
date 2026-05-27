---
name: bugs-fixed-may-2026
description: "Multiple bug fixes including cache TTL, race conditions, OAuth avatar, and Vue rendering issues"
metadata: 
  node_type: memory
  type: project
  originSessionId: 162a19e9-bc83-4fe9-9df7-b8a3b1820b93
---

Key bugs fixed in this cycle:
- **CaffeineLocalCache TTL**: Switched to `Expiry` interface to respect per-entry TTL instead of global setting
- **Email rate-limit race condition**: Replaced non-atomic check with Redis `SET NX` for mutual exclusion
- **MyBatis-Plus null-field update**: Fixed `deleteAccount` not updating null fields during soft-delete
- **OAuth avatar storage**: Changed from downloading to MinIO to storing raw URL directly
- **Profile.vue v-if/v-else chain**: Fixed chain broken by danger-zone section placement
- **Profile isOwner false positive**: Fixed when both compared IDs are undefined
- **Background editor crop mismatch**: Aligned crop dimensions with live ResizeObserver values
- **GitHub private email**: Switched to `/user/emails` API endpoint for retrieving private emails

**Why:** These were issues discovered during OAuth and account deletion feature implementation.
**How to apply:** When working in these areas, be aware of these resolved edge cases to avoid reintroducing them.
