## 1. ContextCompactor 工具类（内联实现于 InterviewAiService）

- [x] 1.1 实现 `compactDeepDiveContext()` 方法：按句子拆分，按关键词/追问术语评分，选取得分最高句子，fallback 到头尾保留
- [x] 1.2 实现 fallback 头尾保留策略：`buildFallbackCompactContext()` 保留原文前半和后半，中间标注"省略"
- [x] 1.3 测试覆盖于 `InterviewAiServiceDeepDiveTest.java`（17 个测试全部通过）

## 2. buildDeepDivePrompt 集成压缩

- [x] 2.1 contextContent 超过 4000 字符时触发压缩
- [x] 2.2 从最新追问消息提取关键词（`extractQuestionTerms`），合并到 expectedKeywords 用于压缩评分
- [x] 2.3 messages null → 空列表处理
- [x] 2.4 对话历史超过 12 条时截断到最近 12 条
- [x] 2.5 单条消息超过 1200 字符时截断并追加压缩标记
- [x] 2.6 `InterviewAiServiceDeepDiveTest.java` 全部 17 个测试通过

## 3. 验证与提交

- [x] 3.1 后端全部 26 个测试通过，无回归
- [x] 3.2 前端 build 通过
- [x] 3.3 已提交

## 4. DeepDiveAgent ReAct 循环

- [x] 4.1 创建 `AgentDecision` DTO（action, keyword, reason）
- [x] 4.2 创建 `deep-dive-agent-decide.md` 决策 prompt 模板
- [x] 4.3 实现 `DeepDiveAgent` 服务（decide → search → stream），最大 1 次工具调用
- [x] 4.4 添加 `SseUtils.sendAgentStep()` 支持 `agent_step` 事件
- [x] 4.5 将 `InterviewStreamService.streamDeepDive()` 切换到走 Agent 层
- [x] 4.6 前端处理 `agent_step` SSE 事件并在 UI 显示检索信息
- [x] 4.7 后端全部 36 个测试通过，前端 build 通过
