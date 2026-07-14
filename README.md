<p align="center">
  <img src="docs/logo.png" alt="Image Space Logo" width="120" />
</p>

<h1 align="center">🌌 Image Space</h1>

<p align="center">
  <strong>一个功能丰富的全栈图片管理系统 —— 上传、整理、分享你的图片世界</strong>
</p>

<p align="center">
  <a href="https://image-space.app">🌐 在线体验</a> ·
  <a href="#-快速开始">🚀 快速开始</a> ·
  <a href="#-功能特性">✨ 功能特性</a> ·
  <a href="#-技术栈">🛠 技术栈</a> ·
  <a href="#-部署指南">📦 部署指南</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen?style=flat-square&logo=springboot" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk" alt="Java" />
  <img src="https://img.shields.io/badge/Vue.js-3.4-4FC08D?style=flat-square&logo=vue.js" alt="Vue 3" />
  <img src="https://img.shields.io/badge/Vite-5-646CFF?style=flat-square&logo=vite" alt="Vite" />
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white" alt="MySQL" />
  <img src="https://img.shields.io/badge/Redis-7-DC382D?style=flat-square&logo=redis&logoColor=white" alt="Redis" />
  <img src="https://img.shields.io/badge/Cloudflare-R2-F38020?style=flat-square&logo=cloudflare" alt="Cloudflare R2" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white" alt="Docker" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License" />
</p>

---

## 📖 目录

- [功能特性](#-功能特性)
- [技术栈](#-技术栈)
- [系统架构](#-系统架构)
- [快速开始](#-快速开始)
- [Docker 部署](#-docker-部署)
- [项目结构](#-项目结构)
- [API 文档](#-api-文档)
- [路由与页面](#-路由与页面)
- [权限系统](#-权限系统)
- [环境变量](#-环境变量)
- [贡献指南](#-贡献指南)
- [许可证](#-许可证)

---

## ✨ 功能特性

### 🖼 图片管理
- **上传与存储** — 支持 JPG / PNG / JPEG / WEBP 格式，单文件最大 20MB，多文件批量上传
- **分类管理** — 在图片页、上传和详情流程中创建、编辑、删除分类；删除分类时图片自动归为"未分类"（无独立 `/categories` 页面）
- **搜索与筛选** — 按名称模糊搜索、按分类筛选、按时间排序
- **分页浏览** — 支持 30 / 50 / 100 条/页，服务端内存分页

### 🏛 图片广场
- **公共画廊** — 浏览所有公开分享的图片
- **点赞互动** — 对喜欢的图片点赞 ❤️
- **评论系统** — 支持富文本评论与图片评论

### 👤 用户系统
- **多种登录** — 账号密码、邮箱验证码、GitHub OAuth、Google OAuth
- **人机验证** — Cloudflare Turnstile 保护登录、注册、发送验证码
- **个人主页** — 可自定义头像（裁剪上传）、背景图片、个人资料
- **找回密码** — 通过邮箱验证码重置密码

### 🔐 权限与安全
- **RBAC 权限** — 基于角色的访问控制（管理员 / 版主 / 普通用户）
- **细粒度权限** — 11 种权限码，精确控制上传、编辑、删除、管理等操作
- **密码加密** — BCrypt 哈希存储，Hutool 实现
- **SQL 注入防护** — 排序字段白名单校验

### 🛠 管理后台
- **用户管理** — 查看、编辑用户角色、删除用户
- **权限管理** — 通过数据库表链管理角色与权限

### 🎆 其他亮点
- **粒子动效背景** — 可配置的动态粒子背景，多种预设效果
- **通知系统** — 实时通知铃铛、抽屉面板、批量已读/删除
- **CDN 缓存刷新** — 媒体 URL 附加 SHA256 版本参数，自动刷新 Cloudflare 缓存
- **自动数据迁移** — Flyway 自动升级旧 Schema，并将旧版 Base64 / 文件路径数据修复到 R2

---

## 🛠 技术栈

### 后端

| 组件 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 3.2.3 |
| 语言 | Java (JDK) | 21 |
| ORM | MyBatis-Plus | 3.5.5 |
| 认证 | Sa-Token | 1.38.0 |
| OAuth | JustAuth | 1.4.0 |
| 对象存储 | Cloudflare R2（MinIO S3 SDK） | 8.6.0 |
| Schema 迁移 | Flyway | 11.7.2 |
| 缓存 | Caffeine + Redis | 3.1.8 / 7 |
| API 文档 | SpringDoc + Knife4j | 2.3.0 / 4.4.0 |
| 工具库 | Hutool | 5.8.25 |
| 邮件 | Spring Boot Mail | — |

### 前端

| 组件 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue.js | 3.4 |
| 构建工具 | Vite | 5.x |
| UI 库 | Element Plus | 2.7+ |
| 状态管理 | Pinia | 2.x |
| 路由 | Vue Router | 4.x |
| HTTP 客户端 | Axios | 1.6+ |
| 测试 | Vitest + Vue Test Utils | — |
| 样式 | SCSS + Element Plus | — |

### 基础设施

| 组件 | 技术 |
|------|------|
| 数据库 | MySQL 8.0 |
| 缓存/会话 | Redis 7 |
| 对象存储 | Cloudflare R2 |
| 反向代理 | Nginx |
| 容器化 | Docker Compose |
| SSL | Cloudflare Origin Certificate |
| CDN | Cloudflare |
| 云服务器 | Azure VM (Ubuntu 24.04) |

---

## 🏗 系统架构

```
                    ┌─────────────────────────────────────────┐
                    │              Cloudflare CDN              │
                    │         (DNS / SSL / Turnstile)          │
                    └─────────────────┬───────────────────────┘
                                      │
                    ┌─────────────────▼───────────────────────┐
                    │           Nginx (80 / 443)               │
                    │                                          │
                    │  /              → Vue SPA (静态文件)       │
                    │  /api/          → Spring Boot (:8088)    │
                    │  public/private → R2 Worker / CDN         │
                    │  /doc.html      → Knife4j API 文档        │
                    └───┬────────────────┬──────────────┬──────┘
                        │                │              │
              ┌─────────▼──┐    ┌───────▼─────┐  ┌────────────┐
              │  Frontend   │    │   Backend   │  │ R2/Worker  │
              │  (Vue SPA)  │    │ Spring Boot │  │ Object CDN │
              │             │    │   :8088     │  │ (external) │
              └─────────────┘    └──┬────┬────┘  └────────────┘
                                    │    │
                              ┌─────▼┐  ┌▼──────┐
                              │MySQL │  │ Redis  │
                              │ 8.0  │  │   7    │
                              └──────┘  └───────┘
```

### 后端分层架构

```
Controller (REST API)
    │
    ├── DTO / VO          ← 请求/响应数据传输对象
    │
    ▼
Service / ServiceImpl     ← 业务逻辑（权限校验、数据处理）
    │
    ▼
Mapper (MyBatis-Plus)     ← 数据访问层
    │
    ▼
Entity (@TableName)       ← 数据库实体映射
```

---

## 🚀 快速开始

### 前置要求

- **JDK 21** + **Maven 3.9+**
- **Node.js 18+** + **npm**
- **MySQL 8.0**
- **Redis 7**
- **Cloudflare R2 凭据和 Worker/CDN 路由**

### 1️⃣ 克隆项目

```bash
git clone https://github.com/ShirokoGranblue/Picture-Managentor.git
cd Picture-Managentor
```

### 2️⃣ 创建数据库

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS picture_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
```

### 3️⃣ 配置后端

```bash
cd backend
cp src/main/resources/application.example.yml src/main/resources/application.yml
# 编辑 application.yml，配置数据库、Redis 和 R2 连接信息
```

### 4️⃣ 启动后端

```bash
mvn spring-boot:run
# 后端监听 http://localhost:8088
# Flyway 会在业务 Bean 初始化前自动创建或升级 Schema
# API 文档 http://localhost:8088/doc.html
```

### 5️⃣ 启动前端

```bash
cd ../frontend
npm install
npm run dev
# 前端监听 http://localhost:3000，自动代理 /api → :8088
```

### 6️⃣ 访问系统

打开浏览器访问 **http://localhost:3000** 🎉

---

## 🐳 Docker 部署

### 一键启动

```bash
# 1. 复制配置模板
cp docker-compose.example.yaml docker-compose.yaml
# 创建 .env 文件并填写凭据（参考下方环境变量说明）

# 2. 构建并启动所有服务
docker compose up -d --build

# 3. 查看服务状态
docker compose ps
```

### 服务列表

| 服务 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| MySQL 8.0 | `mysql` | 3306 | 主数据库，自动初始化 Schema |
| Redis 7 | `redis` | 6379 | 会话存储 & 缓存 |
| Spring Boot | `backend` | 8088 | 后端 API 服务 |
| Vue Frontend | `frontend` | — | 由 Nginx 代理 |
| Nginx | `nginx` | 80 / 443 | 反向代理 & 静态文件 |

### 健康检查

Docker Compose 配置了 MySQL、Redis 的健康检查，后端服务会在这两个依赖健康后启动。正式对象存储是外部 Cloudflare R2，Compose 不启动本地 MinIO。

---

## 📁 项目结构

```
Picture-Managentor/
├── backend/                          # Spring Boot 后端
│   ├── pom.xml                       # Maven 依赖配置
│   ├── Dockerfile                    # 后端容器构建
│   └── src/main/
│       ├── java/com/picmgmt/
│       │   ├── controller/           # REST 控制器
│       │   │   ├── UserController    #   用户、认证、OAuth、头像
│       │   │   ├── ImageController   #   图片 CRUD、点赞、下载
│       │   │   ├── CategoryController#   分类管理
│       │   │   └── CommentController #   评论系统
│       │   ├── service/              # 服务接口
│       │   │   └── impl/            # 服务实现（业务逻辑）
│       │   ├── mapper/               # MyBatis-Plus Mapper
│       │   ├── entity/               # 数据库实体
│       │   ├── dto/                  # 请求数据传输对象
│       │   ├── vo/                   # 响应视图对象
│       │   └── config/               # 配置类 (Sa-Token, CORS, R2...)
│       └── resources/
│           ├── application.example.yml  # 配置模板（不含敏感信息）
│           ├── db/schema.sql         # Flyway 当前 Schema 基线
│           └── mapper/               # MyBatis XML 映射
│
├── frontend/                         # Vue 3 前端
│   ├── package.json                  # npm 依赖
│   ├── vite.config.js                # Vite 构建配置
│   ├── Dockerfile                    # 前端容器构建 (多阶段)
│   ├── index.html                    # 入口 HTML
│   └── src/
│       ├── views/                    # 页面组件
│       │   ├── Login.vue             #   登录页
│       │   ├── Register.vue          #   注册页
│       │   ├── Home.vue              #   图片管理主页
│       │   ├── ImageSquare.vue       #   图片广场
│       │   ├── ImageDetail.vue       #   图片详情 & 评论
│       │   └── Profile.vue           #   个人主页
│       ├── components/               # 通用组件
│       │   ├── NavBar.vue            #   导航栏
│       │   ├── ImageCard.vue         #   图片卡片
│       │   ├── ImageUpload.vue       #   上传组件
│       │   ├── ImageViewer.vue       #   图片查看器
│       │   ├── TurnstileWidget.vue   #   人机验证组件
│       │   ├── NotificationBell.vue  #   通知铃铛
│       │   ├── NotificationDrawer.vue#   通知抽屉
│       │   ├── TagInput.vue          #   标签输入
│       │   ├── ParticleBackground.vue#   粒子背景
│       │   └── ParticleSettings.vue  #   粒子设置
│       ├── api/                      # API 请求模块
│       ├── store/                    # Pinia 状态管理
│       ├── router/                   # Vue Router 路由
│       ├── composables/              # 组合式函数
│       ├── particle/                 # 粒子引擎
│       ├── utils/                    # 工具函数
│       └── __tests__/                # 单元测试 (13 个测试文件)
│
├── deploy/                           # 部署配置
│   └── nginx/
│       ├── default.conf              # Nginx 服务器配置
│       └── cloudflare-ips.conf       # Cloudflare IP 白名单
│
├── docker/                           # Docker 初始化脚本与旧数据导入文件
│
├── docs/                             # 项目文档
│   └── Introduction.txt              # 需求规格说明书
│
├── docker-compose.example.yaml       # Docker Compose 模板
└── .gitignore                        # Git 忽略规则
```

---

## 📡 API 文档

API 文档由 SpringDoc + Knife4j 自动生成：

- **本地开发**：http://localhost:8088/doc.html
- **生产环境**：https://image-space.app/doc.html

### 响应格式

所有接口统一返回 `Result<T>` 格式：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 主要接口

<details>
<summary><strong>👤 用户模块 <code>/user</code></strong></summary>

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| `POST` | `/user/login` | 账号密码登录 | ✗ |
| `POST` | `/user/register` | 用户注册 | ✗ |
| `GET` | `/user/logout` | 退出登录 | ✓ |
| `GET` | `/user/info` | 获取当前用户信息 | ✓ |
| `PUT` | `/user/profile` | 更新个人资料 | ✓ |
| `POST` | `/user/avatar` | 上传头像 | ✓ |
| `POST` | `/user/background` | 上传背景图 | ✓ |
| `GET` | `/user/avatar/{id}` | 获取用户头像 | ✗ |
| `GET` | `/user/background/{id}` | 获取用户背景图 | ✗ |
| `GET` | `/user/profile/{id}` | 获取用户主页 | ✗ |
| `POST` | `/user/send-code` | 发送邮箱验证码 | ✗ |
| `POST` | `/user/forgot-password` | 重置密码 | ✗ |
| `GET` | `/user/oauth/{provider}/url` | 获取 OAuth 授权链接 | ✗ |
| `GET` | `/user/oauth/{provider}/callback` | OAuth 回调 | ✗ |
| `GET` | `/user/admin/list` | [管理员] 用户列表 | ✓ |
| `PUT` | `/user/admin/{id}/role` | [管理员] 修改用户角色 | ✓ |
| `DELETE` | `/user/admin/{id}` | [管理员] 删除用户 | ✓ |

</details>

<details>
<summary><strong>🖼 图片模块 <code>/image</code></strong></summary>

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| `POST` | `/image/upload` | 上传图片 | ✓ |
| `GET` | `/image/list` | 获取我的图片列表 | ✓ |
| `PUT` | `/image/{id}` | 编辑图片信息 | ✓ |
| `DELETE` | `/image/{id}` | 删除图片 | ✓ |
| `GET` | `/image/{id}/detail` | 获取图片详情 | ✗ |
| `GET` | `/image/download/{id}` | 下载图片 | ✗ |
| `GET` | `/image/square` | 图片广场 | ✗ |
| `POST` | `/image/{id}/like` | 点赞图片 | ✓ |
| `DELETE` | `/image/{id}/like` | 取消点赞 | ✓ |

</details>

<details>
<summary><strong>📂 分类模块 <code>/category</code></strong></summary>

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| `POST` | `/category` | 创建分类 | ✓ |
| `GET` | `/category/list` | 获取分类列表 | ✓ |
| `PUT` | `/category/{id}` | 修改分类 | ✓ |
| `DELETE` | `/category/{id}` | 删除分类 | ✓ |

</details>

<details>
<summary><strong>💬 评论模块 <code>/comment</code></strong></summary>

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| `POST` | `/comment` | 发表评论 | ✓ |
| `GET` | `/comment/image/{imageId}` | 获取图片评论 | ✗ |
| `DELETE` | `/comment/{id}` | 删除评论 | ✓ |

</details>

---

## 🗺 路由与页面

| 路由 | 页面 | 认证 | 说明 |
|------|------|:----:|------|
| `/login` | 登录页 | ✗ | 已登录自动跳转 `/home` |
| `/register` | 注册页 | ✗ | 已登录自动跳转 `/home` |
| `/home` | 图片管理 | ✓ | 我的图片 CRUD |
| `/square` | 图片广场 | ✗ | 公开图片画廊 |
| `/image/:id` | 图片详情 | ✗ | 图片查看 & 评论 |
| `/profile/:id` | 个人主页 | ✗ | 用户资料 & 作品 |
| `/` | — | — | 重定向至 `/home` |

所有路由使用 **懒加载** (`() => import(...)`)，路由守卫检查 `localStorage['satoken']`。

---

## 🔐 权限系统

### RBAC 模型

```
users → user_roles → roles
                       │
                       ▼
              role_permissions → permissions
```

### 角色

| 角色 | role_id | 说明 |
|------|---------|------|
| admin | 1 | 管理员 — 拥有所有权限 |
| moderator | 2 | 版主 — 部分管理权限 |
| user | 3 | 普通用户 — 基础操作权限 |

### 权限码

| 权限码 | 说明 |
|--------|------|
| `image:upload` | 上传图片 |
| `image:edit` | 编辑自己的图片 |
| `image:delete` | 删除自己的图片 |
| `image:edit:any` | 编辑任意图片 |
| `image:delete:any` | 删除任意图片 |
| `category:manage` | 管理自己的分类 |
| `category:manage:any` | 管理任意分类 |
| `comment:add` | 发表评论 |
| `comment:delete` | 删除自己的评论 |
| `comment:delete:any` | 删除任意评论 |
| `user:manage` | 用户管理 |

---

## ⚙️ 环境变量

创建 `.env` 文件配置以下变量（参考 `docker-compose.example.yaml`）：

### 数据库

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `MYSQL_ROOT_PASSWORD` | — | MySQL root 密码 |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | — | 数据库密码 |

### 对象存储

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `STORAGE_TYPE` | `r2` | 正式环境对象存储类型 |
| `R2_ENDPOINT` | — | R2 S3 API endpoint |
| `R2_ACCESS_KEY` | — | R2 access key |
| `R2_SECRET_KEY` | — | R2 secret key |
| `R2_BUCKET_NAME` | — | 唯一正式对象存储 bucket |
| `R2_PUBLIC_URL` | `https://cdn.image-space.app` | Worker/CDN 公开域名 |

### OAuth

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `GITHUB_CLIENT_ID` | — | GitHub OAuth Client ID |
| `GITHUB_CLIENT_SECRET` | — | GitHub OAuth Client Secret |
| `GITHUB_REDIRECT_URI` | — | GitHub OAuth 回调 URL |
| `GOOGLE_CLIENT_ID` | — | Google OAuth Client ID |
| `GOOGLE_CLIENT_SECRET` | — | Google OAuth Client Secret |
| `GOOGLE_REDIRECT_URI` | — | Google OAuth 回调 URL |

### 邮件

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `MAIL_HOST` | `smtp.qq.com` | SMTP 服务器 |
| `MAIL_PORT` | `587` | SMTP 端口 |
| `MAIL_USERNAME` | — | 邮箱账号 |
| `MAIL_PASSWORD` | — | SMTP 授权码 |

### 人机验证

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `TURNSTILE_ENABLED` | `false` | 启用 Cloudflare Turnstile |
| `TURNSTILE_SECRET_KEY` | — | Turnstile 服务端密钥 |
| `VITE_TURNSTILE_ENABLED` | `false` | 前端启用 Turnstile |
| `VITE_TURNSTILE_SITE_KEY` | `0x4AAA...` | Turnstile 站点密钥 |

### 应用

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `APP_PUBLIC_BASE_URL` | `https://image-space.app` | 公开访问基础 URL |
| `APP_FRONTEND_BASE_URL` | `https://image-space.app` | 前端基础 URL |
| `APP_ADMIN_ALLOW_LOCAL` | `false` | 后端是否显式允许本地管理员 Host |
| `VITE_ADMIN_ALLOW_LOCAL` | `false` | 前端是否显式允许本地管理员入口 |
| `IMAGE_REVISION` | `unknown` | 写入 backend 镜像 OCI revision 标签 |

> ⚠️ **注意**：修改 `.env` 后需要 `docker compose up -d` 重建容器，`docker compose restart` **不会**重新加载 `.env` 文件。

---

## 🧪 测试

### 后端

```bash
cd backend
mvn test
```

### 前端

```bash
cd frontend
npm test           # 运行所有测试
npm run test:watch  # 监听模式
```

前端包含 **13 个测试文件**，覆盖组件、API 模块、路由、工具函数等。

---

## 🤝 贡献指南

1. **Fork** 本仓库
2. 创建特性分支：`git checkout -b feature/amazing-feature`
3. 提交代码：`git commit -m 'Add amazing feature'`
4. 推送分支：`git push origin feature/amazing-feature`
5. 提交 **Pull Request**

### 开发规范

- 后端遵循 Spring Boot 分层架构：`Controller → Service → Mapper`
- 前端使用 Vue 3 Composition API + `<script setup>` 语法
- 提交信息使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范
- 所有 API 统一返回 `Result<T>` 格式

---

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源协议。

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/ShirokoGranblue">ShirokoGranblue</a>
</p>
