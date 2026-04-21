## Context

当前智谱 AI API Key 通过 `${ZHIPU_API_KEY}` 环境变量注入 `application.yml`，Spring AI 在启动时初始化 `ChatClient` Bean。运行时无法修改 Key，用户必须重启后端才能切换。

前端侧边栏已有"设置"占位入口但未连接真实页面。

## Goals / Non-Goals

**Goals:**
- 前端设置页面可输入 API Key，保存后立即生效，无需重启后端
- Key 以掩码形式显示（仅展示后 4 位），保障安全
- Key 持久化到后端配置文件，重启后仍可用

**Non-Goals:**
- 不做多用户/多 Key 管理，项目无用户体系
- 不做加密存储（单机本地使用场景）
- 不支持配置 LLM 模型、temperature 等其他参数（后续迭代）

## Decisions

### D1: 后端如何持久化 Key

**选择：写入 `application.yml` 文件**

替代方案：
- 写入独立的 `settings.properties` 文件 — 需要额外的配置源加载逻辑
- 写入数据库 — 项目无数据库，引入过重

理由：直接修改 `application.yml` 中的 `api-key` 字段，Spring Boot 的 `YamlPropertiesFactoryBean` 可解析，且与现有配置结构一致。

### D2: 如何让 Key 动态生效

**选择：重新构建 `ChatClient` Bean 并替换**

- 后端 `AiConfig` 暴露 `refreshApiKey(String newKey)` 方法
- 方法内用新 Key 创建 `OpenAiChatModel`，重建 `ChatClient`
- Controller 注入 `AiConfig`，保存 Key 后调用 `refreshApiKey()`
- Controller 中通过 `@Lazy` 或直接在方法参数注入的 `ChatClient` 不会自动更新，所以采用将 `ChatClient` 包装为可刷新的代理对象

替代方案：
- 使用 Spring Cloud Config 的 `@RefreshScope` — 引入额外依赖
- 使用 Environment Post Processor — 过于底层

理由：项目只有一个 `ChatClient` 使用点，直接重建最简单。

### D3: 前端 Key 输入交互

**选择：密码输入框 + 显示/隐藏切换 + 保存按钮**

- 输入框默认 type=password，加载时显示掩码值 `****abcd`
- 用户点击眼睛图标可切换明文/密文
- 保存成功后显示 toast 提示

## Risks / Trade-offs

- **[YAML 文件写入竞态]** → 使用 `synchronized` 或 `ReentrantLock` 保护写入操作
- **[文件路径不可写]** → 启动时检测，不可写时在设置页面提示用户手动配置环境变量
- **[API Key 明文存储在 YAML]** → 单机本地使用场景可接受，后续可按需加密
