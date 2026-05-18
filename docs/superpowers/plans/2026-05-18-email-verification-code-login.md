# Email Verification Code Login Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add email verification code login as alternative to password login — 6-digit code sent via QQ SMTP, stored in Redis (60s TTL), login session 7 days via Sa-Token.

**Architecture:** New `EmailService` wraps `JavaMailSender` for sending codes. `UserServiceImpl.sendCode()` generates code → stores in existing `RedisCacheService` → sends email. `UserServiceImpl.loginByCode()` validates code against Redis → `StpUtil.login()`. Follows existing patterns (`LambdaQueryWrapper`, `BusinessException`, `Result<T>`).

**Tech Stack:** Spring Boot 3.2.5 + MyBatis-Plus + Sa-Token + JavaMailSender + Redis (existing RedisCacheService) + Vue 3 + Element Plus

---

### Task 1: Add spring-boot-starter-mail dependency and SMTP config

**Files:**
- Modify: `backend/pom.xml:104`
- Modify: `backend/src/main/resources/application.yml:43`

- [ ] **Step 1: Add mail dependency to pom.xml**

In `backend/pom.xml`, add before the `spring-boot-starter-test` dependency (line 100):

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>
```

- [ ] **Step 2: Add mail config to application.yml**

In `backend/src/main/resources/application.yml`, add after the `cache:` block (after line 28):

```yaml
  mail:
    host: smtp.qq.com
    port: 587
    username: ''
    password: ''
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

- [ ] **Step 3: Commit**

```bash
git add backend/pom.xml backend/src/main/resources/application.yml
git commit -m "feat: add spring-boot-starter-mail and QQ SMTP config"
```

---

### Task 2: ErrorCode — Add verification code error codes

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/common/ErrorCode.java:29`

- [ ] **Step 1: Add 4 new error codes after PHONE_EXISTS**

In `ErrorCode.java`, after line 29 (`PHONE_EXISTS(2006, "该手机号已被其他用户使用"),`), add:

```java
    EMAIL_NOT_BOUND(2007, "该邮箱未绑定任何账号"),
    CODE_SEND_FAILED(2008, "验证码发送失败"),
    CODE_INVALID(2009, "验证码错误或已过期"),
    CODE_TOO_FREQUENT(2010, "验证码发送过于频繁，请60秒后重试"),
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/common/ErrorCode.java
git commit -m "feat: add verification code error codes (EMAIL_NOT_BOUND, CODE_SEND_FAILED, CODE_INVALID, CODE_TOO_FREQUENT)"
```

---

### Task 3: Create SendCodeDTO and CodeLoginDTO

**Files:**
- Create: `backend/src/main/java/com/picmgmt/dto/SendCodeDTO.java`
- Create: `backend/src/main/java/com/picmgmt/dto/CodeLoginDTO.java`

- [ ] **Step 1: Create SendCodeDTO.java**

```java
package com.picmgmt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendCodeDTO {

    @NotBlank(message = "邮箱不能为空")
    private String email;
}
```

- [ ] **Step 2: Create CodeLoginDTO.java**

```java
package com.picmgmt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CodeLoginDTO {

    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/picmgmt/dto/SendCodeDTO.java backend/src/main/java/com/picmgmt/dto/CodeLoginDTO.java
git commit -m "feat: add SendCodeDTO and CodeLoginDTO for verification code login"
```

---

### Task 4: Create EmailService

**Files:**
- Create: `backend/src/main/java/com/picmgmt/service/EmailService.java`
- Create: `backend/src/main/java/com/picmgmt/service/impl/EmailServiceImpl.java`

- [ ] **Step 1: Create EmailService interface**

```java
package com.picmgmt.service;

public interface EmailService {

    void sendVerificationCode(String to, String code);
}
```

- [ ] **Step 2: Create EmailServiceImpl**

```java
package com.picmgmt.service.impl;

import com.picmgmt.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendVerificationCode(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("ImageSpace 登录验证码");
            helper.setText(String.format("""
                    <div style="font-family:sans-serif;max-width:480px;margin:0 auto;">
                        <h2 style="color:#2563eb;">ImageSpace 登录验证码</h2>
                        <p>您的验证码是：</p>
                        <div style="font-size:32px;font-weight:bold;color:#2563eb;
                                    padding:16px 24px;background:#eff4ff;border-radius:8px;
                                    text-align:center;letter-spacing:6px;">%s</div>
                        <p style="color:#6b7a8d;margin-top:16px;">验证码60秒内有效，请勿泄露给他人。</p>
                    </div>
                    """, code), true);
            mailSender.send(message);
            log.info("Verification code sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send verification code to {}", to, e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/picmgmt/service/EmailService.java backend/src/main/java/com/picmgmt/service/impl/EmailServiceImpl.java
git commit -m "feat: add EmailService for sending verification codes"
```

---

### Task 5: UserService + UserServiceImpl — Add sendCode and loginByCode

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/service/UserService.java:30`
- Modify: `backend/src/main/java/com/picmgmt/service/impl/UserServiceImpl.java`

- [ ] **Step 1: Add sendCode and loginByCode to UserService interface**

In `UserService.java`, add after `checkField()` method (before closing `}`):

```java
    void sendCode(String email);

    String loginByCode(CodeLoginDTO dto);
```

Also add import for `CodeLoginDTO`:
```java
import com.picmgmt.dto.CodeLoginDTO;
```

- [ ] **Step 2: Implement sendCode() and loginByCode() in UserServiceImpl**

Read the current `UserServiceImpl.java` file first. Add these new dependencies and methods:

Add new field to constructor dependencies after `userRepository`:
```java
    private final RedisCacheService redisCacheService;
    private final EmailService emailService;
```

Add import for `CodeLoginDTO`:
```java
import com.picmgmt.dto.CodeLoginDTO;
```

Add import for `RedisCacheService`:
```java
import com.picmgmt.cache.RedisCacheService;
```

Add import for `EmailService`:
```java
import com.picmgmt.service.EmailService;
```

Add import for `Duration` and `ThreadLocalRandom`:
```java
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
```

Add these two methods before the closing `}` of the class:

```java
@Override
public void sendCode(String email) {
    User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getEmail, email));
    if (user == null) {
        throw new BusinessException(ErrorCode.EMAIL_NOT_BOUND);
    }
    String redisKey = "code:login:" + email;
    if (redisCacheService.get(redisKey, String.class).isPresent()) {
        throw new BusinessException(ErrorCode.CODE_TOO_FREQUENT);
    }
    String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    try {
        emailService.sendVerificationCode(email, code);
    } catch (Exception e) {
        throw new BusinessException(ErrorCode.CODE_SEND_FAILED);
    }
    redisCacheService.put(redisKey, code, Duration.ofSeconds(60));
}

@Override
public String loginByCode(CodeLoginDTO dto) {
    String email = dto.getEmail().trim();
    User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getEmail, email));
    if (user == null) {
        throw new BusinessException(ErrorCode.EMAIL_NOT_BOUND);
    }
    String redisKey = "code:login:" + email;
    String storedCode = redisCacheService.get(redisKey, String.class).orElse(null);
    if (storedCode == null || !storedCode.equals(dto.getCode().trim())) {
        throw new BusinessException(ErrorCode.CODE_INVALID);
    }
    redisCacheService.evict(redisKey);
    StpUtil.login(user.getId());
    return StpUtil.getTokenValue();
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/picmgmt/service/UserService.java backend/src/main/java/com/picmgmt/service/impl/UserServiceImpl.java
git commit -m "feat: add sendCode and loginByCode for email verification code login"
```

---

### Task 6: UserController — Add /user/send-code and /user/login-by-code endpoints

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/controller/UserController.java:121`

- [ ] **Step 1: Add new endpoints**

Add imports for new DTOs:
```java
import com.picmgmt.dto.SendCodeDTO;
import com.picmgmt.dto.CodeLoginDTO;
```

Add before the closing `}`:

```java
@Operation(summary = "发送邮箱验证码")
@PostMapping("/send-code")
public Result<Void> sendCode(@Valid @RequestBody SendCodeDTO dto) {
    userService.sendCode(dto.getEmail().trim());
    return Result.ok();
}

@Operation(summary = "邮箱验证码登录")
@PostMapping("/login-by-code")
public Result<String> loginByCode(@Valid @RequestBody CodeLoginDTO dto) {
    return Result.ok(userService.loginByCode(dto));
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/controller/UserController.java
git commit -m "feat: add POST /user/send-code and /user/login-by-code endpoints"
```

---

### Task 7: SaTokenConfig — Whitelist new endpoints

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/config/SaTokenConfig.java:17-21`

- [ ] **Step 1: Add /user/send-code and /user/login-by-code to whitelist**

In `SaTokenConfig.java`, change the `notMatch()` list from:
```java
                            .notMatch("/user/login", "/user/register",
                                    "/doc.html", "/v3/api-docs/**", "/swagger-ui/**",
                                    "/image/square", "/user/profile/**",
                                    "/user/check-field",
                                    "/comment/list/**")
```
to:
```java
                            .notMatch("/user/login", "/user/register",
                                    "/doc.html", "/v3/api-docs/**", "/swagger-ui/**",
                                    "/image/square", "/user/profile/**",
                                    "/user/check-field",
                                    "/user/send-code", "/user/login-by-code",
                                    "/comment/list/**")
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/config/SaTokenConfig.java
git commit -m "feat: whitelist /user/send-code and /user/login-by-code in SaTokenConfig"
```

---

### Task 8: Frontend API — Add sendCode and loginByCode functions

**Files:**
- Modify: `frontend/src/api/user.js:41`

- [ ] **Step 1: Add sendCode and loginByCode exports**

In `frontend/src/api/user.js`, add after the `checkField` function (after line 41):

```javascript
export function sendCode(email) {
  return api.post('/user/send-code', { email })
}

export function loginByCode(data) {
  return api.post('/user/login-by-code', data)
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/api/user.js
git commit -m "feat: add sendCode and loginByCode API functions"
```

---

### Task 9: Login.vue — Add verification code login tab

**Files:**
- Modify: `frontend/src/views/Login.vue`

- [ ] **Step 1: Add tab switch markup in template**

Add after the `p.tagline` element (after line 14) and before the password login form (before line 16):

```html
<el-tabs v-model="loginMode" class="login-tabs">
  <el-tab-pane label="密码登录" name="password"></el-tab-pane>
  <el-tab-pane label="验证码登录" name="code"></el-tab-pane>
</el-tabs>
```

- [ ] **Step 2: Wrap password login form in v-show**

Wrap the existing password form (lines 16-29) with:
```html
<div v-show="loginMode === 'password'">
```
and closing `</div>`.

- [ ] **Step 3: Add verification code login form**

After the wrapped password form, add:

```html
<div v-show="loginMode === 'code'" class="login-form">
  <el-form-item label="邮箱">
    <el-input v-model="codeForm.email" placeholder="your@email.com" size="large" :prefix-icon="Message" />
  </el-form-item>
  <el-form-item label="验证码">
    <div style="display:flex;gap:8px;width:100%">
      <el-input v-model="codeForm.code" placeholder="6位数字" size="large" maxlength="6" style="flex:1" />
      <el-button size="large" @click="handleSendCode" :loading="sending" :disabled="countdown > 0" style="min-width:120px">
        {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
      </el-button>
    </div>
  </el-form-item>
  <el-form-item>
    <el-button type="primary" size="large" class="login-btn" @click="handleCodeLogin" :loading="loading">
      验证并登录
    </el-button>
  </el-form-item>
</div>
```

- [ ] **Step 4: Update script section**

Add `Message` icon import:
```javascript
import { User, Lock, Message } from '@element-plus/icons-vue'
```

Add `sendCode`, `loginByCode` imports:
```javascript
import { login, sendCode, loginByCode } from '../api/user'
```

Add new reactive state after `loading`:
```javascript
const loginMode = ref('password')
const sending = ref(false)
const countdown = ref(0)
let countdownTimer = null

const codeForm = reactive({
  email: '',
  code: ''
})
```

Add `handleSendCode` function:
```javascript
async function handleSendCode() {
  if (!codeForm.email) { ElMessage.warning('请输入邮箱'); return }
  sending.value = true
  try {
    await sendCode(codeForm.email.trim())
    ElMessage.success('验证码已发送')
    countdown.value = 60
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(countdownTimer)
    }, 1000)
  } catch {} finally { sending.value = false }
}
```

Add `handleCodeLogin` function:
```javascript
async function handleCodeLogin() {
  if (!codeForm.email) { ElMessage.warning('请输入邮箱'); return }
  if (!codeForm.code) { ElMessage.warning('请输入验证码'); return }
  loading.value = true
  try {
    const res = await loginByCode({ email: codeForm.email.trim(), code: codeForm.code.trim() })
    userStore.setToken(res.data)
    await userStore.fetchUserInfo()
    ElMessage.success('欢迎回来')
    router.push('/home')
  } catch {} finally { loading.value = false }
}
```

- [ ] **Step 5: Add style for login tabs**

At the end of `<style scoped>`, add before `@media`:

```css
.login-tabs { margin-bottom: var(--space-md); }
.login-tabs :deep(.el-tabs__header) { margin-bottom: 0; }
.login-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }
.login-tabs :deep(.el-tabs__item) {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 550;
}
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/views/Login.vue
git commit -m "feat: add email verification code login tab to Login page"
```

---
