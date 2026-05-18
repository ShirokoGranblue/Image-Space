# SMS Verification Code Login + Graphic Captcha Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add SMS verification code login via Tencent Cloud SMS, plus Hutool Captcha graphic verification as protection for both email and SMS code sending.

**Architecture:** New `CaptchaService` wraps Hutool `CaptchaUtil` for graphic captcha generation/verification with Redis storage. New `SmsService` wraps Tencent Cloud SMS SDK. `UserServiceImpl.sendCode()` gains captcha verification. Parallel SMS methods (`sendSmsCode`, `loginBySmsCode`) mirror existing email methods. All captcha/code data stored in existing `RedisCacheService` with 60s TTL.

**Tech Stack:** Spring Boot 3.2.5 + MyBatis-Plus + Sa-Token + RedisCacheService + Hutool CaptchaUtil + Tencent Cloud SMS SDK + Vue 3 + Element Plus

---

### Task 1: pom.xml + application.yml — SMS SDK and config

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`

- [ ] **Step 1: Add Tencent Cloud SMS SDK to pom.xml**

Read `pom.xml` first. Add before the closing `</dependencies>` tag (after `spring-boot-starter-mail`):

```xml
        <dependency>
            <groupId>com.tencentcloudapi</groupId>
            <artifactId>tencentcloud-sdk-java-sms</artifactId>
            <version>3.1.1058</version>
        </dependency>
```

- [ ] **Step 2: Add SMS config to application.yml**

Read `application.yml` first. Add at the end of the file:

```yaml
sms:
  tencent:
    secret-id: ''
    secret-key: ''
    sdk-app-id: ''
    sign-name: 'ImageSpace'
    template-id: ''
```

- [ ] **Step 3: Commit**

```bash
git add backend/pom.xml backend/src/main/resources/application.yml
git commit -m "feat: add Tencent Cloud SMS SDK and config"
```

---

### Task 2: ErrorCode + New DTOs

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/common/ErrorCode.java`
- Modify: `backend/src/main/java/com/picmgmt/dto/SendCodeDTO.java`
- Create: `backend/src/main/java/com/picmgmt/dto/SendSmsCodeDTO.java`
- Create: `backend/src/main/java/com/picmgmt/dto/SmsLoginDTO.java`

- [ ] **Step 1: Add 3 new error codes to ErrorCode**

Read `ErrorCode.java` first. After `CODE_TOO_FREQUENT(2010, ...)` add:

```java
    PHONE_NOT_BOUND(2011, "该手机号未绑定任何账号"),
    CAPTCHA_INVALID(2012, "图形验证码错误或已过期"),
    SMS_SEND_FAILED(2013, "短信验证码发送失败"),
```

- [ ] **Step 2: Modify SendCodeDTO to add captcha fields**

Read `SendCodeDTO.java`. Replace with:

```java
package com.picmgmt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendCodeDTO {

    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "验证码ID不能为空")
    private String captchaId;

    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;
}
```

- [ ] **Step 3: Create SendSmsCodeDTO.java**

```java
package com.picmgmt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendSmsCodeDTO {

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "验证码ID不能为空")
    private String captchaId;

    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;
}
```

- [ ] **Step 4: Create SmsLoginDTO.java**

```java
package com.picmgmt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SmsLoginDTO {

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/picmgmt/common/ErrorCode.java backend/src/main/java/com/picmgmt/dto/SendCodeDTO.java backend/src/main/java/com/picmgmt/dto/SendSmsCodeDTO.java backend/src/main/java/com/picmgmt/dto/SmsLoginDTO.java
git commit -m "feat: add SMS error codes and DTOs (SendSmsCodeDTO, SmsLoginDTO, update SendCodeDTO)"
```

---

### Task 3: CaptchaService + Impl

**Files:**
- Create: `backend/src/main/java/com/picmgmt/service/CaptchaService.java`
- Create: `backend/src/main/java/com/picmgmt/service/impl/CaptchaServiceImpl.java`

- [ ] **Step 1: Create CaptchaService interface**

```java
package com.picmgmt.service;

import java.util.Map;

public interface CaptchaService {

    Map<String, String> getCaptcha();

    void verify(String captchaId, String code);
}
```

- [ ] **Step 2: Create CaptchaServiceImpl**

```java
package com.picmgmt.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.picmgmt.cache.RedisCacheService;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private final RedisCacheService redisCacheService;

    @Override
    public Map<String, String> getCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(200, 80, 4, 150);
        captcha.createCode();
        String captchaId = UUID.randomUUID().toString();
        String code = captcha.getCode();
        redisCacheService.put("captcha:" + captchaId, code, Duration.ofSeconds(60));
        return Map.of(
            "captchaId", captchaId,
            "captchaImage", "data:image/png;base64," + captcha.getImageBase64Data()
        );
    }

    @Override
    public void verify(String captchaId, String code) {
        String key = "captcha:" + captchaId;
        String storedCode = redisCacheService.get(key, String.class).orElse(null);
        if (storedCode == null || !storedCode.equalsIgnoreCase(code)) {
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }
        redisCacheService.evict(key);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/picmgmt/service/CaptchaService.java backend/src/main/java/com/picmgmt/service/impl/CaptchaServiceImpl.java
git commit -m "feat: add CaptchaService with Hutool LineCaptcha + Redis storage"
```

---

### Task 4: SmsService + Impl

**Files:**
- Create: `backend/src/main/java/com/picmgmt/service/SmsService.java`
- Create: `backend/src/main/java/com/picmgmt/service/impl/SmsServiceImpl.java`

- [ ] **Step 1: Create SmsService interface**

```java
package com.picmgmt.service;

public interface SmsService {

    void sendVerificationCode(String phone, String code);
}
```

- [ ] **Step 2: Create SmsServiceImpl**

```java
package com.picmgmt.service.impl;

import com.picmgmt.service.SmsService;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Value("${sms.tencent.secret-id}")
    private String secretId;

    @Value("${sms.tencent.secret-key}")
    private String secretKey;

    @Value("${sms.tencent.sdk-app-id}")
    private String sdkAppId;

    @Value("${sms.tencent.sign-name}")
    private String signName;

    @Value("${sms.tencent.template-id}")
    private String templateId;

    @Override
    public void sendVerificationCode(String phone, String code) {
        try {
            Credential cred = new Credential(secretId, secretKey);
            SmsClient client = new SmsClient(cred, "ap-guangzhou");
            SendSmsRequest req = new SendSmsRequest();
            req.setSmsSdkAppId(sdkAppId);
            req.setSignName(signName);
            req.setTemplateId(templateId);
            req.setTemplateParamSet(new String[]{code});
            req.setPhoneNumberSet(new String[]{"+86" + phone});
            client.SendSms(req);
            log.info("SMS code sent to {}", phone);
        } catch (TencentCloudSDKException e) {
            log.error("Failed to send SMS to {}", phone, e);
            throw new RuntimeException("短信发送失败", e);
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/picmgmt/service/SmsService.java backend/src/main/java/com/picmgmt/service/impl/SmsServiceImpl.java
git commit -m "feat: add SmsService for Tencent Cloud SMS verification codes"
```

---

### Task 5: UserService + UserServiceImpl — Captcha integration + SMS methods

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/service/UserService.java`
- Modify: `backend/src/main/java/com/picmgmt/service/impl/UserServiceImpl.java`

- [ ] **Step 1: Update UserService interface**

Read `UserService.java`. Change `sendCode` signature to accept captcha params. Add SMS methods:

```java
    void sendCode(String email, String captchaId, String captchaCode);

    String loginByCode(CodeLoginDTO dto);

    void sendSmsCode(String phone, String captchaId, String captchaCode);

    String loginBySmsCode(SmsLoginDTO dto);
```

Add these imports:
```java
import com.picmgmt.dto.SmsLoginDTO;
```

- [ ] **Step 2: Update UserServiceImpl — add captcha verification to sendCode, add SMS methods**

Read `UserServiceImpl.java`. Add new dependency fields:
```java
    private final CaptchaService captchaService;
    private final SmsService smsService;
```

Add imports:
```java
import com.picmgmt.dto.SmsLoginDTO;
import com.picmgmt.service.CaptchaService;
import com.picmgmt.service.SmsService;
```

Modify `sendCode()` — add captcha verification at the beginning:
```java
@Override
public void sendCode(String email, String captchaId, String captchaCode) {
    captchaService.verify(captchaId, captchaCode);
    // ... rest unchanged
```

Add `sendSmsCode()` method:
```java
@Override
public void sendSmsCode(String phone, String captchaId, String captchaCode) {
    captchaService.verify(captchaId, captchaCode);
    User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    if (user == null) {
        throw new BusinessException(ErrorCode.PHONE_NOT_BOUND);
    }
    String redisKey = "code:login:" + phone;
    if (redisCacheService.get(redisKey, String.class).isPresent()) {
        throw new BusinessException(ErrorCode.CODE_TOO_FREQUENT);
    }
    String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    try {
        smsService.sendVerificationCode(phone, code);
    } catch (Exception e) {
        throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
    }
    redisCacheService.put(redisKey, code, Duration.ofSeconds(60));
}
```

Add `loginBySmsCode()` method:
```java
@Override
public String loginBySmsCode(SmsLoginDTO dto) {
    String phone = dto.getPhone().trim();
    User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    if (user == null) {
        throw new BusinessException(ErrorCode.PHONE_NOT_BOUND);
    }
    String redisKey = "code:login:" + phone;
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
git commit -m "feat: add captcha verification to sendCode, add sendSmsCode and loginBySmsCode"
```

---

### Task 6: UserController — Captcha + SMS endpoints + update send-code

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/controller/UserController.java`

- [ ] **Step 1: Add GET /captcha and update+add endpoints**

Read `UserController.java`. Add new imports:
```java
import com.picmgmt.dto.SendSmsCodeDTO;
import com.picmgmt.dto.SmsLoginDTO;
import com.picmgmt.service.CaptchaService;
```

Add `CaptchaService` field:
```java
    private final CaptchaService captchaService;
```

Add GET /captcha before the user endpoints:
```java
@Operation(summary = "获取图形验证码")
@GetMapping("/captcha")
public Result<Map<String, String>> captcha() {
    return Result.ok(captchaService.getCaptcha());
}
```

Update sendCode to pass captcha params:
```java
@Operation(summary = "发送邮箱验证码")
@PostMapping("/send-code")
public Result<Void> sendCode(@Valid @RequestBody SendCodeDTO dto) {
    userService.sendCode(dto.getEmail().trim(), dto.getCaptchaId(), dto.getCaptchaCode());
    return Result.ok();
}
```

Add SMS endpoints before closing `}`:
```java
@Operation(summary = "发送短信验证码")
@PostMapping("/send-sms-code")
public Result<Void> sendSmsCode(@Valid @RequestBody SendSmsCodeDTO dto) {
    userService.sendSmsCode(dto.getPhone().trim(), dto.getCaptchaId(), dto.getCaptchaCode());
    return Result.ok();
}

@Operation(summary = "短信验证码登录")
@PostMapping("/login-by-sms-code")
public Result<String> loginBySmsCode(@Valid @RequestBody SmsLoginDTO dto) {
    return Result.ok(userService.loginBySmsCode(dto));
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/picmgmt/controller/UserController.java
git commit -m "feat: add GET /captcha, update /user/send-code, add /user/send-sms-code and /user/login-by-sms-code"
```

---

### Task 7: SaTokenConfig + Frontend API

**Files:**
- Modify: `backend/src/main/java/com/picmgmt/config/SaTokenConfig.java`
- Modify: `frontend/src/api/user.js`

- [ ] **Step 1: Whitelist new endpoints in SaTokenConfig**

Read `SaTokenConfig.java`. Add to `notMatch()`:
```java
                            .notMatch("/user/login", "/user/register",
                                    "/doc.html", "/v3/api-docs/**", "/swagger-ui/**",
                                    "/image/square", "/user/profile/**",
                                    "/user/check-field",
                                    "/user/send-code", "/user/login-by-code",
                                    "/user/send-sms-code", "/user/login-by-sms-code",
                                    "/captcha",
                                    "/comment/list/**")
```

- [ ] **Step 2: Add frontend API functions**

Read `frontend/src/api/user.js`. Update/add:

Update `sendCode` to accept data with captcha fields:
```javascript
export function sendCode(data) {
  return api.post('/user/send-code', data)
}
```

Add new functions:
```javascript
export function getCaptcha() {
  return api.get('/captcha')
}

export function sendSmsCode(data) {
  return api.post('/user/send-sms-code', data)
}

export function loginBySmsCode(data) {
  return api.post('/user/login-by-sms-code', data)
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/picmgmt/config/SaTokenConfig.java frontend/src/api/user.js
git commit -m "feat: whitelist new endpoints + add captcha/SMS frontend API functions"
```

---

### Task 8: Login.vue — Captcha UI + SMS login tab

**Files:**
- Modify: `frontend/src/views/Login.vue`

- [ ] **Step 1: Update script imports and state**

Read `Login.vue` first. Update API imports:
```javascript
import { login, sendCode, loginByCode, getCaptcha, sendSmsCode, loginBySmsCode } from '../api/user'
```

Add new reactive state (after existing codeForm/countdown etc):
```javascript
const captchaId = ref('')
const captchaImage = ref('')
const smsForm = reactive({ phone: '', code: '' })
const smsSending = ref(false)
const smsCountdown = ref(0)
let smsCountdownTimer = null
```

Add fetchCaptcha function:
```javascript
async function fetchCaptcha() {
  try {
    const res = await getCaptcha()
    captchaId.value = res.data.captchaId
    captchaImage.value = res.data.captchaImage
  } catch {}
}
```

Call `fetchCaptcha()` on mount and when switching to code login tab.

- [ ] **Step 2: Add captcha UI to email code form**

In the email verification code form, add before the code input row:

```html
<el-form-item label="图形验证码">
  <div style="display:flex;gap:8px;align-items:center">
    <el-input v-model="captchaCode" placeholder="4位验证码" size="large" maxlength="4" style="flex:1" />
    <img :src="captchaImage" @click="fetchCaptcha" style="height:40px;cursor:pointer;border-radius:4px" title="点击刷新" />
  </div>
</el-form-item>
```

Add `captchaCode` to reactive state: `const captchaCode = ref('')`

- [ ] **Step 3: Add SMS login tab and form**

Add third tab pane:
```html
<el-tab-pane label="短信登录" name="sms"></el-tab-pane>
```

Add SMS form (v-show="loginMode === 'sms'"):
```html
<div v-show="loginMode === 'sms'" class="login-form">
  <el-form-item label="手机号">
    <el-input v-model="smsForm.phone" placeholder="输入已绑定的手机号" size="large" :prefix-icon="Phone" maxlength="11" />
  </el-form-item>
  <el-form-item label="图形验证码">
    <div style="display:flex;gap:8px;align-items:center">
      <el-input v-model="smsCaptchaCode" placeholder="4位验证码" size="large" maxlength="4" style="flex:1" />
      <img :src="captchaImage" @click="fetchCaptcha" style="height:40px;cursor:pointer" title="点击刷新" />
    </div>
  </el-form-item>
  <el-form-item label="短信验证码">
    <div style="display:flex;gap:8px;width:100%">
      <el-input v-model="smsForm.code" placeholder="6位数字" size="large" maxlength="6" style="flex:1" />
      <el-button size="large" @click="handleSendSmsCode" :loading="smsSending" :disabled="smsCountdown > 0" style="min-width:120px">
        {{ smsCountdown > 0 ? smsCountdown + 's' : '获取验证码' }}
      </el-button>
    </div>
  </el-form-item>
  <el-form-item>
    <el-button type="primary" size="large" class="login-btn" @click="handleSmsLogin" :loading="loading">
      验证并登录
    </el-button>
  </el-form-item>
</div>
```

- [ ] **Step 4: Add SMS handlers**

Handle send SMS code:
```javascript
async function handleSendSmsCode() {
  if (!smsForm.phone) { ElMessage.warning('请输入手机号'); return }
  if (!smsCaptchaCode.value) { ElMessage.warning('请输入图形验证码'); return }
  smsSending.value = true
  try {
    await sendSmsCode({ phone: smsForm.phone.trim(), captchaId: captchaId.value, captchaCode: smsCaptchaCode.value })
    ElMessage.success('验证码已发送')
    fetchCaptcha(); smsCaptchaCode.value = ''
    smsCountdown.value = 60
    smsCountdownTimer = setInterval(() => { smsCountdown.value--; if (smsCountdown.value <= 0) clearInterval(smsCountdownTimer) }, 1000)
  } catch {} finally { smsSending.value = false }
}
```

Handle SMS login:
```javascript
async function handleSmsLogin() {
  if (!smsForm.phone) { ElMessage.warning('请输入手机号'); return }
  if (!smsForm.code) { ElMessage.warning('请输入验证码'); return }
  loading.value = true
  try {
    const res = await loginBySmsCode({ phone: smsForm.phone.trim(), code: smsForm.code.trim() })
    userStore.setToken(res.data)
    await userStore.fetchUserInfo()
    ElMessage.success('欢迎回来')
    router.push('/home')
  } catch {} finally { loading.value = false }
}
```

- [ ] **Step 5: Update handleSendCode to include captcha**

Modify handleSendCode to pass captcha params and refresh captcha after:
```javascript
async function handleSendCode() {
  if (!codeForm.email) { ElMessage.warning('请输入邮箱'); return }
  if (!captchaCode.value) { ElMessage.warning('请输入图形验证码'); return }
  sending.value = true
  try {
    await sendCode({ email: codeForm.email.trim(), captchaId: captchaId.value, captchaCode: captchaCode.value })
    ElMessage.success('验证码已发送')
    fetchCaptcha(); captchaCode.value = ''
    countdown.value = 60
    countdownTimer = setInterval(() => { countdown.value--; if (countdown.value <= 0) clearInterval(countdownTimer) }, 1000)
  } catch {} finally { sending.value = false }
}
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/views/Login.vue
git commit -m "feat: add graphic captcha UI, SMS login tab, and captcha integration to email/sms code forms"
```

---
