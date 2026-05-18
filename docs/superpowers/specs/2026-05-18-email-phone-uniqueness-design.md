# Email/Phone Uniqueness Validation Design

**Date**: 2026-05-18
**Status**: Design approved, pending implementation

## Overview

Add uniqueness validation for `email` and `phone` fields across the user system. When a user sets an email or phone number (during registration or profile update), the system checks the database and rejects duplicates.

## Decisions Made

| Question | Decision |
|----------|----------|
| Empty email/phone handling | Empty strings stored as NULL, NULL values excluded from uniqueness check |
| Registration check | Yes, register form gets email/phone fields |
| Database-level constraint | Yes, UNIQUE index on email and phone columns |
| Real-time frontend validation | Yes, dedicated check endpoint + blur event |
| Exclude self on update | Yes, user's own email/phone is not treated as conflict |

## Database Changes

```sql
-- Normalize existing empty strings to NULL (MySQL UNIQUE allows multiple NULLs)
UPDATE users SET email = NULL WHERE email = '';
UPDATE users SET phone = NULL WHERE phone = '';

ALTER TABLE users ADD UNIQUE (email);
ALTER TABLE users ADD UNIQUE (phone);
```

## Backend Changes

### ErrorCode — new error codes

```java
EMAIL_EXISTS(2005, "该邮箱已被其他用户使用"),
PHONE_EXISTS(2006, "该手机号已被其他用户使用"),
```

### UserServiceImpl.updateProfile() — add uniqueness checks

On update, check email/phone uniqueness only when:
1. The new value is not null and not empty
2. The new value differs from the user's current value
3. Exclude the current user from the check (`.ne(User::getId, userId)`)

If conflict found, throw `BusinessException` with the corresponding `ErrorCode`. Empty strings are stored as NULL to satisfy the database UNIQUE constraint.

### UserServiceImpl.register() — add uniqueness checks

Add email/phone uniqueness checks for registration using the same `LambdaQueryWrapper` + `selectCount` pattern already used for `username`. No need to exclude self (new user, no existing ID).

### UserController — new real-time check endpoint

```
GET /user/check-field?field=email&value=xxx&excludeId=123
```

- `field`: whitelist-validated (`email` or `phone`)
- `value`: the value to check
- `excludeId`: optional, user ID to exclude (for profile editing)
- Returns `Result.ok()` if available, `Result.error(EMAIL_EXISTS/PHONE_EXISTS)` if taken
- Empty value returns `Result.ok()` immediately

### RegisterDTO — add fields

```java
private String email;
private String phone;
```

## Frontend Changes

### API layer (`api/user.js`) — new check-field API

```javascript
export function checkField(field, value, excludeId) {
  return api.get('/user/check-field', { params: { field, value, excludeId } })
}
```

### Register.vue — add email/phone form fields

Add `邮箱` and `手机号` input fields to the registration form, submitting them with the registration request.

### Profile.vue — add blur-triggered real-time validation

- Add `blur` event handlers on email and phone input fields
- On blur, call `/user/check-field` with the current user ID as `excludeId`
- If conflict, show field-level error message via `el-form-item` error state
- On save, the backend check serves as the final gate

### Error display

The existing API interceptor (`api/index.js`) already handles `code !== 200` responses by showing `ElMessage.error`. This covers the backend errors from `updateProfile` and `register`. The new `checkField` endpoint handles pre-submit field-level validation.

## Architecture Note

All uniqueness checks follow the existing pattern established by `USERNAME_EXISTS` in registration:
`LambdaQueryWrapper` → `userMapper.selectCount()` → `BusinessException`

No new abstractions, no new service methods, no new mapper queries.
