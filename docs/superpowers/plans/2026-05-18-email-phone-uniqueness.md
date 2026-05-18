# Email/Phone Uniqueness Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add uniqueness validation for email and phone fields across registration and profile update, with real-time frontend field-level checking.

**Architecture:** Follow the existing `USERNAME_EXISTS` pattern — `LambdaQueryWrapper` + `selectCount` + `BusinessException`. Add a dedicated `GET /user/check-field` endpoint for real-time frontend validation on blur. Empty strings are stored as NULL to satisfy MySQL UNIQUE constraint.

**Tech Stack:** Spring Boot 3.2 + MyBatis-Plus + Vue 3 + Element Plus + MySQL

---

### Task 1: Database — Add UNIQUE constraints on email and phone

**Files:**
- Create: `backend/src/main/resources/db/migration/add-email-phone-unique.sql`
- Modify: `backend/src/main/resources/db/schema.sql:17-18`

- [ ] **Step 1: Create migration script**

Create `backend/src/main/resources/db/migration/add-email-phone-unique.sql`:

```sql
-- Normalize existing empty strings to NULL (MySQL UNIQUE allows multiple NULLs)
UPDATE users SET email = NULL WHERE email = '';
UPDATE users SET phone = NULL WHERE phone = '';

ALTER TABLE users ADD UNIQUE (email);
ALTER TABLE users ADD UNIQUE (phone);
```

- [ ] **Step 2: Update schema.sql for new installations**

In `backend/src/main/resources/db/schema.sql`, change lines 17-18 from:
```sql
    email VARCHAR(100),
    phone VARCHAR(20),
```
to:
```sql
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20) UNIQUE,
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/db/migration/add-email-phone-unique.sql backend/src/main/resources/db/schema.sql
git commit -m "feat: add UNIQUE constraints on email and phone columns"
```

---

### Task 2: ErrorCode — Add EMAIL_EXISTS and PHONE_EXISTS

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/common/ErrorCode.java:27`

- [ ] **Step 1: Add new error codes**

In `ErrorCode.java`, after line 27 (`LOGIN_FAILED(2004, "用户名或密码错误"),`), add:

```java
    EMAIL_EXISTS(2005, "该邮箱已被其他用户使用"),
    PHONE_EXISTS(2006, "该手机号已被其他用户使用"),
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/common/ErrorCode.java
git commit -m "feat: add EMAIL_EXISTS and PHONE_EXISTS error codes"
```

---

### Task 3: RegisterDTO — Add email and phone fields

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/dto/RegisterDTO.java:1-20`

- [ ] **Step 1: Add email and phone fields with validation annotations**

Replace the entire file:

```java
package com.picmgmt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度为2-50个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    private String email;

    private String phone;
}
```

(These are optional fields with no `@NotBlank` — registration doesn't require email/phone.)

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/dto/RegisterDTO.java
git commit -m "feat: add email and phone fields to RegisterDTO"
```

---

### Task 4: UserServiceImpl.register() — Add email/phone uniqueness check

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/service/impl/UserServiceImpl.java:38-39`

- [ ] **Step 1: Add email and phone uniqueness checks after username check**

In `UserServiceImpl.java` method `register()`, after the existing username uniqueness check (line 38, the closing `}` of the `if` block), add email and phone checks. Also set email/phone on the new User entity.

The `register()` method body becomes:

```java
public UserVO register(RegisterDTO dto) {
    if (!dto.getPassword().equals(dto.getConfirmPassword())) {
        throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
    }
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(User::getUsername, dto.getUsername());
    if (userMapper.selectCount(wrapper) > 0) {
        throw new BusinessException(ErrorCode.USERNAME_EXISTS);
    }
    if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
        String email = dto.getEmail().trim();
        if (userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
    }
    if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
        String phone = dto.getPhone().trim();
        if (userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone)) > 0) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }
    }
    User user = new User();
    user.setUsername(dto.getUsername());
    user.setDisplayName(dto.getUsername());
    user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
    user.setRole("user");
    if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
        user.setEmail(dto.getEmail().trim());
    }
    if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
        user.setPhone(dto.getPhone().trim());
    }
    userMapper.insert(user);
    return BeanUtil.copyProperties(user, UserVO.class);
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/service/impl/UserServiceImpl.java
git commit -m "feat: add email/phone uniqueness check on registration"
```

---

### Task 5: UserServiceImpl.updateProfile() — Add uniqueness check + empty→NULL

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/service/impl/UserServiceImpl.java:78-87`

- [ ] **Step 1: Replace the updateProfile method body**

The `updateProfile()` method (lines 78-87) becomes:

```java
@Override
public UserVO updateProfile(Long userId, String displayName, String email, String phone, String bio) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    if (displayName != null) user.setDisplayName(displayName);
    if (email != null) {
        email = email.trim();
        if (email.isEmpty()) {
            user.setEmail(null);
        } else if (!email.equals(user.getEmail())) {
            if (userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, email)
                            .ne(User::getId, userId)) > 0) {
                throw new BusinessException(ErrorCode.EMAIL_EXISTS);
            }
            user.setEmail(email);
        }
    }
    if (phone != null) {
        phone = phone.trim();
        if (phone.isEmpty()) {
            user.setPhone(null);
        } else if (!phone.equals(user.getPhone())) {
            if (userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, phone)
                            .ne(User::getId, userId)) > 0) {
                throw new BusinessException(ErrorCode.PHONE_EXISTS);
            }
            user.setPhone(phone);
        }
    }
    if (bio != null) user.setBio(bio);
    userRepository.updateById(user);
    return userRepository.toVO(user);
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/service/impl/UserServiceImpl.java
git commit -m "feat: add email/phone uniqueness check on profile update with self-exclusion"
```

---

### Task 6: UserService + UserServiceImpl — Add checkField method

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/service/UserService.java:27`
- Modify: `backend/src/main/java/com/picmgmt/service/impl/UserServiceImpl.java:116`

- [ ] **Step 1: Add checkField to UserService interface**

In `UserService.java`, add before the closing `}`:

```java
    void checkField(String field, String value, Long excludeId);
```

- [ ] **Step 2: Add checkField implementation to UserServiceImpl**

In `UserServiceImpl.java`, add before the closing `}`:

```java
@Override
public void checkField(String field, String value, Long excludeId) {
    if (value == null || value.trim().isEmpty()) return;
    value = value.trim();
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    if ("email".equals(field)) {
        wrapper.eq(User::getEmail, value);
    } else if ("phone".equals(field)) {
        wrapper.eq(User::getPhone, value);
    }
    if (excludeId != null) {
        wrapper.ne(User::getId, excludeId);
    }
    if (userMapper.selectCount(wrapper) > 0) {
        throw new BusinessException("email".equals(field)
                ? ErrorCode.EMAIL_EXISTS : ErrorCode.PHONE_EXISTS);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/picmgmt/service/UserService.java backend/src/main/java/com/picmgmt/service/impl/UserServiceImpl.java
git commit -m "feat: add checkField method for real-time email/phone validation"
```

---

### Task 7: UserController — Add check-field endpoint

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/controller/UserController.java:108`

- [ ] **Step 1: Add checkField endpoint**

In `UserController.java`, add before the closing `}`:

```java
@Operation(summary = "检查邮箱/手机号是否已被使用")
@GetMapping("/check-field")
public Result<Void> checkField(@RequestParam String field,
                                @RequestParam String value,
                                @RequestParam(required = false) Long excludeId) {
    if (!"email".equals(field) && !"phone".equals(field)) {
        return Result.error(400, "参数错误");
    }
    userService.checkField(field, value, excludeId);
    return Result.ok();
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/controller/UserController.java
git commit -m "feat: add GET /user/check-field endpoint for email/phone validation"
```

---

### Task 8: Frontend API — Add checkField function

**Files:**
- Modify: `frontend/src/api/user.js:37`

- [ ] **Step 1: Add checkField export**

In `frontend/src/api/user.js`, after the `uploadBackground` function:

```javascript
export function checkField(field, value, excludeId) {
  return api.get('/user/check-field', { params: { field, value, excludeId } })
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/api/user.js
git commit -m "feat: add checkField API function for real-time validation"
```

---

### Task 9: Register.vue — Add email and phone form fields

**Files:**
- Modify: `frontend/src/views/Register.vue:26-52`

- [ ] **Step 1: Add email and phone form items in template**

In the template (after the `确认密码` form item at lines 23-26, before the submit button at line 27), add:

```html
<el-form-item label="邮箱（选填）">
  <el-input v-model="form.email" placeholder="your@email.com" size="large" :prefix-icon="Message" />
</el-form-item>
<el-form-item label="手机号（选填）">
  <el-input v-model="form.phone" placeholder="选填" maxlength="20" size="large" :prefix-icon="Phone" />
</el-form-item>
```

- [ ] **Step 2: Add Message and Phone icons to import**

Change the import of icons from:
```javascript
import { User, Lock } from '@element-plus/icons-vue'
```
to:
```javascript
import { User, Lock, Message, Phone } from '@element-plus/icons-vue'
```

- [ ] **Step 3: Add email and phone to reactive form**

Change the form reactive from:
```javascript
const form = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})
```
to:
```javascript
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: ''
})
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/Register.vue
git commit -m "feat: add email and phone fields to registration form"
```

---

### Task 10: Profile.vue — Add blur-triggered real-time validation

**Files:**
- Modify: `frontend/src/views/Profile.vue:34-38`

- [ ] **Step 1: Import checkField API function**

Change the import from `../api/user` (line 198) from:
```javascript
import { getUserProfile, updateProfile, uploadAvatar, uploadBackground } from '../api/user'
```
to:
```javascript
import { getUserProfile, updateProfile, uploadAvatar, uploadBackground, checkField } from '../api/user'
```

- [ ] **Step 2: Add field error state and blur handlers**

Add before the `startEdit` function (before line 975):

```javascript
const fieldErrors = reactive({ email: '', phone: '' })

async function onEmailBlur() {
  fieldErrors.email = ''
  if (!form.email || !form.email.trim()) return
  try {
    await checkField('email', form.email.trim(), user.value.id)
  } catch {
    fieldErrors.email = '该邮箱已被其他用户使用'
  }
}

async function onPhoneBlur() {
  fieldErrors.phone = ''
  if (!form.phone || !form.phone.trim()) return
  try {
    await checkField('phone', form.phone.trim(), user.value.id)
  } catch {
    fieldErrors.phone = '该手机号已被其他用户使用'
  }
}
```

- [ ] **Step 3: Update the email and phone form items with blur handlers and error display**

Replace lines 34-38 from:
```html
<el-form-item label="邮箱">
  <el-input v-model="form.email" />
</el-form-item>
<el-form-item label="手机号">
  <el-input v-model="form.phone" maxlength="20" />
</el-form-item>
```
to:
```html
<el-form-item label="邮箱" :error="fieldErrors.email">
  <el-input v-model="form.email" @blur="onEmailBlur" />
</el-form-item>
<el-form-item label="手机号" :error="fieldErrors.phone">
  <el-input v-model="form.phone" maxlength="20" @blur="onPhoneBlur" />
</el-form-item>
```

- [ ] **Step 4: Clear field errors when entering edit mode**

In the `startEdit` function (line 975-978), add error clearing:

```javascript
function startEdit() {
  form.displayName = user.value.displayName || ''
  form.email = user.value.email || ''; form.phone = user.value.phone || ''; form.bio = user.value.bio || ''
  fieldErrors.email = ''; fieldErrors.phone = ''
  editing.value = true
}
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/Profile.vue
git commit -m "feat: add blur-triggered email/phone uniqueness check on profile edit"
```

---
