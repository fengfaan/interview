## MODIFIED Requirements

### Requirement: 选择性导入到知识库
系统 SHALL 允许用户勾选想导入的题目，提供两种保存方式：逐条保存（原有）和 AI 清洗合并保存（新增）。

#### Scenario: 导入选中的题目（逐条模式）
- **WHEN** 用户勾选多道题目并点击"导入到知识库"
- **THEN** 系统逐条将选中题目保存为 Obsidian Markdown 笔记，保存完成后显示成功状态

#### Scenario: 导入单道题目
- **WHEN** 用户点击某道题目的"保存"按钮
- **THEN** 系统将该题目保存为 Obsidian Markdown 笔记

#### Scenario: 导入时标注来源
- **WHEN** 题目从网页导入到知识库
- **THEN** 笔记的 frontmatter 自动记录 source: "web-import"（逐条）或 source: "web-import-consolidated"（合并）和原始 URL

#### Scenario: AI 清洗并合并保存
- **WHEN** 用户勾选多道题目并点击"AI 清洗并合并保存"
- **THEN** 系统先对选中题目执行 AI 清洗（去重、合并、分类），然后将结果写入单个 Markdown 文件
