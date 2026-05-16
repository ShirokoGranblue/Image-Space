# CLAUDE.md

为后续在此仓库工作的 Claude Code (claude.ai/code) 提供开发指引。

## 常用命令

```bash
# 后端
cd backend
mvn spring-boot:run                          # 启动后端，监听 :8080
mvn test                                      # 运行全部测试

# 前端
cd frontend
npm install                                   # 安装依赖
npm run dev                                   # 启动开发服务器，监听 :3000
npm run build                                 # 生产构建

# 数据库 — 首次启动前，手动执行 schema.sql 初始化 MySQL
# 数据库名: picture_management，默认账号密码 root/root，见 application.yml
```

API 文档自动生成，访问 `http://localhost:8080/doc.html`（SpringDoc + Knife4j UI）。

## 架构

**后端** (`com.picmgmt`) — 分层架构，Spring Boot 3.2 + JDK 21：

```
controller → service/impl → mapper (MyBatis-Plus BaseMapper)
     ↓            ↓
   dto/vo      entity (@TableName 映射至 snake_case 表名)
```

- **认证**: Sa-Token（非 Spring Security）。Token 存储在 `localStorage['satoken']`，以 `satoken` 请求头传递。`SaTokenConfig` 拦截所有路由，仅放行 `/user/login`、`/user/register`、`/doc.html/**`、`/v3/api-docs/**`、`/swagger-ui/**`、`/upload/**`。
- **密码加密**: 通过 Hutool 实现 BCrypt (`BCrypt.hashpw` / `BCrypt.checkpw`)，不使用 Spring Security 的编码器。
- **文件存储**: 存储路径由 `app.upload-path` 指定（默认 `./upload`）。路径格式: `/upload/{年}/{月}/{uuid}.{扩展名}`。`WebMvcConfig` 将 `/upload/**` URL 映射至文件系统目录以提供访问。
- **跨域**: `WebMvcConfig` 中配置允许所有来源。前端开发环境使用 Vite 代理 (`/api` → `:8080`)，因此跨域配置仅在前后端合并部署时生效。

**前端** — Vue 3 + Element Plus + Pinia + Vue Router：

| 路由 | 页面 | 需登录 |
|-------|------|:---:|
| `/login` | 登录 | 否 |
| `/register` | 注册 | 否 |
| `/home` | 我的图片（增删改查） | 是 |
| `/square` | 图片广场 | 否 |
| `/categories` | 分类管理 | 是 |

路由守卫位于 `router/index.js`，通过 `localStorage['satoken']` 判断 `meta.requiresAuth` 路由。

## 关键设计决策

### 图片分页为内存分页
`ImageServiceImpl.page()` 通过 `ImageMapper.selectImageVOList()` 查询全部匹配行，再在 Java 中切分。查询使用动态 `<if>` 标签和 `ORDER BY ${sortField}`。排序字段在拼接前经过**白名单校验**（`upload_time`、`image_name`、`file_size`），防止 SQL 注入。考虑到本地使用场景（10 人以内并发），此方案完全可行。

### 服务层做归属校验
`ImageServiceImpl.delete()` 和 `update()` 在变更前均校验 `image.userId == loginId || hasRole("admin")`。`CategoryServiceImpl` 遵循相同模式。

### 删除分类后图片归入"未分类"
分类被删除时，`CategoryServiceImpl.delete()` 将下属所有图片的 `category_id` 置为 `NULL`，而非删除图片本身。

### 自定义 SQL 仅存在于 ImageMapper
`UserMapper` 和 `CategoryMapper` 直接使用 MyBatis-Plus `BaseMapper` 提供的方法。`ImageMapper.selectImageVOList` 是唯一的自定义查询——通过 LEFT JOIN users 和 categories 表，一次查询构建出 `ImageVO`。

### API 响应格式
所有接口统一返回 `Result<T>`，结构为 `{ code: 200, message: "success", data: ... }`。`api/index.js` 中的 axios 拦截器负责解包——Vue 组件收到的 `res.data` 即为 `Result` 对象。非 200 的 code 会触发 `ElMessage.error`。

#注意事项
每句话后面加上一句“喵~”。
‘类似于“你好喵~，我喜欢你喵~”’