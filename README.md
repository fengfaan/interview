# Interview Assistant

Interview Assistant 是一个面向开发者的本地 AI 面试准备工作台。它把模拟面试、快速刷题、回答点评、推荐背题答案、简历匹配分析、STAR 改写、Obsidian 知识沉淀和 Prompt 调优放在同一个 Web 应用里，适合每天打开练一轮、改一段简历、沉淀一篇笔记。

项目采用前后端分离架构：

- 前端：Vue 3 + TypeScript + Vite + Pinia + Vue Router + Tailwind CSS
- 后端：Spring Boot 3 + Java 17 + Spring AI
- AI 渠道：智谱 GLM、OpenRouter OpenAI 兼容接口
- 数据存储：本地配置文件、浏览器 LocalStorage、Obsidian Markdown 笔记

它不是招聘 SaaS，也不需要账号系统或数据库。默认定位是个人本地使用。

## 功能概览

### 模拟面试

- 按方向和难度生成面试题
- 支持 Go 后端、React 前端、系统设计、数据库、AI Agent 等方向
- 根据你的回答输出关键词命中、评分和点评
- 支持流式点评，不必等待整段内容一次性返回
- 一键生成 Markdown 格式的推荐背题答案
- 对点评或推荐答案继续发起深挖追问

### 快速刷题

- 一次生成 10、20、50 或 100 道题
- 大批量题目会被后端拆成小批次请求 AI
- 通过 SSE 增量推送，前端收到一批就展示一批
- 单批失败会自动重试，仍失败时跳过该批并保留已生成内容
- 题目、参考答案和关键词可单题保存或批量保存到 Obsidian

### 简历调优

- 粘贴 JD 和简历，生成匹配度分析
- 识别技能缺口、项目表达弱点和业务价值缺失
- 生成 STAR 改写示例
- 可把改写结果应用回简历编辑区
- 对过短或无效输入做基础拦截，减少虚高评分

### Obsidian 知识库

- 在设置页配置本地 Obsidian Vault 绝对路径
- 面试反馈、推荐答案和刷题内容可保存为 Markdown
- 默认写入 `<Vault>/面试知识库/`
- 笔记带 YAML frontmatter，方便在 Obsidian 中筛选和检索
- 应用内支持浏览、搜索和查看笔记详情

### Prompt 管理

- Prompt 模板位于 `backend/prompts/`
- 设置页支持查看、编辑和保存 Prompt
- 支持用 AI 生成 Prompt 优化草稿
- 修改后下一次请求立即生效，无需重新编译

## 预览

### 面试演练室

![Mock Interview Room](stitch-export/ai-career-prep-hub/mock-interview-room.png)

### 简历调优台

![Resume Optimizer](stitch-export/ai-career-prep-hub/resume-optimizer.png)

## 环境要求

- Node.js 18+
- Java 17
- Maven 3.8+，或直接使用 `backend/mvnw`
- 智谱 AI API Key：[open.bigmodel.cn](https://open.bigmodel.cn/)
- 可选：OpenRouter API Key：[openrouter.ai](https://openrouter.ai/)
- 可选：Obsidian 和一个本地 Vault

## 快速启动

### 1. 克隆项目

```bash
git clone <repository-url>
cd interviewAssistant
```

### 2. 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

后端默认运行在：

```text
http://localhost:8080
```

首次启动可以不预设 API Key，进入前端设置页后再填写。如果希望通过环境变量提供智谱 Key：

```bash
export ZHIPU_API_KEY="your_zhipu_api_key"
```

### 3. 启动前端

另开一个终端：

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在：

```text
http://localhost:5173
```

Vite 会把 `/api` 自动代理到后端服务。

## 第一次使用

1. 打开 `http://localhost:5173`
2. 进入「设置」
3. 选择 AI 渠道，填写对应 API Key
4. 智谱模型建议先使用 `glm-4-flash`
5. 如果使用 Obsidian，填写 Vault 的绝对路径
6. 回到「面试演练室」生成一道题
7. 输入回答，查看关键词命中和 AI 点评
8. 生成推荐背题答案，并把有价值的内容保存到知识库

## 配置项

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `ZHIPU_API_KEY` | `not-configured` | 智谱 AI API Key，也可在设置页保存 |
| `OPENROUTER_API_KEY` | 空 | OpenRouter API Key，也可在设置页保存 |
| `OPENROUTER_PROXY` | 空 | OpenRouter 代理，例如 `http://127.0.0.1:7890` |
| `SETTINGS_FILE` | 自动解析 | 本地设置文件路径 |
| `PROMPT_DIR` | `prompts` | Prompt 模板目录 |
| `AI_SYNC_TIMEOUT_MS` | `120000` | 同步 AI 请求超时时间 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:8080` | 后端允许的前端来源 |

设置页保存的 API Key、模型、AI 渠道和 Vault 路径会写入本地 `settings.properties`。后端启动日志会输出实际解析到的设置文件路径。

如果使用 OpenRouter 且本机网络不稳定，可以配置：

```bash
export OPENROUTER_PROXY="http://127.0.0.1:7890"
```

## 常用命令

### 后端

```bash
cd backend
./mvnw test
./mvnw spring-boot:run
```

### 前端

```bash
cd frontend
npm install
npm run build
npm run dev
```

## 项目结构

```text
interviewAssistant/
  backend/
    prompts/                         # Prompt 模板
    src/main/java/com/interviewassistant/
      common/                        # 通用响应、异常和 SSE 工具
      config/                        # AI、CORS、异步配置
      controller/                    # REST 和 SSE 接口
      dto/                           # 请求和响应对象
      prompt/                        # Prompt 标签辅助
      service/                       # AI、设置、Prompt、Obsidian 服务
    src/main/resources/
      application.yml                # 后端配置
    src/test/                        # 后端测试
  frontend/
    src/
      api/                           # HTTP 和 SSE 请求封装
      components/                    # 通用组件
      layouts/                       # 页面布局
      router/                        # 路由配置
      stores/                        # Pinia 状态
      types/                         # TypeScript 类型
      utils/                         # Markdown 和 LocalStorage 工具
      views/                         # 页面视图
  docs/                              # 需求和技术设计文档
  openspec/                          # 变更规格说明
  stitch-export/                     # 设计稿导出资源
```

## API 概览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/interview/question` | 生成单道面试题 |
| `POST` | `/api/interview/batch-questions` | 批量生成题目 |
| `POST` | `/api/interview/batch-questions/stream` | SSE 分批生成快速刷题题目 |
| `POST` | `/api/interview/feedback` | 分析回答并返回评分 |
| `POST` | `/api/interview/feedback/stream` | SSE 流式输出点评 |
| `POST` | `/api/interview/recommended-answer/stream` | SSE 流式输出推荐答案 |
| `POST` | `/api/interview/deep-dive/stream` | SSE 深挖追问 |
| `POST` | `/api/resume/analyze` | JD 和简历匹配分析 |
| `POST` | `/api/resume/rewrite/stream` | SSE STAR 改写 |
| `GET` | `/api/knowledge/notes` | 获取知识库笔记列表 |
| `GET` | `/api/knowledge/note?id=...` | 获取笔记详情 |
| `POST` | `/api/knowledge/notes` | 保存笔记到 Obsidian |
| `GET` | `/api/knowledge/search` | 搜索知识库 |
| `GET` | `/api/settings/apikey` | 获取 API Key 脱敏状态 |
| `POST` | `/api/settings/apikey` | 保存 API Key |
| `GET` | `/api/settings/model` | 获取当前模型和渠道 |
| `POST` | `/api/settings/model` | 保存模型和渠道 |
| `GET` | `/api/settings/vault` | 获取 Obsidian Vault 配置 |
| `POST` | `/api/settings/vault` | 保存 Obsidian Vault 路径 |
| `GET` | `/api/settings/prompts` | 列出 Prompt 文件 |
| `GET` | `/api/settings/prompts/content` | 获取 Prompt 内容 |
| `PUT` | `/api/settings/prompts/content` | 保存 Prompt 内容 |
| `POST` | `/api/settings/prompts/improve` | AI 优化 Prompt |

## Obsidian 笔记格式

配置 Vault 后，应用会在 Vault 下创建或复用：

```text
面试知识库/
```

保存的笔记类似：

```markdown
---
title: "请解释 Go 的垃圾回收机制"
direction: "后端"
tags: ["GC", "三色标记", "写屏障"]
created: "2026-04-23T14:30:00"
source: "recommended-answer"
questionId: "q_001"
---

## 背题思路

...
```

应用只读写 `面试知识库/` 下的 Markdown 文件，避免污染 Vault 根目录。

## Prompt 目录

```text
backend/prompts/
  interview/
    system.md
    question.md
    batch-question.md
    feedback-json.md
    feedback-stream.md
    recommended-answer.md
    deep-dive.md
  resume/
    system.md
    analyze.md
    rewrite.md
  settings/
    prompt-improver.md
```

如果要使用外部 Prompt 目录：

```bash
export PROMPT_DIR="/absolute/path/to/prompts"
```

## 常见问题

### API Key 保存后重启失效

查看后端启动日志中的 `Settings file resolved`。如果用 IDE 启动，工作目录可能不是 `backend/`，建议显式设置 `SETTINGS_FILE`。

### 返回 401

通常是 API Key 无效、过期或复制错误。进入「设置」重新保存 API Key，并确认当前选择的 AI 渠道和 Key 匹配。

### 返回 429

说明触发了供应商速率限制。等待 30 到 60 秒后重试，或切换额度更高的 Key/模型。

### OpenRouter 连接失败

常见原因是本机到 `openrouter.ai` 的链路不稳定。可以配置 `OPENROUTER_PROXY`，或临时切换到智谱等更稳定的渠道。

### Obsidian 保存失败

确认填写的是 Vault 根目录的绝对路径，并且后端进程对该目录有读写权限。

## 后续可迭代方向

- 增加错题回顾和按标签复习
- 为知识库笔记增加复习状态
- 增加简历版本快照
- 增加更多技术方向和面试风格
- 一键导出完整面试复盘 Markdown

## License

MIT
