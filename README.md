# Interview Assistant — AI 面试准备平台

基于 Vue 3 + Spring Boot 3 + 智谱 GLM 的 AI 面试演练与简历优化工具。

## 功能

### 模拟面试室
- 支持三个技术方向：Go 后端、React 前端、系统设计
- 三档难度：基础八股、深度原理、项目实战
- AI 生成面试题并追踪关键词命中情况
- 流式输出详细点评、口语化示范回答、深度追问
- 追问挑战模式，连续深入练习

### 简历优化器
- 输入 JD + 简历，AI 分析匹配度并评分
- 多维度分析（技能匹配、项目经验、表达能力等）
- 逐条生成优化建议，按优先级排序
- STAR 原则改写：选择建议后流式生成改写范例，一键替换原文

## 技术栈

| 层 | 技术 |
|---|------|
| 前端 | Vue 3.4 + TypeScript 5 + Vite 5 + Pinia + Vue Router 4 |
| 样式 | Tailwind CSS 3.4 |
| 后端 | Spring Boot 3.3 + Java 17 |
| AI | Spring AI 1.1 + 智谱 GLM-4-Flash（OpenAI 兼容接口） |
| 状态持久化 | LocalStorage（无数据库） |

## 项目结构

```
interviewAssistant/
├── frontend/          # Vue 3 前端
│   └── src/
│       ├── api/       # HTTP / SSE 请求封装
│       ├── stores/    # Pinia 状态管理
│       ├── views/     # 页面组件
│       ├── types/     # TypeScript 类型定义
│       └── utils/     # 工具函数
├── backend/           # Spring Boot 后端
│   └── src/main/java/com/interviewassistant/
│       ├── controller/   # REST + SSE 接口
│       ├── service/      # 业务逻辑
│       ├── dto/          # 请求/响应 DTO
│       ├── prompt/       # LLM Prompt 模板
│       ├── config/       # 配置（AI、CORS）
│       └── common/       # 通用响应封装、异常处理
└── docs/              # 需求与设计文档
```

## 快速开始

### 环境要求

- Node.js >= 18
- Java 17
- Maven 3.8+
- 智谱 AI API Key（[申请地址](https://open.bigmodel.cn/)）

### 1. 配置环境变量

```bash
export ZHIPU_API_KEY="your_api_key_here"
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端启动在 `http://localhost:8080`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动在 `http://localhost:5173`，Vite 自动代理 `/api` 请求到后端。

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/interview/question` | 生成面试题 |
| POST | `/api/interview/feedback` | 分析回答（关键词命中 + 评分） |
| POST | `/api/interview/feedback/stream` | 流式详细点评（SSE） |
| POST | `/api/resume/analyze` | 简历匹配分析 |
| POST | `/api/resume/rewrite/stream` | STAR 改写（SSE） |

## License

MIT
