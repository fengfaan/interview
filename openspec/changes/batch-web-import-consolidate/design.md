## Context

当前网页抓题流程：URL 抓取 → AI 解析为结构化题目 → 用户预览编辑 → 逐条保存为独立 Markdown 文件。每次抓取可能产生 20-50 道题目，保存后形成几十个分散文件，用户难以整体回顾。

现有代码中：
- `ImportController.save()` 接收 `List<ParsedQuestion>` 循环调用 `obsidianService.createNote()`
- `ObsidianService.createNote()` 每次只写一个文件
- 前端 `ImportView.vue` 通过 checkbox 选择题目后逐条保存

## Goals / Non-Goals

**Goals:**
- 在解析完成后、保存前，提供"AI 清洗整理"能力：去重、合并相似题、统一格式、补充分类
- 将清洗后的整批题目合并写入一个 Markdown 文件，按分类结构组织
- 与现有逐条保存模式共存，用户可选择保存方式

**Non-Goals:**
- 不修改现有逐条保存逻辑（保持向后兼容）
- 不做跨批次去重（仅当次抓取范围内去重）
- 不修改 ObsidianService 的核心 createNote 方法

## Decisions

### 1. AI 清洗通过新增 SSE 端点实现
**选择**: 新增 `POST /api/import/consolidate/stream` SSE 端点
**理由**: 清洗过程可能较慢，SSE 流式推送进度和中间结果与现有解析流保持一致
**备选**: 同步 REST 端点 — 简单但体验差，大量题目时用户无反馈

### 2. 清洗 prompt 设计
**选择**: 新增 `prompts/import/consolidate.md` 专用 prompt
**理由**: 清洗逻辑（去重、合并、分类）与解析逻辑不同，需要独立 prompt
**输入**: 整批解析后的题目 JSON（`[{q, a, k}]`）
**输出**: 清洗后的结构化 JSON，包含分组信息和去重结果

### 3. 合并文件格式
**选择**: 单个 Markdown 文件，按主题分组，每组包含多道题目
**格式**:
```markdown
---
title: "Go 后端面试题集 - <来源标题>"
direction: "网页抓题"
tags: ["Go", "后端", ...]
created: "<timestamp>"
source: "web-import-consolidated"
url: "<原始URL>"
questionCount: <N>
---

# Go 后端面试题集

## 基础概念

### Go 是面向对象的语言吗？
**关键词**: Go, 面向对象, 接口

参考答案内容...

### Goroutine 和线程的区别？
**关键词**: Goroutine, 线程, 调度

参考答案内容...

## 并发编程

### ...
```
**理由**: 按 `## 分类标题` + `### 题目` 的层级组织，符合 Obsidian 大纲导航习惯
**备选**: 每道题用 callout 块 — 过于冗长，不利于搜索

### 4. 前端交互流程
**选择**: 在现有"导入到知识库"按钮旁增加"AI 清洗并合并保存"按钮
**理由**: 不改变现有用户习惯，新增选项让用户自主选择
**流程**: 点击 → SSE 流式展示清洗进度 → 清洗完成预览结果 → 确认保存

### 5. 后端新增 ConsolidateService
**选择**: 独立服务类，不修改现有 ImportService
**理由**: 清洗逻辑独立，不影响现有导入流程；未来可能被其他场景复用

## Risks / Trade-offs

- **[题目数量过多导致单文件过大]** → Mitigation: 设置上限（如 100 题），超出时提示用户分批处理
- **[AI 清洗可能误删或错误合并题目]** → Mitigation: 清洗后提供预览，用户可编辑后再保存；原始解析结果保留
- **[单文件在 Obsidian 中不如多文件灵活]** → Mitigation: 保留逐条保存选项，两种模式共存
- **[清洗耗时较长]** → Mitigation: SSE 流式反馈进度，减少用户等待焦虑
