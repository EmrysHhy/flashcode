# Flashcode 前端

Flashcode 门户。用户在这里登录、描述想要的应用、查看需求文档、预览生成结果，并可以改页面、改源码、发布到案例广场。

生成出的用户应用也以 Vue 3 为主，门户和预览用同一套组件模型。

## 页面

| 路径 | 作用 |
| --- | --- |
| `/home` | 首页对话。发送提示词，接收需求文档。 |
| `/app/:id` | 应用详情。左侧预览，右侧继续修改；支持元素选择和 code-server 高级编辑。 |
| `/cases` | 案例广场。查看已发布应用。 |
| `/about` | 项目说明。技术选型和难点与解法。 |
| `/profile` | 个人资料。 |

![首页对话](src/content/about/images/home-chat.png)

![应用预览](src/content/about/images/app-preview.png)

![元素编辑](src/content/about/images/element-edit.png)

![案例广场](src/content/about/images/case-square.png)

## 技术组件

| 组件 | 用途 |
| --- | --- |
| Vue 3 | 门户框架。 |
| Vite | 开发服务器和构建。 |
| Vue Router | 页面路由，使用 History 模式。 |
| Pinia | 登录用户等状态。 |
| Element Plus | 登录、列表和表单。组件按需自动引入。 |
| Sass | 样式。 |
| marked | 把模型输出和关于页的 Markdown 渲染成 HTML。 |

接口统一走 `src/utils/request.js`。对话、应用、用户分别放在 `src/apis/`。

## 本地启动

需要 Node.js `^20.19.0` 或 `>=22.12.0`。

```bash
npm install
npm run dev
```

开发服务器监听 `0.0.0.0:80`。`/portal` 代理到网关，超时 10 分钟，避免生成请求被提前断开。`/preview` 代理到预览 Nginx。代理地址在 `vite.config.js` 里，按实际环境修改。

```bash
npm run build
npm run preview
```

## 目录

```text
src/
  apis/            用户、需求、应用接口
  components/      页头、页脚、对话、预览、案例、关于页
  content/about/   技术选型与难点说明，图片在 images/
  router/          路由
  stores/          Pinia
  views/           页面
```
