## Why

当前简历模块只有"JD匹配分析 + 单条STAR改写"两个功能，无法满足求职者的两大核心痛点：**不知道简历结构毛病在哪**（缺模块、排序混乱、头重脚轻）以及**知道毛病但不会改**（描述口语化、缺数据、动词软弱）。现有 analyze 接口必须依赖 JD 才能运行，无法独立对简历做"体检"；rewrite 只能逐条改写且只生成一个版本，用户没有选择空间。

## What Changes

- 新增**简历结构分析**端点（无需 JD），从模块完整性、排序逻辑、篇幅密度、格式规范四个维度诊断简历宏观布局问题，输出结构化体检报告（含分项评分、红黄绿灯状态、缺失模块列表）
- 新增**简历深度润色**端点，对选定经历段落进行多版本改写（技术深度版、业务结果版），注入 STAR 法则、强化动词、倒逼数据化，并以批注形式向用户追问缺失的量化数据
- 新增对应的前端交互：结构分析体检报告卡片（含雷达图/评分可视化），润色对比视图（修改前 vs 修改后 Diff）
- 新增 2 个 prompt 模板：`resume/structure-analysis.md`、`resume/deep-polish.md`
、、
## Capabilities

### New Capabilities
- `resume-structure-analysis`: 独立于 JD 的简历结构诊断，覆盖模块完整性、排序逻辑、篇幅密度、格式规范四个维度，输出结构化 JSON 体检报告
- `resume-deep-polish`: 对指定经历段落进行多版本深度润色，包含 STAR 法则注入、动词强效化、数据化追问批注，支持可选的 JD 关键词融入

### Modified Capabilities
_(无已有 spec 需要修改，现有 analyze + rewrite 功能保持不变)_

## Impact

- **后端**: 新增 2 个 Controller 端点（`POST /api/resume/structure-analysis`、`POST /api/resume/polish/stream`）、对应 DTO、Service 方法，复用现有 AiGateway + PromptService 架构
- **前端**: ResumeOptimizerView 新增结构分析入口和结果展示区域、润色对比视图组件，resumeStore 扩展状态
- **Prompt**: 新增 2 个模板文件到 `backend/prompts/resume/`
- **API**: 纯新增端点，不影响现有 `/api/resume/analyze` 和 `/api/resume/rewrite/stream`
