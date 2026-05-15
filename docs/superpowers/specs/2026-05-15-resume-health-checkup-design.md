# 简历综合体检 — Design Spec

Date: 2026-05-15

## Problem

现有简历优化功能是割裂的4个独立tab（JD匹配、结构检查、深度润色、STAR改写），用户需要自己判断用哪个。真实招聘中简历经过筛选漏斗（ATS → HR → 业务主管 → 决策层），每层标准不同。用户不知道自己的简历会在哪一层被筛掉。

## Solution

新增"简历体检"功能，模拟4层筛选漏斗，一次调用返回多维度评估报告。前端采用渐进式诊断流UI，逐层展开漏斗评分，底部附带逐条批注。

## Architecture

```
前端 (新增 "体检" tab, 渐进式诊断流UI)
  │
  │  POST /api/resume/health-checkup
  │  { resume: String, jobDescription?: String }
  │
后端 ResumeController
  └── ResumeAiService.healthCheckup()
        ├── 输入验证 (≥50字符 + 简历关键词信号)
        ├── 渲染 health-checkup.md prompt
        └── aiGateway.generateJson() → HealthCheckupResponse
```

单次同步调用，返回完整JSON。复用现有 AiGateway + generateJson 链路。

## Backend Response Structure

```json
{
  "overallScore": 72,
  "funnelScores": {
    "ats": { "score": 80, "detail": "...", "skipped": false },
    "hr": { "score": 60, "detail": "..." },
    "hiringManager": { "score": 50, "detail": "..." },
    "risk": { "score": 85, "detail": "..." }
  },
  "redFlags": [
    { "category": "stability", "title": "...", "detail": "..." }
  ],
  "warnings": [
    { "category": "weak-verb", "title": "...", "detail": "..." }
  ],
  "highlights": [
    { "category": "recent-experience", "title": "...", "detail": "..." }
  ],
  "annotations": [
    {
      "quote": "原文片段",
      "location": "XX公司经历第2条",
      "category": "weak-verb",
      "problem": "问题描述",
      "suggestion": "修改建议",
      "rewrite": "改写后文本（可为null）"
    }
  ],
  "summary": "一句话总结"
}
```

## Evaluation Dimensions

### Layer 1: ATS (0-100, skipped when no JD)

| Check | Criteria |
|-------|----------|
| JD关键词精确覆盖 | JD中的技术名词是否在简历中原样出现 |
| Section标题规范性 | 是否使用标准标题（工作经历/项目经历/教育背景/专业技能） |
| 日期格式一致性 | 统一的日期格式，可被ATS解析 |
| 结构可解析性 | 是否有清晰的section划分 |

### Layer 2: HR初筛 (0-100)

| Check | Criteria |
|-------|----------|
| 职业定位清晰度 | 10秒内能否判断"这个人做什么" |
| 公司/title可见度 | 最近一份经历的职位和公司是否一目了然 |
| 跳槽频率 | 平均在职时长是否 ≥ 1.5年 |
| 空窗期 | 是否有 ≥ 3个月的无说明空档 |
| 篇幅适当性 | 简历长度是否与工作年限匹配 |

### Layer 3: 业务主管评估 (0-100)

| Check | Criteria |
|-------|----------|
| 量化密度 | 有数字的成果占比（≥60%为优秀，<20%为警告） |
| 动词强度 | 强动词（主导/设计/落地）vs 弱动词（参与/协助/负责）比例 |
| 成果导向比 | "做成了什么" vs "做了什么" 的比例 |
| 技术深度信号 | 是否有具体的架构决策和选型理由 |
| 业务价值信号 | 是否有可衡量的业务影响 |

### Layer 4: 风险信号 (0-100, 扣分制)

| Signal | Trigger |
|--------|---------|
| 频繁跳槽 | 平均在职 < 1年 |
| 空窗期 | ≥ 3个月无说明 |
| Title不匹配 | 职位描述与实际经历内容矛盾 |
| 技能堆砌 | 技能区 > 20项技术 |
| 职业停滞 | 同title同描述持续5年+ |
| 过度包装 | 描述过于浮夸或超出经验范围 |

## Frontend UI — Progressive Diagnosis Flow

```
┌─────────────────────────────────────────────────┐
│  [JD匹配] [结构检查] [深度润色] [简历体检]       │
├─────────────────────────────────────────────────┤
│                                                   │
│  ┌─ 左侧输入 ─────────┐  ┌─ 右侧报告 ─────────┐ │
│  │                     │  │                      │ │
│  │  简历文本 (共享)     │  │  综合评分: 72/100   │ │
│  │                     │  │                      │ │
│  │  目标JD (可选)      │  │  ▾ 第1关 ATS  80%   │ │
│  │  "提供JD可获得       │  │    展开的详情...     │ │
│  │   ATS关键词分析"    │  │                      │ │
│  │                     │  │  ▸ 第2关 HR   60%   │ │
│  │  [开始体检]         │  │  ▸ 第3关 主管  50%   │ │
│  │  ⌘+Enter           │  │  ▸ 第4关 风险  85%   │ │
│  │                     │  │                      │ │
│  └─────────────────────┘  │  ── 逐条批注 (12) ── │ │
│                           │  引用原文 + 分类标签  │ │
│                           │  + 改写建议 + [应用]  │ │
│                           └──────────────────────┘ │
└─────────────────────────────────────────────────┘
```

每层漏斗默认折叠，点击展开显示该层的详细发现（红旗/警告/亮点）。
底部逐条批注区始终可见，带应用改写按钮。

## Annotation Categories

| Category | Label | Color |
|----------|-------|-------|
| weak-verb | 弱动词 | amber |
| no-metric | 无量化 | orange |
| vague | 表述模糊 | yellow |
| redundant | 内容冗余 | gray |
| missing-result | 缺少成果 | red |
| strong | 优秀表达 | green |

## No-JD Mode

When JD is not provided:
- ATS funnel layer: `score = null`, `skipped = true`, `detail = "未提供JD，ATS关键词匹配已跳过"`
- Overall score calculated from remaining 3 layers only
- All other dimensions evaluated normally

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| AI调用次数 | 单次同步调用 | 复用现有模式，避免多次调用延迟 |
| Tab策略 | 新增第4个tab | 不影响现有功能，并行存在 |
| DTO策略 | 独立新DTO | 和StructureAnalysisResponse解耦 |
| Prompt策略 | 独立新prompt | 漏斗视角vs结构视角，评估维度完全不同 |
| JD依赖 | 可选 | 支持海投用户的通用体检需求 |

## Relationship to Existing Features

- 结构检查tab保留不变，定位为"轻量结构修复"
- 体检tab定位为"全面诊断入口"
- 信息架构维度与结构检查有重叠，但体检从漏斗视角评估，结构检查从模块视角评估
- 深度润色和STAR改写是体检之后的"修复工具"

## Tasks (20 total)

1. Backend DTOs (5 tasks): HealthCheckupRequest, HealthCheckupResponse, Finding, Annotation, FunnelScore
2. Backend Prompt (1 task): health-checkup.md
3. Backend Service & Controller (2 tasks): service method + endpoint
4. Frontend Types & Store (5 tasks): types, state, actions, persistence
5. Frontend API (1 task): healthCheckup API call
6. Frontend UI (6 tasks): tab, input, funnel, findings, annotations, wiring
