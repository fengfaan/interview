## Why

当前面试演练室每次只能生成一道题，用户如果想快速浏览大量题目来查漏补缺，需要反复点击"跳过"或"继续挑战"，效率很低。需要一个批量出题模式，一次性生成 10/20/50/100 道题，点击题目即可展开答案，方便用户快速刷题和自测。

## What Changes

- 新增「快速刷题」页面/路由，支持选择方向、题型和出题数量（10/20/50/100）
- 后端新增批量出题 API，一次请求生成多道题目（含答案）
- 前端展示题目列表，点击题目折叠/展开对应答案
- 支持将题目一键保存到 Obsidian 知识库
- 侧边栏新增「快速刷题」导航入口

## Capabilities

### New Capabilities

- `rapid-question-batch`: 批量出题功能——一次生成多道面试题及答案，支持点击展开查看答案、保存到知识库

### Modified Capabilities

（无已有能力需要修改）

## Impact

- **后端新增**: 批量出题 API 端点，复用现有 prompt 模板体系但需新建批量出题 prompt
- **前端新增**: 快速刷题页面组件、路由、侧边栏入口
- **API**: 新增 `POST /api/interview/batch-questions` 端点，返回题目+答案列表
- **Prompt**: 新增 `backend/prompts/interview/batch-question.md` 模板
