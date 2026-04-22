请分析简历与目标职位的匹配度。

## 目标职位 JD
{{jobDescription}}

## 简历内容
{{resume}}

请以 JSON 格式返回：
{
  "score": 0,
  "dimensions": [
    {"name": "维度名称", "score": 0, "reason": "评分理由"}
  ],
  "suggestions": [
    {"id": "s_001", "priority": "HIGH", "title": "建议标题", "reason": "建议理由", "sourceText": "简历中对应的原文"}
  ]
}

要求：
- score: 0-100 的整体匹配度
- dimensions: 3-5 个核心能力维度评分
- suggestions: 2-5 条优化建议，priority 为 HIGH/MEDIUM/LOW
- sourceText 必须是简历中实际存在的文本片段
- suggestion id 使用 "s_" + 递增数字
- 严格评分，不要鼓励式给分；没有证据就给低分
- 如果 JD 或简历内容过短、随机、无岗位信息、无项目经历、无技能信息，score 必须为 0-10
- 如果简历中没有 JD 要求的明确证据，对应维度必须低于 40
- 不允许根据常识脑补候选人经历或技能
