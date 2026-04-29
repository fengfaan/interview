## ADDED Requirements

### Requirement: ReAct Agent 深度追问
系统 SHALL 在深度追问流程中实现 ReAct Agent 循环（最多 1 次工具调用）：
1. Agent 判断是否需要检索知识库（返回结构化 JSON：action=search_notes 或 answer_directly）
2. 若 search_notes：调用 ObsidianService.searchNotes(keyword)，通过 SSE agent_step 事件通知前端检索了哪些笔记
3. 将检索结果注入 prompt 后流式输出最终回答

#### Scenario: Agent 判断需要检索知识库
- **GIVEN** 候选人追问涉及上下文外的知识点
- **WHEN** Agent 决策返回 action=search_notes
- **THEN** 系统搜索 Obsidian 知识库，发送 agent_step SSE 事件，将结果注入最终回答

#### Scenario: Agent 判断直接回答
- **GIVEN** 候选人追问可基于当前上下文充分回答
- **WHEN** Agent 决策返回 action=answer_directly
- **THEN** 系统直接流式输出回答，不触发知识库检索

#### Scenario: 决策失败时优雅降级
- **GIVEN** Agent 决策调用失败（JSON 解析异常、LLM 错误）
- **WHEN** decide() 方法捕获异常
- **THEN** 系统降级为 answer_directly，不中断用户流程

#### Scenario: 知识库未配置时跳过检索
- **GIVEN** Obsidian Vault 未配置
- **WHEN** Agent 决策返回 search_notes
- **THEN** 系统跳过检索，直接基于原始 prompt 流式输出回答

#### Scenario: 前端展示检索步骤
- **GIVEN** 后端发送 agent_step SSE 事件
- **WHEN** 前端收到事件
- **THEN** 在深度追问抽屉中展示蓝色信息栏，显示检索关键词和命中的笔记标题
