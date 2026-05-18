# SMS Verification Code Login + Graphic Captcha Design

**Date**: 2026-05-18
**Status**: Design approved, pending implementation

## Overview

Add SMS verification code login (Tencent Cloud SMS) parallel to existing email login. Add Hutool Captcha graphic verification as a protection layer before sending both email and SMS verification codes.

## Decisions Made

| Question | Decision |
|----------|----------|
| SMS provider | Tencent Cloud SMS |
| Captcha return format | Base64 Data URL in JSON response |
| Captcha trigger timing | Always shown on login page, verified before sending code |
| Captcha TTL | 60 seconds, one-time use (deleted after verification) |
| Login session | Sa-Token, 7 days (same as existing) |

## Graphic Captcha Flow

```
Login page loads → GET /captcha → backend generates Hutool LineCaptcha
  → stores in Redis (key: captcha:{uuid}, TTL: 60s)
  → returns {captchaId: uuid, captchaImage: "data:image/png;base64,..."}
  → User fills captcha code + clicks "获取验证码"
  → Backend validates captcha against Redis → deletes key
  → Proceeds to send email/SMS code
```

## SMS Verification Code Flow

```
User enters phone + captcha → POST /user/send-sms-code
  → validate captcha → generate 6-digit code
  → store in Redis (key: code:login:{phone}, TTL: 60s)
  → send SMS via Tencent Cloud
  → User enters code → POST /user/login-by-sms-code
  → validate against Redis → StpUtil.login() → return token
```

## Backend Changes

### Dependencies (pom.xml)

```xml
<dependency>
    <groupId>com.tencentcloudapi</groupId>
    <artifactId>tencentcloud-sdk-java-sms</artifactId>
    <version>3.1.1058</version>
</dependency>
```

### Configuration (application.yml)

```yaml
sms:
  tencent:
    secret-id: ''
    secret-key: ''
    sdk-app-id: ''
    sign-name: 'ImageSpace'
    template-id: ''
```

### ErrorCode — new codes

```java
PHONE_NOT_BOUND(2011, "该手机号未绑定任何账号"),
CAPTCHA_INVALID(2012, "图形验证码错误或已过期"),
SMS_SEND_FAILED(2013, "短信验证码发送失败"),
```

### New DTOs

**SendSmsCodeDTO.java:**
```java
@Data
public class SendSmsCodeDTO {
    @NotBlank private String phone;
    @NotBlank private String captchaId;
    @NotBlank private String captchaCode;
}
```

**SmsLoginDTO.java:**
```java
@Data
public class SmsLoginDTO {
    @NotBlank private String phone;
    @NotBlank private String code;
}
```

### New Services

**CaptchaService.java:**
- `getCaptcha()` → returns `{captchaId, base64Image}`, stores code in Redis with 60s TTL
- `verify(captchaId, code)` → validates against Redis, deletes key on success, throws CAPTCHA_INVALID on failure

**SmsService.java:**
- `sendVerificationCode(phone, code)` → calls Tencent Cloud SMS API

### Modified: UserServiceImpl

**sendCode()** — add `captchaId` and `captchaCode` params, verify captcha before sending email
**sendSmsCode()** — new: verify captcha → check phone bound → check rate limit → send SMS → store code
**loginBySmsCode()** — new: check phone bound → validate code → delete Redis key → StpUtil.login

### Modified: UserController — new endpoints

```
GET  /captcha                   → public, returns captcha
POST /user/send-code            → modified: add captchaId, captchaCode params
POST /user/send-sms-code        → public, send SMS verification code
POST /user/login-by-sms-code    → public, login with SMS code
```

### Modified: SaTokenConfig

Add `/captcha`, `/user/send-sms-code`, `/user/login-by-sms-code` to whitelist.

## Frontend Changes

### api/user.js

```javascript
export function getCaptcha() { ... }        // GET /captcha
export function sendCode(data) { ... }      // modified: add captcha params
export function sendSmsCode(data) { ... }   // POST /user/send-sms-code
export function loginBySmsCode(data) { ... } // POST /user/login-by-sms-code
```

### Login.vue

- Add graphic captcha image + input to both email and SMS code forms
- Add SMS login sub-tab (phone + captcha + code)
- Captcha image clickable to refresh
- 60s countdown on "获取验证码" button

## Architecture Note

- Captcha Redis key: `captcha:{uuid}`, value: captcha text, TTL: 60s
- SMS code Redis key: `code:login:{phone}`, value: 6-digit string, TTL: 60s
- Email code Redis key: `code:login:{email}`, value: 6-digit string, TTL: 60s
- All follow existing patterns: `LambdaQueryWrapper` + `BusinessException`
- Hutool `CaptchaUtil.createLineCaptcha()` generates the captcha image
