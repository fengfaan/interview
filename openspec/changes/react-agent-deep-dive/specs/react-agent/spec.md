## ADDED Requirements

### Requirement: ReAct Agent 层架构预留
系统 SHALL 在 AiGateway 之上预留 Agent 接口，当前仅用于深度追问场景。

#### Scenario: Agent 接口存在但不影响现有流程
- **WHEN** 深度追问 SSE 端点被调用
- **THEN** 内部可通过 Agent 层处理，但当前版本直接调用压缩后的 prompt 生成回答

注：ReAct Agent 的完整实现（思考→检索→观察→回答循环）作为后续变更，本次仅预留接口和落地上下文压缩。
