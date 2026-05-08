## 1. 后端 Prompt 模板

- [ ] 1.1 创建 `backend/prompts/import/consolidate.md` 清洗整理 prompt，接收题目 JSON 数组，返回去重合并后的分类结构
- [ ] 1.2 在 `PromptService.PROMPT_DESCRIPTIONS` 中注册新 prompt 条目

## 2. 后端 DTO

- [ ] 2.1 创建 `ConsolidateRequest` DTO：包含 `items: List<ParsedQuestion>`, `sourceUrl`, `title` 字段
- [ ] 2.2 创建 `ConsolidateResult` DTO：包含 `categories: List<ConsolidatedCategory>`, `dedupCount`, `totalCount` 字段
- [ ] 2.3 创建 `ConsolidatedCategory` DTO：包含 `categoryName`, `items: List<ParsedQuestion>` 字段
- [ ] 2.4 创建 `ConsolidatedSaveRequest` DTO：包含 `categories: List<ConsolidatedCategory>`, `sourceUrl`, `title` 字段

## 3. 后端 ConsolidateService

- [ ] 3.1 创建 `ConsolidateService` 类，注入 `AiGateway` 和 `PromptService`
- [ ] 3.2 实现 `consolidateStream()` 方法：渲染清洗 prompt，调用 AI，解析 JSON 结果为 `ConsolidateResult`
- [ ] 3.3 实现清洗结果 JSON 解析逻辑：处理 AI 返回的分组结构和去重信息

## 4. 后端 ObsidianService 扩展

- [ ] 4.1 在 `ObsidianService` 中新增 `createConsolidatedNote()` 方法，接收 `ConsolidatedSaveRequest`，生成合并文件格式的 Markdown 并写入 Vault

## 5. 后端 Controller

- [ ] 5.1 在 `ImportController` 中新增 `POST /api/import/consolidate/stream` SSE 端点，调用 `ConsolidateService.consolidateStream()`
- [ ] 5.2 在 `ImportController` 中新增 `POST /api/import/consolidate/save` 端点，调用 `ObsidianService.createConsolidatedNote()`

## 6. 前端 API 层

- [ ] 6.1 在 `frontend/src/api/importApi.ts` 中新增 `consolidateStream()` 方法（SSE 调用）
- [ ] 6.2 在 `frontend/src/api/importApi.ts` 中新增 `consolidatedSave()` 方法

## 7. 前端 Store

- [ ] 7.1 在 import store 中新增清洗相关状态：`consolidatedResult`, `isConsolidating`, `consolidateProgress`
- [ ] 7.2 实现 `doConsolidate()` action：调用清洗 SSE 接口，累积清洗结果
- [ ] 7.3 实现 `doConsolidatedSave()` action：调用合并保存接口

## 8. 前端 UI

- [ ] 8.1 在 `ImportView.vue` 中新增"AI 清洗并合并保存"按钮（与现有"导入到知识库"并列）
- [ ] 8.2 实现清洗进度展示：SSE 进度状态显示
- [ ] 8.3 实现清洗结果预览：按分类分组展示，显示去重统计信息
- [ ] 8.4 实现清洗结果编辑：支持编辑题目、调整分类、删除
- [ ] 8.5 实现合并保存确认和成功反馈
