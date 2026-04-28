## ADDED Requirements

### Requirement: 上下文压缩按句子拆分并保留关键词相关内容
系统 SHALL 对超过 `maxChars` 的文本按句子拆分（分隔符为 `。？！\n`），计算每句与关键词的相关性得分，选取得分最高的句子直到总长度不超过 `maxChars`，并追加 `上下文压缩说明` 标记。

#### Scenario: 长上下文保留关键词和因果相关的句子
- **WHEN** contextContent 超过 4000 字符，包含关键词"叶子节点"和"范围查询"
- **THEN** 压缩后长度不超过 4000，保留包含关键词的句子和包含因果连词的相关句子，输出包含"上下文压缩说明"

#### Scenario: 长上下文没有关键词但追问提到相关概念
- **WHEN** contextContent 超过 4000 字符，expectedKeywords 为空，但最新追问消息包含"红黑树"
- **THEN** 压缩后保留包含"红黑树"的句子

#### Scenario: 长 Markdown 行按句子拆分后再选取
- **WHEN** contextContent 包含超过 4000 字符的连续无标点文本，中间有相关句子
- **THEN** 按句号拆分后选取，保留事务隔离级别相关的句子

#### Scenario: 短上下文不变
- **WHEN** contextContent 少于 4000 字符
- **THEN** 原样返回，不做任何修改

#### Scenario: 全低价值内容 fallback 到头尾保留
- **WHEN** 所有句子与关键词均不相关
- **THEN** 输出包含"原文前半"、"原文后半"和"省略"标记

#### Scenario: null 上下文返回空字符串
- **WHEN** contextContent 为 null
- **THEN** 返回空字符串

### Requirement: 对话历史截断
系统 SHALL 对对话历史做以下处理：
- messages 为 null 或空时，history 输出空字符串
- messages 超过 12 条时，只保留最后 12 条（约最近 6 轮对话）

#### Scenario: 14 条消息只保留最后 12 条
- **WHEN** messages 包含 14 条消息（7 轮）
- **THEN** history 不包含消息1和消息2，包含消息3到消息14

#### Scenario: null messages 返回空 history
- **WHEN** messages 为 null
- **THEN** history 为空字符串

#### Scenario: 空 messages 返回空 history
- **WHEN** messages 为空列表
- **THEN** history 为空字符串

### Requirement: 单条消息压缩
系统 SHALL 对超过 600 字符的单条消息截断，保留前 500 字符并追加 `[已压缩，原文 N 字]` 标记。

#### Scenario: 超长消息被压缩
- **WHEN** 一条用户消息包含 3500 字符（"为什么"重复 700 次）
- **THEN** history 长度小于 1300 字符，包含"已压缩"标记
