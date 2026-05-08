## 1. 后端 DTO 和 Prompt 模板

- [x] 1.1 创建 `StructureAnalysisRequest` DTO（仅 resume 字段，NotBlank + Size 校验）
- [x] 1.2 创建 `StructureAnalysisResponse` DTO（structureScore, moduleChecks, issues, summary）及内嵌类 `ModuleCheck`(name/status/detail)、`StructuralIssue`(severity/description/suggestion)
- [x] 1.3 创建 `PolishStreamRequest` DTO（必填 sourceText，可选 jobDescription）
- [x] 1.4 编写 Prompt 模板 `backend/prompts/resume/structure-analysis.md`，约束四维度诊断输出 JSON 格式
- [x] 1.5 编写 Prompt 模板 `backend/prompts/resume/deep-polish.md`，约束多版本 STAR 改写 + 数据化追问输出格式

## 2. 后端 Service 层

- [x] 2.1 在 `ResumeAiService` 中新增 `analyzeStructure(String resume)` 方法，调用 `AiGateway.generateJson` 返回 `StructureAnalysisResponse`
- [x] 2.2 在 `ResumeAiService` 中新增 `buildPolishPrompt(String sourceText, String jobDescription)` 方法，加载并渲染 deep-polish 模板
- [x] 2.3 在 `ResumeStreamService` 中新增 `streamPolish(PolishStreamRequest)` 方法，调用 `AiGateway.streamText` 返回 SSE 流
- [x] 2.4 在 `ResumeAiService` 中为结构分析新增输入校验逻辑（resume 最少 50 有效字符 + resume 信号词检测）

## 3. 后端 Controller 层

- [x] 3.1 在 `ResumeController` 中新增 `POST /api/resume/structure-analysis` 端点，调用 `ResumeAiService.analyzeStructure`
- [x] 3.2 在 `ResumeController` 中新增 `POST /api/resume/polish/stream` 端点，调用 `ResumeStreamService.streamPolish`
- [x] 3.3 在 `PromptService.PROMPT_DESCRIPTIONS` 中注册两个新模板的中文描述

## 4. 前端类型和 API

- [x] 4.1 在 `frontend/src/types/resume.ts` 中新增 `StructureAnalysisRequest`、`StructureAnalysisResponse`、`ModuleCheck`、`StructuralIssue`、`PolishStreamRequest` 类型定义
- [x] 4.2 在 `frontend/src/api/resumeApi.ts` 中新增 `analyzeStructure()` 函数（同步 JSON）和 `streamPolish()` 函数（SSE 流式）

## 5. 前端 Store 层

- [x] 5.1 在 `resumeStore` 中新增结构分析相关状态（structureResult, isStructureLoading）和 `analyzeStructure()` action
- [x] 5.2 在 `resumeStore` 中新增润色相关状态（polishResult, polishSourceText, isPolishing）和 `startPolish()` action
- [x] 5.3 在 `resumeStore` 中新增 `activeTab` 状态（'match' | 'structure' | 'polish'）用于 Tab 切换

## 6. 前端视图

- [x] 6.1 在 `ResumeOptimizerView` 顶部新增 Tab 切换栏（JD匹配分析 | 结构体检 | 深度润色）
- [x] 6.2 实现结构体检 Tab 内容区：结构评分卡片 + 模块检查列表（红黄绿灯状态标记）+ 结构问题列表 + 一句话总结
- [x] 6.3 实现深度润色 Tab 内容区：经历段落选择/粘贴输入框 + 可选 JD 输入 + 润色按钮
- [x] 6.4 实现润色结果展示：流式 Markdown 渲染，按 `###` 标题分版本展示，包含数据补充建议区域
- [x] 6.5 结构体检 Tab 中 JD 输入区隐藏（只显示简历输入），润色 Tab 中 JD 输入区标记为可选

## 7. 编译验证

- [x] 7.1 后端编译通过（`./mvnw compile`）
- [x] 7.2 前端构建通过（`npm run build`）