# Email Verification Code Login Design

**Date**: 2026-05-18
**Status**: Design approved, pending implementation

## Overview

Add email verification code login as an alternative to password login. Users with bound emails can receive a 6-digit code via email and use it to log in. The verification code is stored in Redis with a 60s TTL, and the login session lasts 7 days (matching existing Sa-Token config).

## Decisions Made

| Question | Decision |
|----------|----------|
| Coexist with password login? | Yes, tab switch on login page |
| 7-day validity meaning | Sa-Token token TTL (604800s, already configured) |
| Code delivery channel | Email only, QQ SMTP |
| Code format + TTL | 6-digit number, 60 seconds |
| Resend interval | 60s (key exists → reject) |
| User must have bound email? | Yes, email must exist in user profile |

## Core Flow

```
User enters email → POST /user/send-code → backend generates 6-digit code
  → stores in Redis (key: code:login:{email}, TTL: 60s)
  → sends email via JavaMailSender
  → User enters code → POST /user/login-by-code
  → backend validates against Redis → success → StpUtil.login()
  → return token (valid 7 days)
```

## Backend Changes

### Dependencies (pom.xml)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

### Configuration (application.yml)

Add under spring level:
```yaml
  mail:
    host: smtp.qq.com
    port: 587
    username: ''            # user fills in
    password: ''            # QQ authorization code, user fills in
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

### ErrorCode — new codes

```java
EMAIL_NOT_BOUND(2007, "该邮箱未绑定任何账号"),
CODE_SEND_FAILED(2008, "验证码发送失败"),
CODE_INVALID(2009, "验证码错误或已过期"),
CODE_TOO_FREQUENT(2010, "验证码发送过于频繁，请60秒后重试"),
```

### New DTOs

**SendCodeDTO.java:**
```java
@Data
public class SendCodeDTO {
    @NotBlank(message = "邮箱不能为空")
    private String email;
}
```

**CodeLoginDTO.java:**
```java
@Data
public class CodeLoginDTO {
    @NotBlank(message = "邮箱不能为空")
    private String email;
    @NotBlank(message = "验证码不能为空")
    private String code;
}
```

### EmailService.java

Wrapper around JavaMailSender, sends verification code email with HTML template.

### UserService — new methods

```java
void sendCode(String email);
String loginByCode(CodeLoginDTO dto);
```

### UserServiceImpl — new implementations

**sendCode():**
1. Validate email exists in users table (throw EMAIL_NOT_BOUND if not)
2. Check Redis for existing key `code:login:{email}` (throw CODE_TOO_FREQUENT if exists)
3. Generate 6-digit random code
4. Store in Redis via `redisCacheService.put("code:login:" + email, code, Duration.ofSeconds(60))`
5. Send email via EmailService (catch failure → throw CODE_SEND_FAILED)

**loginByCode():**
1. Validate email exists → throw EMAIL_NOT_BOUND
2. Get code from Redis: `code:login:{email}`
3. If null or mismatch → throw CODE_INVALID
4. Delete the Redis key
5. `StpUtil.login(user.getId())` → return token

### UserController — new endpoints

```
POST /user/send-code  → public, whitelist in SaTokenConfig
POST /user/login-by-code → public, whitelist in SaTokenConfig
```

### SaTokenConfig

Add `/user/send-code` and `/user/login-by-code` to `notMatch()` list.

## Frontend Changes

### API layer (api/user.js)

```javascript
export function sendCode(email) {
  return api.post('/user/send-code', { email })
}

export function loginByCode(data) {
  return api.post('/user/login-by-code', data)
}
```

### Login.vue

- Add tab switch: "密码登录" / "验证码登录"
- Verification code form: email input + code input + "获取验证码" button (60s countdown)
- Existing password login form remains unchanged

## Architecture Note

- Redis key pattern: `code:login:{email}`, value is 6-digit string, TTL 60s
- Uses existing `RedisCacheService` (L1 Caffeine + L2 Redis) — `put()` supports TTL directly
- Follows existing validation pattern: `LambdaQueryWrapper` + `BusinessException`
- Sa-Token token TTL (7 days) provides the "7-day remembered" behavior
