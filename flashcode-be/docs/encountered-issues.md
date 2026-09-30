# 开发踩坑总览

环境：门户部署在 `192.168.56.107`，经网关 `18080` 调接口。中间件用 `deploy/dev/app/docker-compose-mid.yml`（项目名 `flashcode`）。

更早的「生成 → npm/mvn → Nginx 预览」细节见 [preview-module-issues.md](./preview-module-issues.md)。本文汇总后续多智能体、Milvus、DashScope、构建修复里遇到的问题。

---

## 一、Milvus 总是挂，门户跟着起不来

### 1. 门户 `DEADLINE_EXCEEDED` / `ApplicationContext` 失败

**现象：** portal 启动约 60 秒后退出，日志：

```text
DEADLINE_EXCEEDED ... waiting_for_connection
remote_addr=192.168.56.107:19530
```

**原因：** Spring AI 启动期就创建 `milvusClient`。`VectorConfig` 注入 `MilvusVectorStore`，连不上则整棵 Spring 树失败。DNS 很快，超时说明 **19530 没人在听**（Milvus 未就绪、已退出、或端口映射丢了）。

**处理：** 等 `flashcode-milvus-standalone` 为 `healthy` 再启 portal。Nacos 可把 `connect-timeout-ms` 拉到 60000。当前未做成「连不上也能启动」。

### 2. 容器 exit 80：`streaming node is not alive`

**现象：** `flashcode-milvus-standalone` 反复退出，etcd / minio 仍 healthy。日志：

```text
streaming node is not alive
no available streaming node
the session is expired without activing closing
```

**原因：** `milvusdb/milvus:v3.0.1` standalone 自带 streaming node，etcd lease 一丢整容器 exit 80。在 8G VirtualBox 上曾重启上百次，最后 containerd 报 `AlreadyExists: task ... already exists`，`restart: always` 也拉不起来。

**处理：** 镜像改为 **`milvusdb/milvus:v2.6.4`**，不要设 `MQ_TYPE=woodpecker`。3.x 与 2.6 元数据不兼容，必须清空 `milvus/data`、`milvus/etcd`（建议连 minio 一起清）再 `up`。

### 3. 清数据后 `database not found[database=flashcode_db]`

**现象：** Milvus 已 healthy，门户仍起不来。

**原因：** Nacos 配了 `database-name: flashcode_db`。清空 etcd 后库没了，SDK 初始化即失败。

**处理：** Proxy ready 后创建库：

```text
from pymilvus import MilvusClient
c = MilvusClient(uri="http://flashcode-milvus-standalone:19530")
c.create_database("flashcode_db")
```

冷启动 healthz 通过不等于 Proxy 已可写，过早创建会报 `Milvus Proxy is not ready yet`。

### 4. 其它历史上的 exit 码

| 现象 | 原因 | 处理 |
|---|---|---|
| exit 134 | 数据目录权限 | `user: root` |
| exit 80 + 端口映射 | 裸写 `19530:19530` 会绑 `[::]`，VirtualBox 上 `docker-proxy` 卡死 | 只绑 `0.0.0.0:19530` / `9091` |
| minio 假 unhealthy | `mc ready local` 冷启动超过 healthcheck timeout | `start_period: 60s`、`timeout: 40s` |
| 拉镜像极慢 / compose 卡住 | 多个 `compose pull/up` 并行 | 先 `pkill` 再单独 `docker pull` |

两份 compose 必须对齐：`docker-compose-mid.yml`（虚拟机实际用）和 `docker-compose.yml`。只改一份会把 v3 / IPv6 又带回来。

**建议顺序：** etcd、minio、milvus `healthy` → 确认 `flashcode_db` → 再 deploy portal。不要并行多个 compose 拉镜像。

---

## 二、DashScope / ChatClient

### 5. `This model only supports incremental_output set to True`

**现象：** `POST /flashcode/agent/app/generate` 业务异常，HTTP 400。

**原因：** qwen3（含 thinking）只接受流式。代码用 `.call()`，SDK 把 `incremental_output` 设成 `false`。

**处理：**

- `DashScopeChatOptions` 加 `incrementalOutput(true)`（默认 ChatClient 与 VL 选项都要）
- 用 `ChatContentSupport.collect(spec)`：`spec.stream().content()` 拼全文，不要 `.call().content()`

改完必须重新部署 portal。

### 6. 参考图切 VL 模型

**现象：** 上传参考图后仍走 coder 模型，或图片被 `saveCode` 清掉。

**原因：** `saveCode` 会先删整个 `user-code/{appId}`。参考图不能放在代码目录。

**处理：** 图写到 `user-reference/{appId}/`。有图时请求级 `.options(vlOptions)` + `user.media`，模型用 `qwen3-vl-plus`。`CommitNode` 延迟删代码时一并删参考图。路径必须在 `user-reference` 下，见 `VisionChatSupport.resolveImage`。

---

## 三、多智能体生成 / 修复

### 7. ErrorFix：`NumberFormatException: For input string: "default"`

**现象：** 生成已成功，`npm run build` 失败后修复节点崩：

```text
RedisChatMemoryConfig.get
MessageChatMemoryAdvisor.adviseStream
```

**原因：** 生成节点传了 `CONVERSATION_ID = appId`，修复节点没传。Spring AI 默认 `"default"`，`Long.parseLong` 炸。流式调用必走 memory advisor。

**处理：** `ErrorFixAgent` 同样设置 `ChatMemory.CONVERSATION_ID`。`RedisChatMemoryConfig` 对 `default` / 非数字 ID 当空记忆，不再 parseLong。

### 8. `vite.config.js`：`ReferenceError: path is not defined`

**现象：** `npm install` 成功，`npm run build` 退出码 1。

**原因：** 模型写了 `path.resolve` 却没 `import`/`require`。ErrorFix 本该修，但当时被第 7 条挡住。

**处理：** 生成 / 修复提示词写明：用 `path` 必须先引入。修复通路打通后可把该错误原文丢给模型。

### 9. 解析抽空、类型走错、叠多个启动类

详见 [preview-module-issues.md](./preview-module-issues.md) 第 1～3 条：剥 `<think>`、按文件后缀判类型、`saveCode` 先清空目录。

### 10. 打包失败只回「命令执行失败」

`CommandUtil` 失败时带上工作目录、退出码和截断后的 stdout，否则修代码看不到 `path is not defined`。

### 11. 提示词 API 前缀

前端必须 `/{appId}/api/...`，后端 Controller 仍是 `/api/...`。写反会导致预览里接口 404。详见预览文档第 2 条。

### 12. 错误 URL：`No static resource flashcode/agent/flashcode`

请求路径写错。多智能体生成是：

```text
POST /flashcode/agent/app/generate
```

经网关一般为 `/portal/flashcode/agent/app/generate`。

---

## 四、Maven / 两份工程

IDEA 有时编的是课上副本 `D:\code\JAVA\usual_test_java\spring-ai\flashcode`，不是 Cursor 工作区 `d:\code\JAVA\project\flashcode`。两份 POM 不一致时，改工作区解决不了 IDEA 报错。

### 13. mybatis-plus 重复声明

```text
dependencyManagement ... must be unique: mybatis-plus-spring-boot3-starter
```

根 POM 里同一依赖写了两遍。删一份即可。工作区根 POM 本来就只有一份。

### 14. `Child module bite-mstemplate does not exist`

课上根 POM 的 `<modules>` 列了 `bite-mstemplate`，目录不存在。Maven 扫描直接失败。从 `<modules>` 去掉即可。磁盘上实际模块：admin / common / file / gateway / gitee-mcp-server / portal。

---

## 五、部署与本机

### 15. Docker TLS 证书路径

Nacos `docker.cert-path: /workspace/cert` 是**容器内**路径。IDEA 本机没有该目录，本机起 portal 会失败。容器靠 pom 挂载。详见预览文档第 5 条。

### 16. Nginx 脚本 CRLF

Windows 检出的 `update_nginx_location.sh` 带 `\r`，bash 把 `\r` 当命令。执行前 `sed -i 's/\r$//'`。详见预览文档。

### 17. 生成耗时长

同步链路：模型 → 解析 → npm/mvn → 预览。常要 5～15 分钟。读超时已在 `ChatClientConfig` 拉到 10 分钟。同一 `appId` 须等上次返回再点。

---

## 六、排查顺序（门户起不来或生成失败）

1. `docker ps -a`：milvus standalone 是否 `healthy`，portal 是否 `Up`
2. `ss -lntp | grep 19530`：端口是否在听
3. portal 日志：`DEADLINE_EXCEEDED` / `database not found` → Milvus；`incremental_output` → 流式选项；`"default"` → 会话 ID
4. `npm run build`：看 `CommandUtil` 打出的 stdout，再决定改提示词还是等 ErrorFix
5. 确认 IDEA 打开的是哪一份 `flashcode` 工程

常用命令：

```bash
docker logs --tail 300 flashcode-bite-portal-service-1
docker inspect -f '{{.State.Health.Status}}' flashcode-milvus-standalone
```



