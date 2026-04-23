# Interview Assistant — AI 面试助手

一站式 AI 面试准备平台，提供 **模拟面试演练** 和 **简历智能优化** 两大核心功能，帮助求职者高效备战面试。

基于 Vue 3 + Spring Boot 3 构建，接入智谱 GLM 大模型，支持本地一键部署。

## 核心功能

### 模拟面试室

- 三大技术方向：Go 后端、React 前端、系统设计
- 三档难度：基础八股 → 深度原理 → 项目实战
- AI 实时生成面试题，追踪关键词命中情况并评分
- 流式输出详细点评，包含口语化示范回答与深度追问
- 追问挑战模式，连续深入练习，模拟真实面试节奏

### 简历优化器

- 输入岗位 JD + 个人简历，AI 多维度分析匹配度并评分
- 逐条生成优化建议，按优先级排序，明确改进方向
- 选中建议后一键生成 STAR 原则改写范例，直接替换原文

### Prompt 管理

- 所有 AI 提示词以 Markdown 文件外置，支持运行时编辑、热加载
- 内置 AI 辅助优化 Prompt 功能，无需手动调优

## 项目优势

**零门槛部署** — 无数据库依赖，状态全部存储在浏览器 LocalStorage，后端仅依赖一个 `settings.properties` 文件，克隆即可运行。

**流式响应体验** — 面试点评和简历改写均采用 SSE 流式输出，实时呈现 AI 生成内容，无需等待完整响应。

**Prompt 即配置** — 所有 LLM 提示词模板独立存放于 `backend/prompts/` 目录，修改 Markdown 文件即可定制行为，无需改动代码或重新编译。

**模型可切换** — 设置页面支持运行时切换 GLM 系列模型（glm-4-flash / glm-4-air / glm-4-plus 等），灵活平衡速度与质量。

**结构化 + 流式双反馈** — 面试模块先返回结构化 JSON（关键词命中 + 评分），再流式输出详细点评，兼顾程序可读性与内容丰富度。

**现代技术栈** — Vue 3 Composition API + TypeScript + Pinia + Tailwind CSS 前端，Spring Boot 3 + Spring AI 后端，代码规范、类型安全。

## 技术栈

| 层 | 技术 |
|---|------|
| 前端 | Vue 3.4 + TypeScript 5 + Vite 5 + Pinia + Vue Router 4 |
| 样式 | Tailwind CSS 3.4 |
| 后端 | Spring Boot 3.3 + Java 17 |
| AI | Spring AI 1.1 + 智谱 GLM（OpenAI 兼容接口） |
| 状态持久化 | LocalStorage（无数据库） |

## 本地部署

### 环境要求

- Node.js >= 18
- Java 17
- Maven 3.8+
- 智谱 AI API Key（[点击申请](https://open.bigmodel.cn/)）

### 1. 克隆项目

```bash
git clone <repository-url>
cd interviewAssistant
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端启动在 `http://localhost:8080`。首次运行可在设置页面配置 API Key，或通过环境变量指定：

```bash
export ZHIPU_API_KEY="your_api_key_here"
```

设置页保存的 API Key 和模型默认持久化到 `backend/settings.properties`。如果需要换到其他位置，可以设置 `SETTINGS_FILE=/absolute/path/to/settings.properties`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动在 `http://localhost:5173`，Vite 自动代理 `/api` 请求到后端。

### 4. 开始使用

浏览器打开 `http://localhost:5173`，进入设置页面配置 API Key 和模型，即可开始模拟面试或优化简历。

## 项目结构

```
interviewAssistant/
├── frontend/                    # Vue 3 前端
│   └── src/
│       ├── api/                 # HTTP / SSE 请求封装
│       ├── stores/              # Pinia 状态管理
│       ├── views/               # 页面组件
│       ├── types/               # TypeScript 类型定义
│       └── utils/               # 工具函数
├── backend/                     # Spring Boot 后端
│   ├── prompts/                 # LLM Prompt 模板（Markdown，热加载）
│   └── src/main/java/com/interviewassistant/
│       ├── controller/          # REST + SSE 接口
│       ├── service/             # 业务逻辑
│       ├── dto/                 # 请求/响应 DTO
│       ├── prompt/              # Prompt 加载与渲染
│       ├── config/              # 配置（AI、CORS）
│       └── common/              # 通用响应封装、异常处理
└── docs/                        # 需求与设计文档
```

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/interview/question` | 生成面试题 |
| POST | `/api/interview/feedback` | 分析回答（关键词命中 + 评分） |
| POST | `/api/interview/feedback/stream` | 流式详细点评（SSE） |
| POST | `/api/interview/recommended-answer/stream` | 流式推荐回答（SSE） |
| POST | `/api/resume/analyze` | 简历匹配分析 |
| POST | `/api/resume/rewrite/stream` | STAR 改写（SSE） |
| GET | `/api/settings/api-key` | 获取 API Key（脱敏） |
| PUT | `/api/settings/api-key` | 保存 API Key |
| GET | `/api/settings/model` | 获取当前模型 |
| PUT | `/api/settings/model` | 切换模型 |
| GET | `/api/settings/prompts` | 列出所有 Prompt 文件 |
| GET | `/api/settings/prompts/{path}` | 获取 Prompt 内容 |
| PUT | `/api/settings/prompts` | 保存 Prompt 内容 |
| POST | `/api/settings/prompts/improve` | AI 优化 Prompt |

## License

MIT
