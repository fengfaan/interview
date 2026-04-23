## ADDED Requirements

### Requirement: 知识库导航入口
系统 SHALL 在侧边栏新增「知识库」导航项，链接到 `/knowledge` 路由。

#### Scenario: 侧边栏显示知识库入口
- **WHEN** 用户查看应用侧边栏
- **THEN** 侧边栏包含「知识库」导航项，位于「设置」之前，点击跳转到 `/knowledge`

### Requirement: 笔记列表浏览
系统 SHALL 在知识库页面展示 Obsidian Vault `面试知识库/` 目录下的所有 Markdown 笔记列表，按创建时间倒序排列。

#### Scenario: 加载笔记列表
- **WHEN** 用户进入知识库页面且 Vault 已配置
- **THEN** 系统展示笔记列表，每条笔记显示标题、方向标签、创建时间

#### Scenario: Vault 未配置时的引导
- **WHEN** 用户进入知识库页面但 Vault 未配置
- **THEN** 页面显示引导信息"请先在设置页配置 Obsidian Vault 路径"，并提供跳转设置页的链接

#### Scenario: 笔记列表为空
- **WHEN** 用户进入知识库页面且 Vault 已配置但无笔记
- **THEN** 页面显示"暂无笔记，在面试练习中保存知识点吧"

### Requirement: 笔记分类筛选
系统 SHALL 支持按方向（后端/前端/系统设计）筛选笔记列表。

#### Scenario: 按方向筛选
- **WHEN** 用户选择某个方向标签（如"后端"）
- **THEN** 笔记列表只显示该方向下的笔记

#### Scenario: 查看全部
- **WHEN** 用户选择"全部"标签
- **THEN** 笔记列表显示所有方向的笔记

### Requirement: 笔记内容查看
系统 SHALL 支持点击笔记列表中的条目，在页面内展示笔记的完整 Markdown 内容（渲染为 HTML）。

#### Scenario: 查看笔记详情
- **WHEN** 用户点击笔记列表中的某条笔记
- **THEN** 系统在右侧或下方展示该笔记的完整内容，Markdown 渲染为 HTML，支持代码高亮

### Requirement: 笔记搜索
系统 SHALL 提供关键词搜索功能，在笔记的文件名和内容中搜索。

#### Scenario: 搜索笔记
- **WHEN** 用户在搜索框中输入关键词并提交
- **THEN** 系统返回文件名或内容中包含该关键词的笔记列表，高亮显示匹配内容

#### Scenario: 搜索无结果
- **WHEN** 用户搜索的关键词未匹配到任何笔记
- **THEN** 显示"未找到匹配的笔记"
