# 预览模块问题记录

记录 flashcode 应用生成 → 打包 → Nginx 预览这条链路里踩过的坑。环境：portal 容器部署在 `192.168.56.107`，经网关 `18080` 调 `POST /portal/flashcode/app/generate`，预览走 `http://192.168.56.107/preview/{appId}/#/`。

生成是**同步**的：模型输出 → 解析落盘 → `npm` / `mvn` → 拷到 `user-preview` → 预览容器里起 jar → 改 nginx。整段经常要 **5～15 分钟**。

---

## 一、已经解决

### 1. 解析模型输出抽空、走错打包分支

**现象：** Vue 预览白屏，`index.html` 0 字节；或第一行写成 `VUE3` 却按 Html 拷文件。

**原因：**

- `enableThinking(true)` 时输出带 `<think>...</think>`，旧正则解析不到 `FILE:`。
- 整段正则遇到空代码块、反引号不换行会写出空文件。
- 只认第一行 `APP_TYPE=`，不看实际文件。

**处理：** `AnalysisUtil` 先剥 think，再按行解析 `FILE:` / \`\`\`；用文件后缀判类型（单 html → Html，`.vue`+`.java` → Spring_Vue3，仅 `.vue` → Vue3），`APP_TYPE=` 只作兜底。`AppServiceImpl` 先 `getFiles` 再 `resolveType`。

### 2. 提示词 API 前缀写错

**现象：** 前端请求打不到预览 Nginx 的 `/{appId}/api/`。

**原因：** 系统提示写成 `` `/api` + appId + `/api` ``，例子却是 `/api/users`。课上规则是 URL 前缀 `/{appId}/api`（例如 `/10000003/api/users`）。Controller 仍是 `@RequestMapping("/api/...")`，不加 appId。

**处理：** `getSysPrompt` 已改成 `/{appId}/api`。

### 3. 同一 appId 叠了多个启动类

**现象：** `mvn` 报 `Unable to find a single main class`。

**原因：** `saveCode` 只覆盖同名文件，三次生成留下三个 `*Application.java`。

**处理：** 写入前删除整个 `user-code/{appId}`。同一 `appId` 仍须等上一次返回后再点。

### 4. 打包失败只回「命令执行失败: npm install」

**现象：** Apifox 看不到三个 main、半包 Tomcat 等真因。

**处理：** `CommandUtil.runCommand` 非 0 退出时，`ServiceException` 带工作目录、退出码和约 2000 字截断后的 stdout。

### 5. Docker TLS 证书路径

**现象：** 启动失败：`DOCKER_CERT_PATH '/workspace/cert' doesn't exist`。

**原因：** Nacos 里 `docker.cert-path: /workspace/cert` 是**容器内**路径。IDEA 本机没有该目录。容器则靠 pom 挂载：

`/root/emrys-java/flashcode/deploy/dev/app/config/cert → /workspace/cert`

**处理：**

- Nacos **不要改**成 Windows 路径。
- `DockerClientConfig`：配置路径不存在时，回退到仓库 `deploy/dev/app/config/cert`（方便 IDEA 启动）。
- 真正出预览必须跑 **容器里的 portal**，本机写入的文件 Nginx 读不到。

### 6. nginx 脚本 CRLF（10000003 最后一公里）

**现象：** jar、前端 dist 都成功，接口仍失败：`update_nginx_location.sh: $'\r': command not found`。预览页能开，`/{appId}/api/` 404。

**原因：** Windows 检出的 `.sh` 是 CRLF，预览容器是 Linux bash。

**处理：**

- 虚拟机上已 `sed` 掉 `\r`，并为 `10000003` 补了 location。
- `CommandUtil.updateNginxConfig` 执行脚本前先 `sed -i 's/\r$//'`。
- 仓库 `.gitattributes`：`*.sh` / `*.bash` 固定 LF。

`10000004` 已完整走过 nginx 更新，说明这条已通。

### 7. 其它已处理

| 问题 | 处理 |
|---|---|
| `DockerClient` 依赖 httpclient5 启动崩溃 | 改为 `ZerodepDockerHttpClient` |
| 根 `pom.xml` 重复声明 `mybatis-plus-spring-boot3-starter` | 删掉重复项 |
| `git merge` 报 `index.lock` | 无 git 进程后可删锁；当时还被 `target/classes/bootstrap.yml` 脏文件挡住，restore 后已把 `check_bug` 快进合进 `master` |

### 8. 已跑通的预览实例

| appId | 应用 | 结果 |
|---|---|---|
| 10000003 | 教学管理系统 | 生成成功；曾卡在 nginx CRLF，脚本修好后页面和 `/10000003/api/students` 可用 |
| 10000004 | 笔记本电脑管理系统 | 全链路成功（含 nginx），预览 `http://192.168.56.107/preview/10000004/#/` |

---

## 二、需要解决（配置 / 环境 / 体验）

代码对齐当前章节后，下面这些仍会让人觉得「没返回、服务繁忙、很慢」。

### 1. 网关 / Apifox 超时（优先）

**现象：** 生成其实在跑或已经成功，Apifox 仍空白或 `500000 服务繁忙`。

**原因：**

- 截图里的 **「控制台」** 只显示前置/后置脚本日志，没写脚本就是「没有内容」。看 **「返回响应」**。
- Nacos `bite-gateway-dev.yaml` 里 **只有 file 配了** `response-timeout: 300000`，**portal 没有**。生成经常超过默认等待时间，连接被掐，portal 仍继续跑。

**建议：** 在 Nacos 给 portal 路由加上：

```yaml
        - id: bite-portal
          uri: lb://bite-portal
          predicates:
            - Path=/portal/**
          filters:
            - StripPrefix=1
          metadata:
            response-timeout: 900000
            connect-timeout: 30000
```

Apifox 超时调到 10 分钟以上（或 0）。同一 `appId` 不要连点。

### 2. Maven 官方源 + 容器 `.m2` 不持久

**现象：** 第一次 `mvn package` 很慢，曾出现 `tomcat-embed-core` 半包下载失败。

**原因：** 容器内 Maven 走 `repo.maven.apache.org`，无阿里云镜像；`.m2` 在容器层，每次重建门户几乎冷启动。

**建议：** 给 portal 配国内 `settings.xml`，并把宿主机 `.m2/repository` 挂进容器。属环境，未改业务代码。

### 3. 同一 appId 并发生成

**现象：** 两个 `npm install` 同时解包 `node_modules`，`ETXTBSY` / `ENOENT`。

**原因：** 超时后重试或连点，没有按 `appId` 互斥。清空目录不能防两次请求一起清、一起写。

**建议：** 按 `appId` 加锁，第二次等待或直接拒绝。代码里尚未做。

### 4. 打包堵在 Tomcat 工作线程

**现象：** 生成期间 Hikari `Thread starvation`、Nacos gRPC `UNHEALTHY`，偶发网关 `Unable to find instance for bite-portal`。

**原因：** `npm` / `mvn` 跑在处理 HTTP 的同一线程上，心跳发不出去。实例可能被摘掉，短时间 `500000`。

**建议：** 异步生成，或把构建丢到单独线程池，避免占满 `http-nio` 线程。当前章节未做。

### 5. Portal 冷启动慢、Nacos 找不到实例

**现象：** `mvn deploy` 后立刻调接口 → `503 Unable to find instance for bite-portal` → `500000 服务繁忙`。

**原因：** 门户启动约 **6 分钟**（例如 `Started ... in 376s`）后才注册。注册 IP 是 Docker 网桥 `172.17.0.5:18083`，容器**未映射** 18083。

**建议：** 等日志出现 `Started BitePortalServiceApplication` 再调。可选：加入 `flashcode_network`、映射 `18083`，或 `-Dspring.cloud.nacos.discovery.ip=192.168.56.107`。

### 6. 不要用 IDEA 本机 portal 做出预览

本机 `user-code` / `user-preview` 在 Windows 工程目录；Nginx 读的是虚拟机：

`/root/emrys-java/flashcode/deploy/dev/data/flashcodedata/flashcode-app/user-preview`

两边不是同一块盘，预览一定空或旧。生成请 `mvn deploy` 门户服务。

---

## 三、操作备忘

```text
# 只部署 portal（Windows 需用 IDEA 自带 mvn 全路径）
mvn clean deploy -pl bite-portal/bite-portal-service -am -DskipTests

# 确认门户已启动
docker logs flashcode-bite-portal-service-1 2>&1 | grep -E "Started BitePortal|APPLICATION FAILED"

# 确认证书和预览目录已挂载
docker inspect flashcode-bite-portal-service-1 --format "{{json .Mounts}}"
```

Nacos（保持即可，不要改成本机路径）：

```yaml
docker:
  host: tcp://192.168.56.107:2376
  cert-path: /workspace/cert
```

---

## 四、对照课上代码：刻意没做的

当前阶段只对齐「一次生成、一次打包、失败就抛」，没有引入后续章节：

- ErrorFixAgent（构建失败再让模型修）
- ChatMemory / 向量 RAG
- 改 Docker / 预览容器整体结构

老师那边失败还能再修一轮；这边失败会直接把异常（现在含命令输出）丢回接口。







现在这台虚拟机上 Milvus 已经挂了：flashcode-milvus-standalone 是 Exited (80)，etcd / minio 还活着。门户 flashcode-bite-portal-service-1 也跟着 Exited (1) 了，就是你看到的 DEADLINE_EXCEEDED。

“总是炸”其实是两件事叠在一起。 容器自己会崩；门户启动时又把 milvusClient 当硬依赖，连不上就整进程退出。所以你感觉每次都是 Milvus 把整个系统拖死。

这一次的真正死因
最后一次启动只活了大约 2 分钟（10:52 起、10:54 退），日志里反复是：

streaming node is not alive
no available streaming node
streamingnode 的 etcd lease 过期：the session is expired without activing closing
Milvus v3.0.1 standalone 内部自带 streaming node。这个子进程一死，整个 standalone 就以 exit 80 退出。这不是门户代码写错，也不是这次 OOM（OOMKilled=false）。

更麻烦的是它已经 重启了 135 次。最后 Docker 自己也卡死了：

Error=failed to create task for container: AlreadyExists: task ... already exists
containerd 里残留了一份旧 task，restart: always 再也拉不起来，所以它已经死了几个小时，19530 现在根本没人在听。门户再连 192.168.56.107:19530，DNS 很快（几毫秒），gRPC 握手空等 60 秒，然后 Spring 上下文失败。

为什么你会觉得“每次都炸”
同一套环境里叠了好几层，表现都像“Milvus 挂了”：

现象	原因
exit 80 + streaming node（当前）
v3.0.1 standalone 的 streaming 组件不稳，lease 一丢就整容器退出
exit 80 + 端口映射
docker-compose.yml 仍是裸写 19530:19530，会顺带绑 [::]:19530。VirtualBox 上 docker-proxy 很容易卡死，下次起不来也是 80。docker-compose-mid.yml 已经改成只绑 0.0.0.0
exit 134
数据目录权限，后来加了 user: root
门户 DEADLINE_EXCEEDED
Spring AI 启动期就建 milvusClient。Milvus 还在 health: starting、已经退出、或 19530 没映射，门户都会直接起不来
启动顺序
门户不在 compose 里 depends_on Milvus healthy；冷启动 Milvus 经常要 2～3 分钟
当前端口绑定已经是 IPv4 only，所以这一次不是 IPv6 proxy，是 3.x 的 streaming node 崩了，再被 Docker 的残留 task 锁死。

和门户的关系
VectorConfig 注入了 MilvusVectorStore，自动配置会在启动时创建 milvusClient。连不上就：

milvusClient → vectorStore → 整棵 Spring 树失败 → 门户退出。

Nacos 里 host 配 192.168.56.107:19530 没问题（DNS 很快），超时说明 对端没在听，不是地址写错。