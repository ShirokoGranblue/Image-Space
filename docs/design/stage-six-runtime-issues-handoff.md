# Stage Six Runtime Issues Handoff

> 阶段六运行问题分析、总结和后续线程交接说明。
>
> 最后实测时间：2026-07-14（Asia/Shanghai）
> 仓库：C:\Users\l2653\Desktop\image-space
> 环境：Windows + PowerShell 7 + Docker Desktop
> 范围：后端、中间件、前端页面、路由跳转和只读业务流程验证

## 0. 2026-07-14 实施与上线结果

本文件列出的仓库内 P1/P2 问题已经完成实现、测试和生产部署；Cloudflare Worker 凭据、secret、部署和 CDN public 链路也已于 2026-07-14 完成闭环。仍未验收的项目只剩需要真实外部账号的邮件/OAuth/评论点赞流程，以及两项已单独记录的前端问题。

已完成：

- 头像/背景读取现在优先对象存储；对象缺失时只回退到严格校验的 `data:image/*;base64,...`，GET 不上传对象、不更新数据库；启动迁移会修复“key 存在但对象缺失”的记录；
- 使用 Flyway 管理正式版本迁移，已有库以版本 `0` baseline，随后执行 V1/V2；删除原有启动时 schema initializer 和散落 SQL；
- `/categories` 采用“分类能力合并到首页”的唯一入口，旧地址显示明确 404；
- 管理员域名校验前后端一致，非管理域名不再初始化审计请求，403 后会停止 SSE/轮询并只重定向一次；本地放行必须显式启用开发开关；
- 正式对象存储明确为 R2；没有删除遗留 MinIO 容器或卷；
- backend Dockerfile 使用 BuildKit Maven 缓存、三次有界重试和 OCI revision label；干净 Docker 构建已成功；
- Turnstile 前后端默认值已对齐为关闭；生产补充密钥后已同时启用 `TURNSTILE_ENABLED` 与 `VITE_TURNSTILE_ENABLED`，compose 显式传入站点 key，backend 容器与实际前端 bundle 均确认启用；
- 生产 HTTPS/nginx 已恢复并稳定运行，未改成 HTTP-only 配置。

生产发布证据：

- 发布前数据库备份：`/home/azureuser/backups/picture_management-pre-stage6-20260714T055835Z.sql.gz`；SHA-256 为 `c48c8cf9833e93bc7ba820223a6aa45ad6bf49e88b517e4cbf4ea6ab24520ee1`；
- backend 镜像：`sha256:db618203d0d4a246080fd76bdd78cf66b3e2999614348a3cfe999afa0043c620`，revision `stage6-20260714-2132fb52`；
- Flyway history：baseline `0`、V1 `CurrentSchema`、V2 `AlignLegacySchema`，三项 `success=1`；第二次启动报告 schema 已是最新；
- backend、frontend、mysql、redis、nginx 均为 Up，mysql/redis 为 healthy，`nginx -t` 成功；
- nginx 容器内 `index.html` 与宿主机 `frontend/dist/index.html` SHA-256 一致；
- 本地 backend `mvn clean test`：129 个测试通过；生产 frontend：37 个测试文件、209 个测试通过，生产构建成功；R2 Worker：10 个测试通过；
- 生产 `r2-proxy/node_modules` 曾残留 `@cloudflare/vitest-pool-workers 0.12.21`、Vitest 3.2.4 和 Wrangler 4.95，与当前锁文件不一致；执行 `npm ci` 后已对齐为 0.16.20、Vitest 4.1.9、Wrangler 4.107，Worker 10 个测试重新通过；
- `r2-proxy` 的 `auth:check`、`secret:backend`、`deploy` npm scripts 会通过 Node `--env-file-if-exists=../.env` 加载仓库根凭据；`npm run auth:check` 已在服务器成功，避免 raw Wrangler 再回退到无 GUI OAuth；
- `BACKEND_INTERNAL_TOKEN` 已成功写入 Worker secret，当前 Worker 已部署到 `cdn.image-space.app/*`，版本 ID 为 `388bdd80-6e99-416d-bf4d-c857f948a257`；
- Worker 协议验收通过：public GET/HEAD 均为 `200 image/png` 且带 `Cache-Control: public, max-age=31536000, immutable`，过期 public 版本为 404，旧根路径为 404，private 无凭据为 403，后三者均为 `no-store`；
- 真实浏览器访问 `/square` 返回 5 张公开图片，全部 `naturalWidth > 0`、损坏图片数为 0，控制台 0 错误/0 警告；此前 public 502/ORB 已不再复现；
- Turnstile 生产探针确认前端编译开关为 true、编译站点 key 与服务器配置一致、backend 容器开关和 secret 均有效；无 Turnstile token 的登录请求返回业务码 `2018`；
- 真实浏览器确认登录页已渲染 Cloudflare Turnstile“请验证您是真人”控件；未完成挑战时 token 为空，前端提交处理会阻止请求，未自动操作或绕过人机验证；
- 临时专用账号通过真实线上 UI 完成上传、公开转私有、私有 Worker 读取、无凭据拒绝、删除和退出；删除后 API 详情为 404、私有 URL 失效；账号、角色、图片、评论、通知、审计等测试数据已全部清零；
- 线上旧头像场景实测：对象 key 缺失且旧 Base64 有效时返回 `HTTP 200 image/png`，GET 前后数据库 key 不变。

尚未闭环：

- 邮件验证码、OAuth、评论/点赞未使用真实外部账号执行。生产配置存在，但本轮没有专用测试邮箱和已授权的第三方账号，不能把配置存在扩大为验收通过；
- 端到端删除后观察到详情抽屉仍发出三次 404 轮询请求，720px 高度下固定分页条会遮挡编辑按钮。这两项不是本交接文件原列问题，本轮未扩大范围修改，已记录为后续独立前端修复项。

## 1. 交接摘要

> 本节保留 2026-07-13 的原始运行基线，供追溯使用；当前生产结论以第 0 节为准。

当前项目的核心后端链路已经恢复，但尚未达到正式上线验收通过状态。

已经验证通过：

- backend、mysql、redis 由 compose 管理，并处于同一 Docker 网络；
- backend 持续运行并监听 8088；
- Redis 密码、数据库连接、当前源码字段和本地浏览器 CORS 已修复；
- 密码登录、登录后跳转 /home、退出登录、验证码接口通过；
- /home、/square、/profile/{uuid}、/image/{uuid}、/403 可以渲染；
- 首页筛选、通知抽屉、上传弹窗、登录方式切换等低副作用交互通过；
- backend 完整测试 119 个全部通过。

仍需继续处理：

1. 头像和背景对象下载失败时没有旧 Base64 回退，接口返回 500；
2. 数据库 schema 演进依赖人工执行 SQL，缺少可重复的版本化迁移；
3. /categories 文档路由与实际前端路由不一致；
4. 管理员审计页的 Host 防护只拦截 API，页面仍然渲染并重复弹错；
5. MinIO 遗留容器与当前 R2 存储配置容易造成部署误解；
6. Docker 内 Maven 构建受外部下载中断影响，当前运行镜像使用了临时的当前源码 JAR 构建路径；
7. 上传、编辑、删除、邮件、OAuth、私有媒体等高副作用流程尚未完成端到端验收。

按照用户要求，以下两个问题延期到上线后测试，本交接范围内不要重复修改：

- nginx 缺少 SSL origin 证书导致的 HTTPS 入口问题；
- R2/Worker/CDN public 图片链路导致的浏览器 ORB 和图片加载失败问题。

## 2. 不要重复处理的已完成事项

### 2.1 正式 compose 网络

backend-local 是此前通过 docker compose run 创建的临时测试容器，不是正式服务。不要再围绕 backend-local 修改 compose。

当前正式服务：

    backend
    mysql
    redis
    frontend
    nginx

当前网络：

    picture_management_picture_management

最后一次确认的容器地址：

    backend  172.21.0.6
    mysql    172.21.0.3
    redis    172.21.0.5
    frontend 172.21.0.2

状态基线：

| 服务 | 最后状态 | 说明 |
|---|---|---|
| backend | Up | 0.0.0.0:8088->8088/tcp |
| mysql | Up / healthy | 使用已有数据卷 |
| redis | Up / healthy | 使用已有数据卷 |
| frontend | Up | 容器内部 80 端口，本地验证使用 Vite 3000 |
| minio | Up / healthy | 旧 compose 遗留容器，不是当前正式 compose 服务 |
| nginx | Restarting | 证书问题按用户要求延期处理 |

backend 宿主机端口已经写入：

    docker-compose.override.yaml

    services:
      backend:
        ports:
          - "8088:8088"

### 2.2 本地 CORS

浏览器实际 Origin：

    http://127.0.0.1:3000

原配置只允许 localhost:3000，导致浏览器登录 403。当前已修改：

    backend/src/main/java/com/picmgmt/config/WebMvcConfig.java

新增允许：

    http://127.0.0.1:3000
    http://127.0.0.1:5173

验证：

    POST /api/user/login => HTTP 200 / code 200
    GET  /user/captcha   => HTTP 200 / code 200

不要再次把本问题误判为 backend 与 Redis 不在同一网络。

### 2.3 当前本地数据库字段已经补齐

旧数据卷缺少当前源码字段，已经在现有数据卷中补齐。

users：

- uuid
- email_verified

images：

- uuid
- original_key
- original_filename
- original_content_type
- original_ext
- original_size
- width
- height
- medium_key
- thumb_key
- media_version

本次没有删除表、删除行或重建数据。下一线程执行任何 ALTER TABLE 前必须先检查字段是否已经存在。

注意：这次是针对当前本地数据卷的人工运行时迁移，仓库中还没有形成正式、可重复的 migration 文件。

### 2.4 当前 backend 镜像构建背景

backend/Dockerfile 仍然是多阶段构建：

    maven build stage
        -> mvn -DskipTests package
    runtime JRE stage

本次 docker compose build backend 连续两次因 Maven Central 下载依赖时响应截断失败，错误类似：

    Premature end of Content-Length delimited message body

宿主机执行以下命令成功：

    mvn "-DskipTests" package

为了完成容器级验证，临时使用当前源码 JAR 构造了同名运行镜像，再由 compose 创建正式 backend 容器。

因此：

- 当前运行容器使用的是当前源码编译结果；
- Dockerfile 没有被退回为复制旧 target 的方案；
- Dockerfile 在当前网络条件下还没有完成一次完整的容器内 Maven 构建；
- 下一线程不要声称 Docker 构建已通过，除非重新执行并看到完整 BUILD SUCCESS。

## 3. 按用户要求延期到上线后处理

### 3.1 nginx SSL origin 证书

现象：

    nginx => Restarting (1)

缺失路径：

    /etc/nginx/ssl/origin_cert.pem

影响：

- nginx 不能稳定提供 80/443；
- HTTPS 入口未验收；
- 本地 Vite 3000 直连 backend 8088 不受影响。

上线后验证：

    docker compose ps --all
    docker compose logs --tail=200 nginx
    curl.exe -I https://image-space.app
    curl.exe -I https://admin.image-space.app

验收标准：

- nginx 为 Up，不再重启；
- HTTPS 返回预期状态码；
- 主站和管理员域名命中正确 server block；
- 不替换为 HTTP-only 配置。

### 3.2 R2/Worker/CDN public 图片链路

浏览器实测请求：

    https://cdn.image-space.app/public/...

结果：

    net::ERR_BLOCKED_BY_ORB

影响页面：

- /home
- /square
- /profile/{uuid}
- /image/{uuid}

图片列表 API 正常：

    GET /api/image/list => 200

因此问题不在列表查询，而在浏览器拉取图片的 R2/Worker/CDN 链路。

上线后检查真实响应，不要只检查 HTML 状态码：

    curl.exe -I "https://cdn.image-space.app/public/images/<storage-key>?v=<media-version>"

验收重点：

- public 图片返回 Content-Type: image/*；
- public 图片返回 Cache-Control: public, max-age=31536000, immutable；
- 不返回 HTML 错误页或 JSON 错误体；
- Worker 能正确读取 Redis media meta；
- R2 对象存在且 storage key 与数据库一致；
- private/SPECIFIED 图片不会被 public URL 直接访问；
- public/private 切换后的旧 URL 不能绕过权限。

## 4. 当前需要继续处理的问题

### 4.1 P1：头像和背景对象失败时返回 500

实测：

    GET /api/user/avatar/{uuid}     => 500
    GET /api/user/background/{uuid} => 500

数据库用户同时存在：

- avatar_key / background_key；
- 旧 avatar / background Base64 数据。

启动日志：

    迁移完成: 图片 0 张, 头像 0 个, 背景 0 个, 评论 0 条

当前迁移逻辑发现已有 key 后跳过迁移。用户媒体接口随后直接使用 storage key 下载，下载失败后没有旧 Base64 回退。

涉及文件：

- backend/src/main/java/com/picmgmt/controller/UserController.java
- backend/src/main/java/com/picmgmt/config/StorageMigrationRunner.java
- backend/src/test/java/com/picmgmt/controller/UserControllerTest.java

建议方案：

1. 优先使用对象存储；
2. storage key 下载失败时，仅当旧值是合法 data:*;base64,... 时回退到 Base64；
3. 不要把任意外部 URL 当作 Base64 回退；
4. Base64 解码失败时返回受控的 404/业务错误，不要变成无上下文的 500；
5. GET 请求不要隐式修改数据库或上传对象；
6. 增加一次性数据修复流程，检测 key 对象不存在但 Base64 存在的记录；
7. 修复迁移逻辑，使 key 存在但对象不存在的记录不会永久被视为已迁移。

建议测试：

- storage 对象存在时返回 storage 内容；
- storage 对象不存在、旧 Base64 存在时返回 Base64 解码内容；
- storage 对象和 Base64 都不存在时返回受控错误；
- Base64 MIME 类型正确映射到响应 Content-Type；
- GET 不调用 upload 或 update。

验收：

- 旧用户头像和背景不再出现 500；
- 有效 Base64 可以显示；
- 存储对象存在时仍优先使用对象存储；
- 新上传头像/背景流程不回归；
- 单元测试和个人页截图中不再出现头像/背景 500。

### 4.2 P1：数据库 schema 演进不可重复

旧数据卷启动当前 backend 时出现：

    Unknown column 'uuid' in 'field list'

必须人工补齐字段后 backend 才能启动。

风险：

- 新环境可以通过 schema.sql 启动，旧环境却可能失败；
- docker compose up -d 不会自动执行已有数据卷的初始化 SQL；
- 版本升级没有明确 schema 版本；
- 下一次新增字段会重复出现启动失败。

建议方案：

1. 先检查仓库是否已有 Flyway、Liquibase 或其他 migration 机制；
2. 如果已有机制，沿用现有机制，不要增加第二套；
3. 如果没有，建立按版本命名的 SQL migration；
4. 对字段增加幂等检查；
5. 迁移前做备份或至少输出数据卷与数据库版本；
6. 将本次人工变更整理为正式 migration；
7. 在 CI 中使用旧 schema fixture 测试升级路径；
8. 启动日志明确输出 schema 版本和成功/失败状态。

验收：

- 旧 schema 加旧数据可以自动升级；
- 全新数据库可以直接初始化；
- migration 重复执行不报重复字段；
- 不删除用户、图片、评论和分类；
- backend 日志包含 schema 版本。

### 4.3 P2：/categories 路由和实际前端不一致

访问：

    http://127.0.0.1:3000/categories

实际只显示跳转占位内容，没有独立分类管理页。

分类功能当前出现在：

- 首页筛选栏；
- 首页分类统计；
- 上传图片弹窗中的分类选择和新建分类。

需要产品/计划选择一个方向，不要两种同时做：

方案 A：保留独立 /categories 页面

- 增加真实路由；
- 复用现有分类 API 和组件；
- 支持分类列表、新建、删除和图片归类；
- 更新路由测试和截图。

方案 B：取消独立页面

- 确认分类功能已经合并到首页；
- 删除设计文档、README、路由表中失效的 /categories；
- 补充首页分类入口说明。

验收：

- 文档路由表与 router/index.js 一致；
- 不存在的路由有明确 404/回退页面，而不是空壳；
- 分类管理入口唯一且可发现。

### 4.4 P1：管理员审计页 Host 拦截后的前端体验不完整

未登录访问：

    /admin/audit-log -> /login

登录后使用本地域名访问：

    http://127.0.0.1:3000/admin/audit-log

页面本体仍然渲染，但审计 API 被 Host 防护拦截，页面重复弹出：

    当前域名无权访问后台管理系统

页面同时显示：

    实时连接不可用，已启用轮询

根因判断：

- Host 防护主要在后端/API 层生效；
- 前端入口没有同步阻止；
- 页面进入后继续启动 SSE/轮询；
- 于是形成重复错误提示和无效请求。

建议方案：

1. 确认后端 AdminDomainInterceptor 的允许域名和本地开发策略；
2. 前端进入审计页前执行与后端一致的 host 判断；
3. 非管理域名直接导航到 /403，或只显示一次阻断状态；
4. API 返回 Host 403 后停止 SSE 和轮询；
5. 使用一次性错误状态替代连续 toast；
6. 保留生产 admin.image-space.app 的访问路径；
7. 本地调试使用明确开发开关，不要永久放宽生产 Host。

验收：

- 非管理域名不会初始化审计请求；
- 非管理域名只出现一次 403/跳转结果；
- SSE 失败不会无限重启轮询；
- admin.image-space.app 可以正常查询；
- 非管理员仍受到权限码和 Host 双重限制。

### 4.5 P2：MinIO 遗留容器与 R2 配置混淆

当前 backend 环境使用：

    STORAGE_TYPE=r2
    R2_ENDPOINT=...
    R2_BUCKET_NAME=image-space

同时 Docker 中仍有旧 minio 容器，并且加入 compose 网络。当前 backend 的 MinioStorageService 实际通过 MinIO SDK 访问 R2，不代表 backend 使用本地 MinIO。

风险：

- 维护人员误以为图片应该在本地 MinIO；
- 容器列表出现看似必需、实际未使用的服务；
- 清理旧 MinIO 时可能误删数据；
- 本地开发和线上存储方案边界不清楚。

建议方案：

1. 用 docker inspect、compose 文件和环境变量确认 MinIO 是否还有消费者；
2. 不要直接删除 MinIO 容器或卷；
3. 如果只用于旧环境，拆分 development compose 和 production compose；
4. 如果正式环境只用 R2，更新 README、AGENTS 和部署文档；
5. 为容器增加明确 label；
6. 确认 R2 对象可访问后，再在明确授权下清理旧 MinIO。

验收：

- 文档明确唯一正式对象存储；
- compose 不再无说明地启动遗留存储服务；
- 清理旧容器前确认数据卷和对象安全；
- 新上传、下载、删除均指向同一明确存储后端。

### 4.6 P1：Docker 构建不可稳定复现

证据：

- docker compose build backend 连续失败于 Maven Central 依赖下载；
- 宿主机 Maven 使用缓存后可以成功构建；
- 当前验证使用了临时 JAR runtime image。

风险：

- 新机器或服务器可能无法构建 backend；
- 依赖下载失败时无法仅靠 docker compose up -d --build 恢复；
- 当前临时构建方式不能作为长期部署规范；
- 镜像没有明确版本号或 digest 交接信息。

建议方案：

1. 优先在 CI 中构建 backend 镜像；
2. 使用稳定 Maven mirror、缓存或内部 Nexus/Artifactory；
3. 使用 Git SHA/版本号 tag，不要只依赖 latest；
4. 生产 compose 直接拉取已验证镜像；
5. 保留当前多阶段 Dockerfile，不要改成复制可能过期的 target JAR；
6. 部署前检查镜像 digest 和启动日志；
7. 分开记录构建失败和运行失败。

验收：

- 干净环境 docker compose build backend 成功；
- 镜像包含当前 Git 提交对应源码；
- 镜像 tag 可追溯；
- 服务器部署不依赖宿主机预先存在 JAR；
- 构建失败时能明确区分网络/依赖错误。

### 4.7 P2：高副作用业务流程尚未完成端到端验收

本次没有执行以下操作，以避免真实数据和外部服务副作用：

- 真实图片上传；
- 图片编辑；
- 图片删除；
- 分类创建和删除；
- 真实用户注册；
- 发送邮箱验证码；
- 邮箱验证码登录；
- OAuth 登录；
- 私有/SPECIFIED 图片访问；
- 图片下载；
- 评论发布和评论图片上传；
- 点赞；
- R2 对象上传、删除和权限切换。

下一线程需要使用专用测试数据、测试邮箱和可回滚对象存储完成这些流程。不能把“只读页面通过”扩大解释为“完整业务通过”。

## 5. 推荐处理顺序

下一线程建议按以下顺序：

1. 读取本交接文档并确认 git status，不要重置现有工作区；
2. 修复头像/背景下载回退，并增加单元测试；
3. 将本次人工数据库变更整理为正式、幂等、可版本化 migration；
4. 决定 /categories 是补页面还是修正文档；
5. 修复管理员页面的前端 Host 阻断、SSE/轮询停止和重复提示；
6. 明确 R2 与 MinIO 的环境边界并补充部署文档；
7. 解决 Docker 构建的 Maven mirror/cache/镜像版本追踪问题；
8. 使用专用测试数据执行上传、编辑、删除、私有访问、评论和邮件流程；
9. 最后按用户要求在上线后处理 nginx SSL 和 CDN/R2 public 图片链路；
10. 汇总截图、API 响应、容器日志和测试结果。

## 6. 下一线程启动检查清单

先执行只读命令：

    Set-Location C:\Users\l2653\Desktop\image-space
    git status --short
    docker compose ps --all
    docker network ls
    docker network inspect picture_management_picture_management
    docker compose logs --tail=200 backend
    docker compose logs --tail=200 nginx

不要输出 .env 内容、密码、token、R2 secret 或 SMTP 凭据。

后端验证：

    Set-Location C:\Users\l2653\Desktop\image-space\backend
    mvn test

前端验证：

    Set-Location C:\Users\l2653\Desktop\image-space\frontend
    npm.cmd run build

代码检查：

    Set-Location C:\Users\l2653\Desktop\image-space
    git diff --check

浏览器截图目录：

    C:\Users\l2653\Desktop\image-space\frontend\output\playwright\live-regression\

关键截图：

- login-current-1440.png
- login-email-1440.png
- login-390.png
- register-current-1440.png
- home-1440.png
- home-notifications-1440.png
- home-private-filter-1440.png
- upload-dialog-1440.png
- square-1440.png
- profile-1440.png
- image-detail-1440.png
- 403-1440.png
- admin-audit-log-1440.png
- categories-unmatched-1440.png

## 7. 工作区和变更注意事项

- 工作区在本次任务开始前已经存在阶段一到阶段五相关改动；
- 不要执行 git reset --hard 或 git checkout --；
- 已有直接修改：
  - docker-compose.override.yaml：增加 backend 8088 端口；
  - backend/src/main/java/com/picmgmt/config/WebMvcConfig.java：增加本地 loopback Origin；
- 本次新增文件：
  - docs/design/stage-six-runtime-issues-handoff.md；
- 当前数据库字段补齐是在本地 MySQL 数据卷中执行的运行时操作，不等同于已经提交正式 migration 文件；
- 没有提交 Git commit；
- 没有执行真实注册、发送邮件、上传、编辑或删除业务数据；
- 不要把截图中图片加载失败直接归因于 Docker 网络，API 列表和页面路由正常，媒体对象访问链路需要单独排查。

## 8. 交接完成定义

下一线程完成以下条件后，才可以把阶段六剩余问题标记为完成：

- 头像/背景对象缺失有兼容回退或完成数据修复；
- 旧数据库可以通过正式 migration 自动升级；
- /categories 的产品决策已落地，文档和路由一致；
- 非管理域名访问审计页不会重复弹错或启动无效轮询；
- R2 与 MinIO 的职责和部署配置已经明确；
- Docker backend 镜像可以从干净环境稳定构建或从可追溯 registry 镜像部署；
- 上传、编辑、删除、私有访问、评论、邮箱等高副作用流程有专用测试证据；
- nginx SSL 和 CDN/R2 public 链路在上线后完成实测并单独记录结果。
