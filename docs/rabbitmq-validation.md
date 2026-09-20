# RabbitMQ 接入验收记录

验收日期：2026-09-19。范围为本地代码、Docker 镜像和隔离容器；未连接生产、未提交或推送 Git。

## 实施结果

- 原图同步保存，图片记录与派生图事件同事务提交；中图和缩略图异步生成，前端下次查询自然使用派生地址。
- 图片任务、删除及编辑共享行锁，派生字段更新不覆盖业务字段；对象补偿和提交后的缓存刷新使用持久化任务。
- 审计保留 AOP 采集和脱敏，独立事务保存待发送事件；消费按事件 ID 去重，提交后发送风险 SSE。
- 发送确认、无法路由退回、持久化重试、死信及按事件 ID 重放已接入。
- RabbitMQ 固定官方版本与摘要，配置持久卷、健康检查、专用 vhost 和回环管理端口；镜像构建排除本地私有配置。
- 新增 V5 迁移；新建图片填充空的遗留 `image_path`，兼容空库中该列的非空约束。

## 检查结果

| 检查 | 结果 |
|---|---|
| `mvn clean verify -Pmessaging-it` | 后端单测 175 项通过，真实消息集成测试 10 项通过，无失败、错误或跳过 |
| `npm.cmd run check` | lint、类型检查、270 项前端测试、生产构建通过 |
| Worker `npm.cmd test` | 10 项通过 |
| 主 Compose + override、示例 Compose、隔离 Compose | `config --quiet` 全部通过；主/示例验证时仅临时注入测试 RabbitMQ 密码，不改私有配置 |
| Docker 后端镜像 | 从源码构建成功；运行 JAR 只包含 example/docker 配置，不包含本地 `application.yml` |
| Flyway | 空库 V1–V5、V4 升级且保留历史审计、同库第二次启动均通过 |
| Git | `git diff --check` 通过；已有修改保留，未执行提交/推送 |

构建过程中出现的 Node 子进程 `EPERM` 在获准的本地执行环境中重跑通过；Docker Hub 一次临时 EOF 重试后通过。前端构建存在依赖库的 pure annotation 提示，未阻止构建。Worker 测试输出有测试响应按文本读取的提示，10 项断言全部通过。

## 故障与浏览器证据

- RabbitMQ 停止期间上传成功，原图仍可下载，图片和审计待发送记录保留；恢复后自动补齐。
- 停机期间删除的图片没有被后台任务恢复；改为私有的图片保留当前权限。
- RabbitMQ 与 backend 重启后数据仍可用，没有重复执行迁移。
- 重复事件只写一条审计；发送无路由保持待发送；有界重试次数不会因消费者重启重置。
- 派生对象写入失败时数据库不暴露未完成 key，清理任务可恢复；图片行锁阻止并发编辑越过处理中状态；缓存刷新失败可独立重试。
- 运维脚本真实重放事件 `a12b6a87-5c43-4106-ab45-a8e2a333c8d2`：进入死信后修复载荷，重放后 `DONE`，对应审计行数为 1。
- 浏览器通过真实上传组件提交样本：先加载 `original.png`（naturalWidth=2400），恢复队列并刷新后加载 `thumb.jpg`（naturalWidth=400）。
- 审计页面显示实时通道已连接，实际收到 `IMAGE_DELETE / FAILED` SSE，验收审计 ID 为 23。浏览器两条预期 HTTP 错误来自故意删除不存在的测试图片。

机器可读结果与截图位于被 Git 忽略的 `frontend/output/messaging/`：

- `messaging-acceptance.json`
- `browser-original-fallback.png`
- `browser-variants-ready.png`
- `browser-audit-sse.png`
- `backend-final.log`、`frontend-check.log`、`worker-test.log`、`docker-final-build.log`

## 本地耗时测量

样本为同一张 2400×1600 PNG，11,525,178 字节，使用本地存储适配器。同步与异步各预热一次，再测三次。后台完成耗时从请求开始计时，并以轮询确认数据库已有两个派生 key，因此包含检测间隔。

| 指标 | 三次测量（秒） | 中位数（秒） |
|---|---|---:|
| 同步上传响应 | 0.830 / 0.778 / 0.698 | 0.778 |
| 异步上传响应 | 0.399 / 0.384 / 0.399 | 0.399 |
| 异步派生图完成 | 1.942 / 1.896 / 1.974 | 1.942 |

RabbitMQ 停止时另一次上传响应为 0.379 秒。这些是本机单样本延迟测量，不是生产吞吐量、R2 网络耗时或 Cloudflare 命中率结论。

## 部署边界与文件索引

本次未验证真实 R2、Cloudflare 边缘缓存及生产 nginx。浏览器媒体链路使用隔离存储卷和本地适配器，但元数据与私有授权调用真实后端端点。单节点 RabbitMQ 不提供磁盘丢失情况下的集群高可用。

运行与回滚说明见 [rabbitmq.md](rabbitmq.md)。主要变更文件：

| 部分 | 文件 |
|---|---|
| 图片 | `ImageWriteService.java`、`AsyncImageService.java`、`ImageMapper.java` |
| 审计 | `AuditAspect.java`、`AuditLog.java` |
| 消息 | `messaging/MessageEvent.java`、`OutboxService.java`、`OutboxPublisher.java`、`RabbitConfiguration.java`、`MessageProcessor.java`、`MessageFailureService.java`、`MessageConsumers.java` |
| 迁移 | `V5__AddMessageOutbox.java` |
| 存储补偿 | `StorageService.java`、`LocalStorageService.java`、`MinioStorageService.java` |
| 构建与配置 | `backend/pom.xml`、`backend/.dockerignore`、两个 application 模板、`.env.example`、主/示例 Compose、`docker-compose.messaging-test.yaml` |
| 测试 | `ImageWriteServiceTest.java`、`AuditAspectTest.java`、`messaging/MessageConsumersTest.java`、`messaging/MessagingIT.java` |
| 验收与运维 | `backend/scripts/messaging-smoke.py`、`messaging-media-fixture.py`、`replay-message.ps1`、本记录及运行说明 |

原先已有的图片/评论控制器和评论服务修改均未覆盖；`StorageService.java` 在保留原有改动的基础上增加可靠补偿删除方法。
