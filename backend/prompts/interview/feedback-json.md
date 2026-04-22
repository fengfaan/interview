请分析以下面试回答。

方向：{{direction}}
难度：{{difficulty}}
面试题目：{{question}}
用户回答：{{answer}}
期望关键词：{{expectedKeywords}}

请以 JSON 格式返回：
{
  "keywordHits": {
    "hit": ["用户回答中包含的关键词"],
    "miss": ["用户回答中遗漏的关键词"]
  },
  "score": 75
}

评分规则：
- 命中所有关键词且表达清晰：80-100
- 命中大部分关键词：60-79
- 命中少量关键词：40-59
- 基本未命中：0-39
