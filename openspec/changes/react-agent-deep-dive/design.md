## Context

当前深度追问流程是前端将完整对话历史发给后端 `InterviewStreamService.streamDeepDive()`，后者调用 `AiGateway.streamText()` 一次性流式返回。当推荐答案或反馈点评很长（几千字）时，整个上下文原样灌入 prompt，导致 token 浪费和回答质量下降。

`ObsidianService` 已有 `searchNotes(keyword)` 方法可搜索知识库笔记，但深度追问功能尚未利用这一能力。

测试文件 `InterviewAiServiceDeepDiveTest.java` 已定义了上下文压缩的完整行为规格（10 个测试用例），涵盖：长上下文保留关键词相关行、保留追问相关内容、长行按句子拆分、长历史截断、单条消息压缩、null/empty 边界、短上下文不变、全低价值 fallback。

## Goals / Non-Goals

**Goals:**
- `buildDeepDivePrompt()` 中的 `contextContent` 超过 4000 字符时，按句子拆分并保留与关键词/追问相关的内容
- 对话历史超过 12 条消息时截断，保留最近的消息
- 单条消息过长时压缩
- 引入 `ContextCompactor` 工具类，可复用于其他场景
- 测试用例全部通过

**Non-Goals:**
- 本次不实现 ReAct Agent 循环和知识库检索集成——上下文压缩是 Agent 的前置依赖，先落地
- 不修改 prompt 模板
- 不修改前端
- 不修改 SSE 端点签名

## Decisions

**1. `ContextCompactor` 作为独立工具类**

放在 `service/ContextCompactor.java`，提供静态方法。不注入为 Spring Bean，因为它是纯文本处理，无依赖。

方法签名：
```java
static String compact(String text, int maxChars, List<String> keywords)
```

**2. 按中文句号/问号/感叹号拆分句子**

用正则 `[。？！\n]` 拆分，保留分隔符。Markdown 标题行（`#` 开头）作为独立句子保留。

**3. 相关性评分策略**

每句得分 = 关键词命中数 + 追问关键词命中数。选取得分最高的句子，直到达到 maxChars。如果所有句子得分都为 0，fallback 到头尾各保留一半。

**4. `buildDeepDivePrompt()` 修改点**

- `contextContent` → 经 `ContextCompactor.compact(contextContent, 4000, mergedKeywords)` 处理
- `mergedKeywords` = `expectedKeywords` + 从最新追问消息中提取的关键词
- `messages` 为 null 时 → 空列表，history 输出空字符串
- `messages` 超过 12 条 → 截取最后 12 条
- 单条消息超过 600 字 → 截断并追加 `[已压缩，原文 N 字]`

**5. 先实现压缩，再考虑 Agent**

测试文件已经定义了压缩行为。先把 `ContextCompactor` 和 `buildDeepDivePrompt()` 的改动做好让测试通过，Agent 层作为后续变更。

## Risks / Trade-offs

- [相关性评分过于简单] → 用关键词精确匹配，对同义词和语义相似性无能为力。对当前面试场景足够，后续可用 embedding 改进。
- [4000 字符硬编码] → 作为 `ContextCompactor` 参数传入，后续可配置化。
- [测试先于实现] → 测试已经写好，实现必须严格匹配测试断言，包括 `"上下文压缩说明"` 和 `"已压缩"` 等特定字符串。
