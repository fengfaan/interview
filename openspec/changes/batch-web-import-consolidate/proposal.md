## Why

网页抓题目前以"一条一文件"的方式保存到知识库，导致同一来源的题目分散在数十个独立 Markdown 文件中，难以整体回顾和复习。用户希望抓完题后，经过 AI 清洗整理（去重、合并、分类），将同批次题目统一存入一个知识库文件，形成结构化的专题笔记。

## What Changes

- 新增"AI 清洗整理"步骤：在用户确认题目后、保存前，调用 LLM 对整批题目进行去重、合并相似题、统一格式、补充分类标签
- 新增"批量合并保存"功能：将清洗后的整批题目写入单个 Markdown 文件（而非逐条创建独立文件），文件结构按专题/分类组织
- 新增合并保存 API：`POST /api/import/consolidate/stream`（AI 清洗）和 `POST /api/import/consolidate/save`（合并保存），原有逐条保存 API 不变
- 修改前端 Import 页面：增加"AI 清洗并合并保存"操作入口，展示清洗进度和预览

## Capabilities

### New Capabilities
- `batch-consolidate-import`: AI 驱动的批量题目清洗（去重、合并、分类）与合并保存到单一知识库文件

### Modified Capabilities
- `knowledge-save`: 新增合并保存模式，支持将多道题目写入同一 Markdown 文件
- `question-parse-import`: 在解析与保存之间插入 AI 清洗整理步骤

## Impact

- **后端 API**: `ImportController` / `ImportService` 增加 AI 清洗端点和合并保存逻辑
- **后端 Prompt**: 新增清洗整理专用 prompt 模板
- **前端 Import 页面**: 增加清洗操作按钮和进度展示
- **知识库文件结构**: 新的合并文件格式，与现有单条文件共存
- **ObsidianService**: 新增批量写入单文件方法
