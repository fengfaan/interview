## Context

面试助手是一个 Spring Boot + Vue 3 的单用户应用，当前使用 LocalStorage（前端）和 `settings.properties`（后端）做持久化，没有数据库。用户在本地使用 Obsidian 管理 Markdown 笔记，Vault 目录就在用户本机磁盘上。

当前面试流程中，用户获得反馈和推荐答案后，知识点无法持久化保存——关闭页面即丢失。需要一个轻量方案打通 Obsidian Vault。

约束：
- 单用户本地应用，无需考虑多租户或权限
- 后端运行在 localhost，可直接访问用户文件系统
- Obsidian Vault 就是一个包含 `.md` 文件的普通目录，无特殊 API

## Goals / Non-Goals

**Goals:**
- 用户在设置页配置 Obsidian Vault 路径，后端校验路径有效性
- 面试反馈和推荐答案可一键保存为 Obsidian Markdown 笔记
- 在面试助手内浏览和搜索 Obsidian 笔记
- 笔记使用 Obsidian 兼容的 frontmatter 格式，方便在 Obsidian 中管理

**Non-Goals:**
- 不实现双向同步——只在面试助手内创建/读取文件，不监听 Obsidian 侧的文件变化
- 不实现笔记编辑——用户在 Obsidian 中编辑，面试助手只读浏览
- 不实现 Obsidian 插件开发
- 不引入数据库——直接读写文件系统

## Decisions

### 1. 后端直接读写文件系统，不引入数据库

**选择**: 通过 `java.nio.file` API 直接读写 Obsidian Vault 目录下的 Markdown 文件。

**备选方案**:
- 引入嵌入式数据库（H2）存储笔记索引 → 增加复杂度，需要同步机制
- 使用 Obsidian REST API（需安装 Local REST API 插件）→ 增加用户配置负担

**理由**: 应用是单用户本地工具，Vault 就是本地目录，直接文件系统操作最简单可靠。

### 2. Markdown 文件使用 frontmatter 元数据

**选择**: 保存的笔记使用 YAML frontmatter 记录元数据（题目方向、难度、创建时间、标签等）。

```markdown
---
title: "Spring Bean 生命周期"
direction: "backend"
difficulty: "DEEP_PRINCIPLE"
tags: [spring, ioc, bean-lifecycle]
created: "2026-04-23T10:30:00"
source: "interview-assistant"
---
```

**理由**: Obsidian 原生支持 frontmatter，用户可在 Obsidian 中通过属性面板查看和搜索。同时后端解析 frontmatter 也很方便。

### 3. 文件存储结构

**选择**: 在 Vault 根目录下创建 `面试知识库/` 子目录，按方向分类存储：
```
<Vault>/面试知识库/
  ├── 后端/
  ├── 前端/
  └── 系统设计/
```

**理由**: 与用户现有笔记隔离，避免污染 Vault 根目录；分类结构方便浏览。用户可在 Obsidian 中自由移动这些文件。

### 4. 搜索实现

**选择**: 后端逐文件读取并做简单的关键词匹配搜索（文件名 + 内容）。

**备选方案**:
- 集成 Apache Lucene 全文检索 → 过重，单用户场景没必要
- 前端缓存所有笔记内容后前端搜索 → 大量笔记时性能差

**理由**: 笔记数量预期在百级别，逐文件搜索足够。如果未来性能不足，可引入缓存索引。

### 5. 前端路由和 UI 集成

**选择**: 在侧边栏新增「知识库」导航项，对应 `/knowledge` 路由。面试反馈区域新增「保存到知识库」按钮。

**理由**: 知识库是独立功能模块，适合独立路由。保存按钮就近放在反馈区域内，操作路径最短。

## Risks / Trade-offs

- **[文件系统路径安全]** → 后端必须校验路径在合法范围内，防止路径遍历攻击。使用 `Path.normalize()` + `startsWith()` 确保不越界
- **[Vault 路径不存在或不可读]** → 设置保存时校验路径有效性（存在、是目录、可读写），无效路径拒绝保存并给出提示
- **[文件名冲突]** → 同一题可能多次保存，使用时间戳后缀避免文件名冲突，并在 frontmatter 中记录关联的 questionId
- **[大文件或非 Markdown 文件]** → 浏览时只列出 `.md` 文件，读取时限制文件大小（超过 1MB 拒绝读取）
