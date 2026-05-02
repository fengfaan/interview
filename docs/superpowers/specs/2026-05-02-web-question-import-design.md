# 网页抓题导入功能设计

## 概述

用户浏览网页面试题（掘金、CSDN、LeetCode 面经等）时，通过输入 URL 自动抓取或手动粘贴内容，AI 解析为结构化面试题，选择性导入 Obsidian 知识库。

## 用户流程

页面有两个输入模式，通过 Tab 切换：

### 模式一：URL 抓取

1. 用户输入 URL → 点击「抓取」
2. 后端 Playwright 打开页面，提取文章文本，返回标题+内容
3. 前端显示抓取到的文本预览（可折叠）
4. 用户选择方向和级别 → 点击「解析面试题」
5. AI 解析文本为结构化题目列表，前端展示卡片
6. 用户可编辑/删除题目，勾选想导入的 → 点击「导入到知识库」
7. 逐条保存，显示进度

### 模式二：手动粘贴

1. 用户直接在文本框粘贴网页内容
2. 后续流程同 URL 模式的步骤 4-7

## 后端架构

### 新增组件

| 组件 | 职责 |
|---|---|
| `BrowserCaptureService` | Playwright 浏览器实例管理（懒初始化单例），提供 `capture(url)` 方法 |
| `ImportController` | REST API 端点 |
| `import-parse.md` | 新 prompt 模板 |

### 核心流程

**抓取流程：**
- Playwright 打开页面（headless Chromium）
- 等待 networkidle，最多 30s 超时
- 按 CSS 选择器优先级提取文本：`article` > `main` > `.post-body` > `body`
- 截断到 4000 字符
- 返回 CaptureResult(title, content, url, capturedAt)

**解析流程：**
- 渲染 `import-parse.md` prompt（注入 direction、level、content）
- `aiGateway.generateText()` 一次性返回 JSON
- 解析为 `List<BatchQuestionItem>`
- 返回给前端

**导入流程：**
- 复用 `ObsidianService.saveNote()` 逐条保存
- frontmatter 标记 `source: "web-import"` 和原始 URL

### Playwright 生命周期

- 应用启动时不初始化
- 首次调用 `capture()` 时懒加载 Playwright 实例
- 首次使用时自动执行 `playwright install chromium`
- 应用关闭时通过 `@PreDestroy` 关闭浏览器释放资源

## API 设计

### POST /api/import/capture

输入 URL，返回抓取的页面文本。

```
请求: { "url": "https://..." }
响应: { "success": true, "data": { "title": "文章标题", "content": "...", "url": "https://...", "capturedAt": "2026-05-02T..." } }
错误: URL 无效 / 网页超时 / 网页不可达
```

### POST /api/import/parse

输入文本+方向+级别，AI 解析为面试题。

```
请求: { "content": "...", "direction": "GO_BACKEND", "level": "BASIC" }
响应: { "success": true, "data": { "items": [{ "question": "...", "answer": "...", "keywords": [...] }] } }
```

### POST /api/import/save

批量保存选中题目到知识库。

```
请求: { "items": [...], "direction": "GO_BACKEND", "level": "BASIC", "sourceUrl": "https://..." }
响应: { "success": true, "data": { "results": [{ "title": "题目", "success": true }] } }
```

## Prompt 模板

`backend/prompts/import/import-parse.md` 指导 AI：
- 从网页文本中识别所有面试题
- 每道题提取题目（q）、参考答案（a）、关键词（k）
- 如果文本不包含面试题，返回空数组
- 输出紧凑 JSON 格式 `{"items":[{"q":"...","a":"...","k":[...]}]}`

## 前端 UI

### 新增文件

- `ImportView.vue` — 网页抓题页面
- `importStore.ts` — Pinia store（URL、抓取状态、解析结果、编辑状态、导入进度）
- `importApi.ts` — API 调用封装
- 类型定义扩展

### 页面布局

**上半部分：输入区域**
- Tab 切换：「URL 抓取」|「粘贴文本」
- URL 模式：输入框 + 抓取按钮 + 抓取文本预览（可折叠）
- 粘贴模式：大文本框
- 方向和级别选择器（复用快速刷题的按钮组样式）

**下半部分：解析结果**
- 题目卡片列表：题目文本、答案预览（截断）、关键词标签
- 每张卡片：勾选框、编辑按钮、删除按钮
- 底部操作栏：「已选 N 题」+「导入到知识库」按钮

### 导航

侧边栏新增「网页抓题」入口，图标 `cloud_download`，路由 `/import`。

## 知识库保存适配

- `ObsidianService.saveNote()` 新增可选 `url` 字段，写入 frontmatter
- 批量导入时逐条保存并汇报每条结果，不因单条失败中断整个批次
- frontmatter 自动标记 `source: "web-import"`

## 依赖

- `com.microsoft.playwright:playwright`（Maven 依赖，~5MB JAR + ~200MB Chromium 二进制首次下载）

## 风险与缓解

| 风险 | 缓解 |
|---|---|
| Playwright 安装体积大 | 首次使用时自动安装，README 说明环境要求 |
| 网站反爬检测 | Playwright 自带反检测；个人本地使用频率低；手动粘贴作为兜底 |
| AI 解析准确度 | 用户可编辑解析结果后再导入 |
| JS 重度页面加载慢 | networkidle 等待 + 30s 超时兜底 |
