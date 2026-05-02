# Prompt Style Profiles Design

**Date:** 2026-05-01

## Problem

All 5 interview directions (Go后端, React前端, 系统设计, 数据库相关, AI Agent) share the same prompt templates for all 3 question types (基础八股, 深度原理, 项目实战). This produces homogeneous questions regardless of direction or difficulty. Users need per-direction × per-question-type prompt style customization.

## Solution

Add a "Prompt Style Profiles" layer between users and the existing prompt templates. Each of the 15 direction×level combinations stores structured style fields (focusAreas, scenarioPreference, keywordStyle) as JSON files. The backend reads these, converts them into a natural language instruction string, and injects it into existing templates via a new `{{styleInstruction}}` variable.

## Data Model

Each style profile is a JSON file at `prompts/styles/{direction}/{level}.json`:

```json
{
  "focusAreas": "Go runtime 原理、并发模型（GMP）、GC 调优、channel 与锁选型",
  "scenarioPreference": "微服务拆分、分布式事务、高可用缓存、消息队列幂等",
  "keywordStyle": "偏向底层原理术语，如 GMP 调度模型、三色标记法、乐观锁"
}
```

- `focusAreas` — which technical domains to emphasize in questions
- `scenarioPreference` — which business scenarios to prefer
- `keywordStyle` — what style of keywords to generate

Defaults are pre-seeded per direction (not per level — levels inherit direction defaults initially).

## Injection Mechanism

A new `StyleService` converts the JSON fields into a paragraph:

> "出题侧重：{focusAreas}。场景偏好：{scenarioPreference}。关键词风格：{keywordStyle}。"

This string is injected as `{{styleInstruction}}` into the 6 interview prompt templates that need it:
- `interview/question.md`
- `interview/batch-question.md`
- `interview/feedback-stream.md`
- `interview/feedback-json.md`
- `interview/recommended-answer.md`
- `interview/deep-dive.md`

## API

New endpoints under `/api/settings/styles`:

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/settings/styles` | List all style profiles (direction, level, hasCustomization) |
| GET | `/api/settings/styles/{direction}/{level}` | Get specific profile |
| PUT | `/api/settings/styles/{direction}/{level}` | Save specific profile |

## Frontend

Settings page gets a new "提示词风格" card between the model card and the Obsidian vault card. It shows direction buttons (5) + level buttons (3) as selectors, then 3 text inputs for the fields, with save.

## Isolation

The existing "提示词管理" section continues to manage raw `.md` template files. The new "提示词风格" section manages structured style profiles. They are completely independent.

## Scope

This plan covers: backend service + API + JSON defaults + template injection + frontend Settings UI. Does NOT cover: interview room UI changes, new prompt templates, or resume prompts.
