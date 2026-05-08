## ADDED Requirements

### Requirement: AI 清洗整理面试题
系统 SHALL 提供 AI 驱动的题目清洗功能，对解析后的面试题进行去重、合并相似题、统一格式和补充分类标签。

#### Scenario: 成功清洗面试题
- **WHEN** 用户在导入页面点击"AI 清洗并合并保存"，且存在已解析的题目
- **THEN** 系统通过 SSE 流式调用 AI 对整批题目进行去重和整理，返回清洗后的结构化结果，包含分组信息和去重统计

#### Scenario: 清洗去重
- **WHEN** AI 清洗过程中发现语义相似的重复题目
- **THEN** 系统合并这些题目为一条，保留最完整的答案内容，并在结果中标注合并信息

#### Scenario: 清洗分类
- **WHEN** AI 清洗完成
- **THEN** 每道题目被归入一个主题分类（如"基础概念"、"并发编程"），分类信息随题目返回

#### Scenario: 清洗进度反馈
- **WHEN** AI 正在执行清洗
- **THEN** 后端通过 SSE 流式推送进度状态，前端实时展示清洗进度

### Requirement: 清洗结果预览与编辑
系统 SHALL 在清洗完成后展示整理后的题目，用户可以预览和编辑清洗结果。

#### Scenario: 预览清洗结果
- **WHEN** AI 清洗完成
- **THEN** 前端展示清洗后的题目列表，按分类分组显示，标注去重数量和合并的题目

#### Scenario: 编辑清洗后的题目
- **WHEN** 用户对清洗结果不满意
- **THEN** 用户可以编辑题目内容、调整分类、删除某道题，然后再保存

### Requirement: 合并保存到单一知识库文件
系统 SHALL 将清洗后的整批题目写入单个 Markdown 文件，按主题分类组织。

#### Scenario: 合并保存成功
- **WHEN** 用户确认清洗结果后点击保存
- **THEN** 系统将所有题目写入一个 Markdown 文件，保存到 `面试知识库/网页抓题/` 目录，文件按 `<主题标题>-<timestamp>.md` 命名

#### Scenario: 合并文件格式正确
- **WHEN** 系统执行合并保存
- **THEN** 生成的文件包含 YAML frontmatter（title, direction, tags, created, source: web-import-consolidated, url, questionCount），正文按 `## 分类` + `### 题目` 层级组织

#### Scenario: 保存成功反馈
- **WHEN** 合并保存完成
- **THEN** 前端显示成功提示，包含文件路径和题目数量统计
