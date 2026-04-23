## ADDED Requirements

### Requirement: 从面试反馈保存知识点
系统 SHALL 在面试反馈区域提供「保存到知识库」按钮，允许用户将当前题目、关键词命中情况、AI 评语保存为 Obsidian Markdown 笔记。

#### Scenario: 保存面试反馈到知识库
- **WHEN** 用户在面试反馈页面点击「保存到知识库」按钮，且 Vault 已配置
- **THEN** 系统将题目、方向、难度、关键词命中情况、AI 评语整理为 Markdown 文件，保存到 Vault 的 `面试知识库/<方向>/` 目录下，文件名使用题目标题 + 时间戳，包含 frontmatter 元数据

#### Scenario: Vault 未配置时保存
- **WHEN** 用户点击「保存到知识库」但 Vault 路径未配置
- **THEN** 系统提示用户先在设置页配置 Obsidian Vault 路径，不执行保存

### Requirement: 从推荐答案保存知识点
系统 SHALL 在推荐答案区域提供「保存到知识库」按钮，允许用户将题目和推荐答案保存为笔记。

#### Scenario: 保存推荐答案到知识库
- **WHEN** 用户在推荐答案展示区域点击「保存到知识库」
- **THEN** 系统将题目和推荐答案内容保存为 Markdown 笔记，frontmatter 中标记 `source: recommended-answer`

### Requirement: 保存文件格式规范
系统 SHALL 使用 Obsidian 兼容的 Markdown 格式保存笔记，包含 YAML frontmatter。

#### Scenario: 生成的文件格式正确
- **WHEN** 系统保存一条面试知识点
- **THEN** 文件包含 YAML frontmatter（title, direction, difficulty, tags, created, source, questionId），正文包含题目、反馈/答案内容，文件扩展名为 `.md`

#### Scenario: 文件名唯一
- **WHEN** 同一题目被多次保存
- **THEN** 系统使用题目标题 + 时间戳确保文件名不冲突，不覆盖已有文件

### Requirement: 保存成功反馈
系统 SHALL 在保存成功后给出明确提示。

#### Scenario: 保存成功提示
- **WHEN** 笔记成功保存到 Vault
- **THEN** 前端显示"已保存到知识库"的成功提示，按钮变为已保存状态
