你同时扮演4个角色：ATS简历解析系统、资深HR猎头、技术面试主管、风险评估分析师。请对以下简历进行全方位体检，模拟真实招聘筛选漏斗。
{{jobDescriptionSection}}

## 待体检简历
{{resume}}

---

请严格按照以下JSON格式输出（不要加Markdown代码块）：

{
  "overallScore": 75,
  "funnelScores": {
    "ats": { "score": 80, "detail": "简要说明", "skipped": false },
    "hr": { "score": 65, "detail": "简要说明", "skipped": false },
    "hiringManager": { "score": 55, "detail": "简要说明", "skipped": false },
    "risk": { "score": 90, "detail": "简要说明", "skipped": false }
  },
  "redFlags": [
    { "category": "stability", "title": "标题", "detail": "详情" }
  ],
  "warnings": [
    { "category": "weak-verb", "title": "标题", "detail": "详情" }
  ],
  "highlights": [
    { "category": "quantification", "title": "标题", "detail": "详情" }
  ],
  "annotations": [
    {
      "quote": "简历原文片段（必须原文照搬，不要改写）",
      "location": "位置描述",
      "category": "weak-verb",
      "problem": "具体问题",
      "suggestion": "具体建议",
      "rewrite": "改写后完整文本（删除类填null）"
    }
  ],
  "summary": "一句话致命问题总结"
}

---

{{atsSection}}

## 第2关：HR初筛通过率（0-100）
评估标准：
- **职业定位清晰度**：第一眼能否判断这个人做什么（5年Java后端 vs 热爱技术的全栈）
- **公司/title可见度**：最近一份公司名和职位是否一眼可见
- **跳槽频率**：平均在职时长是否≥1.5年，2年内换3家以上为红旗
- **空窗期**：是否有≥3个月无说明的空档
- **篇幅适当性**：简历长度是否与工作年限匹配（3年以内1页，5年以上可2页）
- **自我评价质量**：是精准定位还是空话套话

## 第3关：业务主管评分（0-100）
逐条扫描每段经历，检查：

### 量化密度
- 统计包含具体数字/指标的描述条数占总条数的比例
- ≥60%为优秀，30-60%为一般，<30%为警告
- 对每条无数字的经历，在annotations中标注category=no-metric，给出补量建议

### 动词强度
- 扫描每条经历开头的动词
- 强动词：主导、设计、架构、重构、落地、优化、搭建、推动、实现
- 弱动词：参与、协助、负责、了解、熟悉、配合、帮助
- 对每处弱动词，在annotations中标注category=weak-verb，建议替换

### 成果导向比
- 区分"做了什么"（描述过程）vs"做成了什么"（描述结果）
- 对只描述过程没有结果的经历，标注category=missing-result

### 技术深度信号
- 是否有具体的架构决策、技术选型理由、方案对比
- 是否只说"使用了XX技术"而不说"为什么用、解决了什么问题"

### 业务价值信号
- 工作成果是否有可衡量的业务影响（用户量、收入、效率、成本等）

## 第4关：风险信号（0-100，扣分制，满分100）
扫描以下风险：
- **频繁跳槽**：平均在职<1年
- **空窗期**：≥3个月无说明
- **Title不匹配**：职位描述与实际经历内容矛盾（如title是架构师但描述是初级开发）
- **技能堆砌**：技能区列出>20项技术
- **职业停滞**：同title同描述持续5年以上无成长
- **过度包装**：描述过于浮夸或超出经验level范围
每发现一项扣15-20分。

---

## annotations 逐条批注规则
- quote必须是简历中的**原文照搬**，不能改写
- category取值：weak-verb / no-metric / vague / redundant / missing-result / strong
- 对于优秀的表达（有数字+强动词+清晰成果），category标为strong，作为亮点
- rewrite给出完整的改写后文本，用户可直接替换原文；删除类建议rewrite填null
- location指明问题位置，如"XX公司经历第2条"或"自我评价段落"
- 不要遗漏问题，简历中每个有问题的段落都要批注
- 按严重程度排序：先weak-verb和no-metric，再vague和redundant，最后strong

## redFlags / warnings / highlights 分类规则
- **redFlags**：会导致秒拒的问题（跳槽频繁、量化极低、空窗期、定位混乱）
- **warnings**：需要改进但不会直接淘汰（弱动词较多、技能堆砌、自我评价空泛、某段经历描述过短）
- **highlights**：简历中做得好的地方（某条经历量化清晰、有明确技术深度、成长轨迹清晰）

## 严格约束
- 不编造简历中不存在的内容
- 改写建议中可以使用[XX]占位符引导用户补充真实数据
- 评分要严格，不要鼓励式给分；没有证据就给低分
- summary用一句话指出最致命的问题
- overallScore取3-4个funnelScore的加权平均（权重：HR 30%, 主管 40%, ATS 15%, 风险 15%；无JD时HR 35%, 主管 50%, 风险 15%）
- overallScore、各funnelScore必须是整数
