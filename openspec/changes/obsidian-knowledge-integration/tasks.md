## 1. 后端 - Obsidian 配置管理

- [ ] 1.1 扩展 `SettingsService`，添加 Vault 路径的读取和保存方法，持久化到 `settings.properties`
- [ ] 1.2 在 `SettingsController` 中添加 Vault 路径的 GET/POST 端点（`/api/settings/vault`），包含路径有效性校验（存在、是目录、可读写）
- [ ] 1.3 编写 Vault 路径校验的单元测试

## 2. 后端 - Obsidian 文件服务

- [ ] 2.1 创建 `ObsidianService`，实现笔记列表获取（扫描 `面试知识库/` 目录下的 `.md` 文件，解析 frontmatter 获取元数据）
- [ ] 2.2 实现笔记内容读取（读取指定 `.md` 文件，路径安全校验防止路径遍历）
- [ ] 2.3 实现笔记创建（根据方向创建子目录，生成带 frontmatter 的 Markdown 文件，文件名用标题+时间戳保证唯一）
- [ ] 2.4 实现笔记搜索（遍历文件，匹配文件名和内容中的关键词）
- [ ] 2.5 创建 `KnowledgeController`（`/api/knowledge/*`），提供列表、详情、创建、搜索的 REST 端点
- [ ] 2.6 添加 Vault 未配置时的错误处理（返回友好错误信息）

## 3. 前端 - 设置页 Vault 配置

- [ ] 3.1 在 `settingsApi.ts` 中添加 Vault 路径的查询和保存 API 调用
- [ ] 3.2 在 `settingsStore.ts` 中添加 Vault 配置状态管理
- [ ] 3.3 在 `SettingsView.vue` 中添加 Vault 路径配置 UI（输入框 + 保存按钮 + 状态显示）
- [ ] 3.4 添加 TypeScript 类型定义（`types/settings.ts` 扩展 Vault 相关类型）

## 4. 前端 - 知识库浏览页面

- [ ] 4.1 创建 `types/knowledge.ts`，定义笔记列表项、笔记详情、搜索结果等类型
- [ ] 4.2 创建 `api/knowledgeApi.ts`，封装知识库相关的 API 调用
- [ ] 4.3 创建 `stores/knowledgeStore.ts`，管理笔记列表、当前笔记、搜索状态
- [ ] 4.4 创建 `views/KnowledgeBaseView.vue` 知识库页面（笔记列表 + 分类筛选 + 搜索框 + 笔记详情展示）
- [ ] 4.5 在 `router/index.ts` 中添加 `/knowledge` 路由
- [ ] 4.6 在 `components/AppSidebar.vue` 中添加「知识库」导航项

## 5. 前端 - 面试页面保存按钮

- [ ] 5.1 在 `MockInterviewRoomView.vue` 的反馈区域添加「保存到知识库」按钮
- [ ] 5.2 在推荐答案区域添加「保存到知识库」按钮
- [ ] 5.3 实现保存逻辑：调用知识库创建 API，传递题目、方向、难度、反馈内容/推荐答案
- [ ] 5.4 添加保存成功/失败的状态反馈（按钮状态变化 + toast 提示）
