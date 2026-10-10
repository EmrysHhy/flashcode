# Flashcode 后端

Flashcode 的服务端。负责登录、需求对话、代码生成、构建预览、截图、源码托管和文件存储。

门户、网关、文件服务、搜图服务各自打成可执行包，注册到 Nacos。模型名、图搜开关、Gitee 令牌放在 Nacos，改配置不用重新打包。

## 模块

| 模块 | 作用 |
| --- | --- |
| `gateway` | 统一入口。校验 JWT，再按 Nacos 实例转发。 |
| `portal` | 用户、需求文档、应用生成和预览。`portal-api` 提供 Feign 接口，`portal-service` 是实现。 |
| `file` | 上传截图、配图和头像到阿里云 OSS。 |
| `image-mcp` | 单独提供 `search_images`。搜图失败不打断代码生成。 |
| `common` | 安全、Redis、缓存、消息、领域对象和通用工具。 |

生成链路在 `portal-service` 的 `MultiAgentWorkFlow`：生成代码 → 按类型构建预览 → 失败则修错 → Selenium 截图 → 推送 Gitee。生成、截图、提交在一次流程里各最多执行 2 次。

## 技术组件

运行环境是 Java 21、Spring Boot 3.3.3。

| 分类 | 技术/组件 | 简介 |
| --- | --- | --- |
| AI相关 | Spring AI Alibaba | 接入通义千问。对话、向量和 Graph 工作流共用这一套，模型名配在 Nacos 的 `spring.ai.dashscope.chat.options.model`。 |
| AI相关 | 会话记忆 | 多轮上下文放在 Redis。生成出的整份源码不写入记忆。 |
| AI相关 | 多模态 | 按模型名打开 `multiModel`。名字里带 `-vl` 或 `qwen3.8` 时走视觉接口，提示词里只放 `IMG_n`。 |
| AI相关 | RAG | 生成和改需求时用 `QuestionAnswerAdvisor` 查 Milvus。 |
| AI相关 | MCP | `image-mcp` 提供搜图，门户只决定搜什么、用哪几张。 |
| AI相关 | Multi-Agent | `StateGraph` 串起生成、构建预览、修错、截图和提交。 |
| AI相关 | Milvus | 向量库，旁边跑 etcd 和 MinIO。向量由通义 embedding 写入。 |
| 后端 | Spring Cloud | Gateway 做入口，OpenFeign 调文件服务，LoadBalancer 按 Nacos 实例转发。 |
| 后端 | Redis | 对话记忆和登录验证码。 |
| 后端 | Nacos | 注册中心和配置中心，独立模式，配置数据在 MySQL。 |
| 后端 | MySQL | 用户、应用、需求文档和聊天记录。短描述和应用全文分开存放。 |
| 后端 | MyBatis-Plus | 门户持久层。 |
| 后端 | JWT | 登录令牌。网关和门户用同一套校验，登录详情在 Redis。 |
| 后端 | Nginx | 预览容器托管用户应用。`/{appId}/api` 按应用动态写入并热重载。 |
| 后端 | 阿里云 OSS | 截图、配图和头像，公开读。 |
| 后端 | Selenium | 容器内 Chrome 打开预览地址截首页。驱动随镜像准备。 |
| 运维 | Docker / Compose | 中间件和应用容器化。Compose 拉起 MySQL、Nacos、Redis、Milvus、预览 Nginx、Prometheus、Grafana。 |
| 运维 | Prometheus / Grafana | 抓取门户 Actuator。生成耗时、token 用量和上下文占用率进监控面板。 |
| 三方对接 | Gitee | 只推文本源码到 `flash-user-code/{appId}/`。 |
| 三方对接 | 阿里云短信 / 邮件 | 登录验证码。发送次数和验证码缓存在 Redis。 |

密钥、数据库密码和第三方令牌只放在 Nacos 或环境变量里，不写入仓库。

## 构建与部署

在 `flashcode-be` 目录：

```bash
mvn clean package -DskipTests
```

开发环境中间件：

```bash
docker compose -p flashcode -f deploy/dev/app/docker-compose-mid.yml up -d
```

门户通过 docker-java 在预览容器里执行用户工程的 `npm run build`，并按 appId 分配端口启动后端 jar。源码目录、预览目录和发布目录分开挂载。

## 生成约定

模型只生成 `Html`、`Vue3`、`Spring_Vue3`。输出第一行是应用类型，随后每个文件按 `FILE: 相对路径` 加完整代码块给出。`AnalysisUtil` 按这个协议落盘，并拒绝越出应用目录的路径。

- Html：复制 `index.html`。
- Vue3：`npm install` 后 `npm run build`。
- Spring_Vue3：先构建前端，再 `mvn clean package -DskipTests`。对外前缀是 `/{appId}/api`，Controller 仍映射 `/api`。
