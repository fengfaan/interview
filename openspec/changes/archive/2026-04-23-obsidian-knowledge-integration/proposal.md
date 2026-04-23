## Why

用户在面试刷题过程中经常遇到记不住的知识点，跳过后难以找回。Obsidian 是用户已有的本地 Markdown 知识库工具。将两者打通，可以让用户把面试中的关键知识点一键保存到 Obsidian，并在面试助手内直接查看和搜索 Obsidian 笔记，形成「刷题 → 记录 → 复习」的闭环。

## What Changes

- 新增 Obsidian Vault 路径配置（在设置页），后端读写指定 Vault 目录下的 Markdown 文件
- 新增「保存到知识库」功能：在面试反馈详情中，一键将知识点（题目、关键词、推荐答案）保存为 Obsidian Markdown 笔记
- 新增知识库浏览与搜索页面：在面试助手内查看 Obsidian 笔记列表、搜索笔记内容、查看笔记详情
- 新增后端 API：Obsidian 文件的 CRUD 操作（列表、读取、搜索、创建）
- 前端新增路由和页面组件用于知识库浏览

## Capabilities

### New Capabilities

- `obsidian-config`: Obsidian Vault 路径配置与管理（设置页集成，后端校验路径有效性）
- `knowledge-save`: 从面试反馈/推荐答案中保存知识点到 Obsidian（一键保存为结构化 Markdown）
- `knowledge-browse`: 知识库浏览与搜索（笔记列表、内容搜索、详情查看，集成到面试助手侧边栏）

### Modified Capabilities

（无已有能力需要修改）

## Impact

- **后端新增**: `ObsidianController`（REST API）、`ObsidianService`（文件系统读写）、设置扩展（vault 路径存储）
- **前端新增**: 知识库浏览页面/组件、面试反馈中的保存按钮、设置页的 Vault 路径配置
- **依赖**: 后端需要文件系统访问权限（读取用户本地 Obsidian Vault 目录），无新增外部依赖
- **API**: 新增 `/api/knowledge/*` 系列端点，设置 API 扩展 vault 路径字段
