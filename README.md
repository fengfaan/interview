# Interview Assistant

本地 AI 面试准备工作台。模拟面试、快速刷题、回答点评、推荐背题答案、简历调优（JD 匹配 / 结构体检 / 深度润色）、简历文件导入、网页题目抓取、Obsidian 知识沉淀、提示词风格配置和 Prompt 调优，全部集成在一个 Web 应用里。面向个人本地使用，不需要账号系统、数据库或云服务。

前后端分离架构：

- **前端**：Vue 3 + TypeScript + Vite + Pinia + Vue Router + Tailwind CSS
- **后端**：Spring Boot 3 + Java 17 + Spring AI
- **AI 渠道**：智谱 GLM、MiMo、OpenRouter（OpenAI 兼容接口）
- **数据存储**：本地配置文件、浏览器 LocalStorage、Obsidian Markdown 笔记

## 功能概览

### 模拟面试

- 按技术方向（Go 后端、React 前端、系统设计、数据库、AI Agent）和难度级别生成面试题
- 根据回答输出关键词命中、评分和流式点评
- 一键生成 Markdown 格式的推荐背题答案
- 对点评或推荐答案发起多轮深挖追问（DeepDive Agent 自主调用知识库工具检索）

### 快速刷题

- 一次生成 10、20、50 或 100 道题，后端拆批请求 AI
- SSE 增量推送，前端收到一批就展示一批
- 单批失败自动重试，仍失败则跳过并保留已生成内容
- 题目、参考答案和关键词可单题或批量保存到 Obsidian

### 简历调优

三个独立功能通过 Tab 切换，共享简历输入：

- **JD 匹配分析**：粘贴 JD 和简历，生成匹配度评分、维度分析和优化建议，点击建议可流式生成 STAR 改写并一键应用到简历
- **结构体检**：无需 JD，AI 从模块完整性、排序逻辑、篇幅密度三个维度诊断简历宏观布局，输出评分、红黄绿灯状态和问题列表
- **深度润色**：粘贴某段经历描述，AI 生成技术深度版和业务结果版两个改写版本，应用 STAR 法则、强化动词，并以批注形式追问缺失的量化数据
- **文件导入**：支持上传 PDF / DOCX 简历文件，自动提取文本填充到输入区

### 网页题目抓取

- 通过内置浏览器抓取或手动粘贴网页文本
- AI 自动解析提取面试题，支持去重、合并、分类
- 解析结果可直接导入快速刷题库

### Obsidian 知识库

- 配置本地 Obsidian Vault 路径后，面试反馈、推荐答案和刷题内容可保存为带 YAML frontmatter 的 Markdown 笔记
- 保存前自动做向量去重检查，避免重复笔记
- 应用内支持浏览、搜索和查看笔记详情
- 内置 ONNX 向量推理（`bge-micro-v2`），无需外部 sidecar 即可做语义检索

### 提示词风格

- 按技术方向 × 难度级别（5 方向 × 3 级别 = 15 个组合）配置个性化出题风格
- 三个维度：出题侧重、场景偏好、关键词风格
- 风格通过 `{{styleInstruction}}` 占位符自动注入面试相关 Prompt 模板，无需手动编辑模板

### Prompt 管理

- 所有 Prompt 模板以 `.md` 文件存放在 `backend/prompts/`，支持 `{{placeholder}}` 变量替换
- 设置页可查看、编辑和保存 Prompt，修改后下次请求立即生效
- 支持用 AI 生成 Prompt 优化草稿

### 模型容错

- 内置熔断器（Circuit Breaker），当某个模型连续失败时自动切换到备用模型
- 支持配置多模型降级链路，避免单模型故障导致整体不可用
- 设置页可查看各模型健康状态

## 预览

### 面试演练室

![Mock Interview Room](stitch-export/ai-career-prep-hub/mock-interview-room.png)

### 简历调优台

![Resume Optimizer](stitch-export/ai-career-prep-hub/resume-optimizer.png)

## 环境要求

- Node.js 18+
- Java 17
- Maven 3.8+，或使用 `backend/mvnw`
- 智谱 AI API Key：[open.bigmodel.cn](https://open.bigmodel.cn/)
- 可选：MiMo API Key、OpenRouter API Key
- 可选：Obsidian 和本地 Vault

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

后端默认运行在 `http://localhost:8080`。

首次启动可不预设 API Key，进入前端设置页后再填写。也可通过环境变量提供：

```bash
export ZHIPU_API_KEY="your_zhipu_api_key"
```

### 3. 启动前端

另开终端：

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，Vite 自动将 `/api` 代理到后端。

## 第一次使用

1. 打开 `http://localhost:5173`
2. 进入「设置」，选择 AI 渠道并填写 API Key
3. 智谱建议使用 `glm-4-flash`
4. 如果使用 Obsidian，填写 Vault 绝对路径
5. 回到「面试演练室」生成一道题，输入回答查看点评
6. 生成推荐背题答案，保存有价值的到知识库

## 配置项

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `ZHIPU_API_KEY` | `not-configured` | 智谱 AI API Key |
| `MIMO_API_KEY` | 空 | MiMo API Key |
| `OPENROUTER_API_KEY` | 空 | OpenRouter API Key |
| `OPENROUTER_PROXY` | 空 | OpenRouter 代理，如 `http://127.0.0.1:7890` |
| `SETTINGS_FILE` | 自动解析 | 本地设置文件路径 |
| `PROMPT_DIR` | `prompts` | Prompt 模板目录 |
| `AI_SYNC_TIMEOUT_MS` | `120000` | 同步 AI 请求超时时间 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:8080` | 后端允许的前端来源 |
| `SMART_EMBEDDING_ENDPOINT` | 空（使用内置 ONNX） | 外部向量服务地址 |

设置页保存的配置会写入本地 `settings.properties`。

## 常用命令

```bash
# 后端
cd backend
./mvnw spring-boot:run    # 启动
./mvnw test                # 测试
./mvnw compile             # 编译

# 前端
cd frontend
npm install                # 安装依赖
npm run dev                # 开发服务器
npm run build              # 类型检查 + 生产构建
```

## 项目结构

```text
interviewAssistant/
  backend/
    prompts/                           # Prompt 模板
      styles/                          # 风格配置 JSON（按方向/级别）
    src/main/java/com/interviewassistant/
      ai/
        agent/                         # DeepDive Agent、Knowledge Tools
        circuitbreaker/                # 熔断器和降级服务
        config/                        # AI 渠道配置（AiConfig）
        embedding/                     # ONNX 向量推理（BertTokenizer + OnnxEmbeddingService）
        gateway/                       # AiGateway（统一 AI 调用入口）
        prompt/                        # Prompt 渲染和标签辅助
        style/                         # 风格配置（StyleProfile + StyleService）
        util/                          # AI 工具类
      common/                          # 通用响应、异常、SSE 工具
      config/                          # CORS、异步等全局配置
      controller/                      # REST 和 SSE 接口
      dto/                             # 请求和响应对象（按领域分包）
      service/                         # 业务服务（Interview、Resume、Settings、Obsidian、Prompt）
    src/main/resources/
      application.yml
    src/test/
  frontend/
    src/
      api/                             # HTTP 和 SSE 请求封装
      components/                      # 通用组件
      layouts/                         # 页面布局
      router/                          # 路由配置
      stores/                          # Pinia 状态（7 个 Store）
      types/                           # TypeScript 类型
      utils/                           # Markdown、LocalStorage 工具
      views/                           # 页面视图
  docs/                                # 需求和技术设计文档
```

## API 概览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/interview/question` | 生成单道面试题 |
| `POST` | `/api/interview/batch-questions/stream` | SSE 分批生成快速刷题 |
| `POST` | `/api/interview/feedback` | 分析回答，返回评分 |
| `POST` | `/api/interview/feedback/stream` | SSE 流式点评 |
| `POST` | `/api/interview/recommended-answer/stream` | SSE 流式推荐答案 |
| `POST` | `/api/interview/deep-dive/stream` | SSE 多轮深挖追问 |
| `POST` | `/api/resume/analyze` | JD 和简历匹配分析 |
| `POST` | `/api/resume/structure-analysis` | 简历结构体检 |
| `POST` | `/api/resume/rewrite/stream` | SSE STAR 改写 |
| `POST` | `/api/resume/polish/stream` | SSE 简历深度润色 |
| `POST` | `/api/resume/import-file` | 上传 PDF/DOCX 导入简历文本 |
| `POST` | `/api/import/capture` | 浏览器抓取网页内容 |
| `POST` | `/api/import/parse` | AI 解析网页文本提取面试题 |
| `POST` | `/api/import/consolidate/stream` | SSE AI 清洗整理面试题 |
| `GET` | `/api/knowledge/notes` | 知识库笔记列表 |
| `GET` | `/api/knowledge/search` | 搜索知识库 |
| `GET` | `/api/knowledge/smart-connections/search` | 向量语义检索 |
| `POST` | `/api/knowledge/notes` | 保存笔记到 Obsidian |
| `GET` | `/api/settings/model-health` | 模型健康状态 |
| `GET` | `/api/settings/styles/{direction}/{level}` | 获取风格配置 |
| `PUT` | `/api/settings/styles/{direction}/{level}` | 保存风格配置 |
| `GET` | `/api/settings/prompts` | 列出 Prompt 文件 |
| `PUT` | `/api/settings/prompts/content` | 保存 Prompt 内容 |
| `POST` | `/api/settings/prompts/improve` | AI 优化 Prompt |

## 架构图

```
Browser (Vue 3 SPA :5173)
  ├── REST JSON → /api/* (Vite 代理到 :8080)
  └── SSE streaming → fetch + ReadableStream
        │
Spring Boot API (:8080)
  ├── AiGateway（统一入口）
  │     ├── 熔断器 → 降级到备用模型
  │     └── Spring AI ChatClient
  ├── PromptService ← prompts/*.md 模板 + 风格注入
  ├── StyleService  ← prompts/styles/*.json
  ├── DeepDiveAgent ← KnowledgeTools（知识库检索）
  ├── ResumeFileParser（PDF/DOCX 文件解析）
  └── OnnxEmbeddingService（本地向量推理）
        │
  LLM Provider（智谱 GLM / MiMo / OpenRouter）
```

## License

MIT
