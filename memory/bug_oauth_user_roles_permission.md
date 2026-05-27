---
name: oauth-user-roles-permission-fix
description: "New users (OAuth + regular) had no user_roles entry, causing @SaCheckPermission failures"
metadata: 
  node_type: memory
  type: project
  originSessionId: 4c7f230e-5d9e-4453-ac75-d087daafd71c
---

**Bug:** Users registered via OAuth or `/user/register` got `users` table row with `role='user'` but no `user_roles` table entry. `SaTokenPermissionImpl` reads permissions via `user → user_roles → role_permissions → permissions`. Without `user_roles`, `getPermissionList()` returned empty list, so `@SaCheckPermission("image:upload")` in `ImageController` denied access.

**Symptom:** "无权执行此操作" on image upload, but avatars/backgrounds/comments worked (those use `StpUtil.getLoginIdAsLong()` only, no `@SaCheckPermission`).

**Fix in OAuthServiceImpl.java:**
- Added `UserRoleMapper` + `ensureUserRole(userId)` method that inserts `role_id=3` (user role) if missing
- Called in both email-match and auto-register paths
- `bindOAuthUsername()` changed from `updateById` (writes all fields) to `LambdaUpdateWrapper.set()` (targeted update) to prevent lost-update race

**Fix in UserServiceImpl.java:**
- `register()` now inserts `user_roles` entry after `userMapper.insert(user)`

**Database:** Ran `INSERT IGNORE INTO user_roles (user_id, role_id) SELECT id, 3 FROM users WHERE role='user' AND deleted=0 AND id NOT IN (SELECT user_id FROM user_roles)` to backfill existing users.

**Why:** `users.role` is a string column for display, not used by SaToken. The actual RBAC is table-driven via `user_roles`/`roles`/`role_permissions`/`permissions`.
**How to apply:** Any new user creation path must also insert `user_roles`. Use `ensureUserRole()` helper in OAuthServiceImpl.
