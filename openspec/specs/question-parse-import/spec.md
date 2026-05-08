## ADDED Requirements

### Requirement: AI 解析网页内容为面试题
系统 SHALL 将抓取的网页文本内容发送给 AI，解析为结构化面试题列表（题目、参考答案、关键词）。

#### Scenario: 成功解析面试题
- **WHEN** 用户触发解析，且网页内容包含可识别的面试题
- **THEN** AI 返回结构化 JSON 数组，每项包含 question、answer、keywords 字段

#### Scenario: 网页内容不包含面试题
- **WHEN** 网页内容与面试题无关（如纯新闻、广告页面）
- **THEN** AI 返回空数组，前端显示"未识别到面试题"提示

#### Scenario: 解析结果流式输出
- **WHEN** AI 正在解析网页内容
- **THEN** 后端通过 SSE 流式推送解析进度，前端实时显示

### Requirement: 解析结果预览与编辑
系统 SHALL 在前端展示解析后的面试题列表，用户可以编辑每道题的内容和标签。

#### Scenario: 查看解析结果
- **WHEN** AI 解析完成
- **THEN** 前端以卡片列表展示所有解析出的题目，每张卡片显示题目、答案和关键词标签

#### Scenario: 编辑单道题目
- **WHEN** 用户点击某道题目的编辑按钮
- **THEN** 题目文本、答案和关键词变为可编辑状态，用户修改后可保存

#### Scenario: 删除不需要的题目
- **WHEN** 用户点击某道题目的删除按钮
- **THEN** 该题目从导入列表中移除

### Requirement: 选择性导入到知识库
系统 SHALL 允许用户勾选想导入的题目，一键保存到 Obsidian 知识库。

#### Scenario: 导入选中的题目
- **WHEN** 用户勾选多道题目并点击"导入到知识库"
- **THEN** 系统逐条将选中题目保存为 Obsidian Markdown 笔记，保存完成后显示成功状态

#### Scenario: 导入单道题目
- **WHEN** 用户点击某道题目的"保存"按钮
- **THEN** 系统将该题目保存为 Obsidian Markdown 笔记

#### Scenario: 导入时标注来源
- **WHEN** 题目从网页导入到知识库
- **THEN** 笔记的 frontmatter 自动记录 source: "web-import" 和原始 URL

### Requirement: 方向和级别标注
系统 SHALL 允许用户为整批导入的题目设置技术方向和难度级别。

#### Scenario: 设置方向和级别
- **WHEN** 用户在导入页面选择方向（如 Go 后端）和级别（如基础八股）
- **THEN** 所有导入的题目自动关联该方向和级别标签
