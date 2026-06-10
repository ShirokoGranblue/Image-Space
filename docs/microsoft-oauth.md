# Microsoft OAuth2 / OIDC 登录配置指南

## 概述

本项目已集成 Microsoft OAuth2 / OIDC 第三方登录功能，支持 Microsoft 个人账号和组织账号登录。

## Microsoft Entra 应用注册步骤

### 1. 登录 Azure Portal

访问 [Azure Portal](https://portal.azure.com) 并登录。

### 2. 注册应用

1. 进入 **Microsoft Entra ID** > **应用注册** > **新注册**
   - 或直接访问: https://portal.azure.com/#view/Microsoft_AAD_RegisteredApps/ApplicationsListBlade

2. 填写应用信息：
   - **名称**: ImageSpace（或你喜欢的名称）
   - **受支持的账户类型**: 选择 **任何组织目录中的帐户和个人 Microsoft 帐户**
   - **重定向 URI**:
     - 平台: **Web**
     - URI:
       - 开发环境: `http://localhost:8088/user/oauth/microsoft/callback`
       - 生产环境: `https://你的域名/api/user/oauth/microsoft/callback`

3. 点击 **注册**

### 3. 获取凭据

注册成功后：

1. 在应用概览页复制 **应用程序(客户端) ID** → 对应 `MICROSOFT_CLIENT_ID`
2. 进入 **证书和密码** > **客户端密码** > **新客户端密码**
   - 描述: 随意填写
   - 过期时间: 建议 24 个月
   - 复制生成的 **Value**（不是 Secret ID）→ 对应 `MICROSOFT_CLIENT_SECRET`

### 4. 配置 API 权限

1. 进入 **API 权限** > **添加权限** > **Microsoft Graph** > **委托的权限**
2. 添加以下权限：
   - `openid` - 登录
   - `profile` - 查看基本信息
   - `email` - 查看邮箱
   - `User.Read` - 读取用户资料

### 5. 配置环境变量

在项目根目录的 `.env` 文件中添加：

```bash
# Microsoft OAuth2
MICROSOFT_CLIENT_ID=你的应用程序ID
MICROSOFT_CLIENT_SECRET=你的客户端密码
MICROSOFT_REDIRECT_URI=https://你的域名/api/user/oauth/microsoft/callback

# 前端回调地址（可选，不配置则自动检测）
FRONTEND_MICROSOFT_LOGIN_SUCCESS_URL=
FRONTEND_MICROSOFT_LOGIN_FAILURE_URL=
```

### 6. Docker 部署配置

如果使用 docker-compose，确保在 `docker-compose.yaml` 的 backend 服务中已添加：

```yaml
environment:
  MICROSOFT_CLIENT_ID: ${MICROSOFT_CLIENT_ID:-}
  MICROSOFT_CLIENT_SECRET: ${MICROSOFT_CLIENT_SECRET:-}
  MICROSOFT_REDIRECT_URI: ${MICROSOFT_REDIRECT_URI:-}
  FRONTEND_MICROSOFT_LOGIN_SUCCESS_URL: ${FRONTEND_MICROSOFT_LOGIN_SUCCESS_URL:-}
  FRONTEND_MICROSOFT_LOGIN_FAILURE_URL: ${FRONTEND_MICROSOFT_LOGIN_FAILURE_URL:-}
```

## 技术实现说明

### 登录流程

```
前端点击 "Microsoft 登录"
    ↓
后端 /user/oauth/microsoft/login（生成 state，存 Redis）
    ↓
重定向到 Microsoft 授权页面
    ↓
用户登录并授权
    ↓
Microsoft 回调 /user/oauth/microsoft/callback（携带 code + state）
    ↓
后端校验 state（防 CSRF）
    ↓
使用 code 换取 access_token
    ↓
调用 Microsoft Graph /me 获取用户信息
    ↓
根据 Microsoft ID 绑定/创建本地用户
    ↓
生成 Sa-Token（本系统登录态）
    ↓
重定向前端 /login?oauth_code=xxx
    ↓
前端用 oauth_code 换取 sa-token
    ↓
登录完成
```

### 安全特性

- **State CSRF 防护**: 每次登录请求生成随机 state，存入 Redis（5 分钟 TTL），回调时验证并删除
- **一次性 Code**: 登录成功后不直接在 URL 暴露 token，而是生成一次性 code（60 秒 TTL），前端用它换取真正的 sa-token
- **敏感信息保护**: 日志中不打印 client_secret、access_token 等敏感信息
- **用户状态检查**: 被删除/禁用的用户无法通过 OAuth 登录

### 用户绑定逻辑

1. **已有绑定**: 直接登录对应的本地账号
2. **无绑定但有同邮箱账号**: 自动绑定到现有账号
3. **全新用户**: 自动创建本地账号并绑定

### 数据库表

```sql
CREATE TABLE user_oauth_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL,         -- 'microsoft'
    provider_user_id VARCHAR(128) NOT NULL, -- Microsoft 用户 ID
    provider_email VARCHAR(255),
    provider_username VARCHAR(255),
    avatar_url VARCHAR(1024),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_provider_user (provider, provider_user_id),
    KEY idx_user_id (user_id)
);
```

## 测试验证

### 正常登录流程测试

1. 访问 http://localhost:3000/login
2. 点击 "Microsoft" 按钮
3. 应跳转到 Microsoft 登录页面
4. 输入 Microsoft 账号密码
5. 授权后应自动跳回前端并完成登录

### 错误场景测试

- **State 过期**: 等待 5 分钟后再授权，应提示 "安全验证已过期"
- **重复回调**: 使用同一个 callback URL 两次，第二次应失败
- **无邮箱账号**: 某些 Microsoft 账号可能没有邮箱，系统会使用 userPrincipalName

## 生产环境安全建议

1. **使用 HttpOnly Cookie**: 建议改为通过 HttpOnly Cookie 传递 token，避免 token 暴露在 URL 中
2. **配置 HTTPS**: 生产环境必须使用 HTTPS，确保 redirect URI 也是 HTTPS
3. **定期轮换密钥**: 定期更换 Microsoft 客户端密码
4. **监控日志**: 监控 OAuth 相关日志，及时发现异常
5. **限制 redirect URI**: 在 Azure Portal 中严格配置允许的 redirect URI

## 常见问题

### Q: 为什么我的 redirect URI 报错？
A: 确保 Azure Portal 中配置的 redirect URI 与 `.env` 中的 `MICROSOFT_REDIRECT_URI` 完全一致，包括协议和端口。

### Q: Microsoft 登录按钮点击没反应？
A: 检查浏览器控制台是否有错误，确认后端服务正常运行，且 `/user/oauth/microsoft/login` 路径已被正确路由。

### Q: 登录后提示 "登录失败"？
A: 检查后端日志，可能是 token 交换失败或用户信息获取失败。确认 `MICROSOFT_CLIENT_ID` 和 `MICROSOFT_CLIENT_SECRET` 配置正确。
