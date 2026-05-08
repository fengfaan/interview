## ADDED Requirements

### Requirement: URL 输入与网页抓取
系统 SHALL 提供一个 API 端点，接收用户输入的 URL，使用 Playwright 打开网页并提取页面文本内容。

#### Scenario: 成功抓取普通网页
- **WHEN** 用户提交一个有效的 HTTP/HTTPS URL
- **THEN** 系统使用 Playwright 打开页面，等待页面加载完成，提取主要文本内容并返回

#### Scenario: URL 格式无效
- **WHEN** 用户提交的 URL 格式不合法（非 http/https）
- **THEN** 系统返回错误提示"URL 格式无效，请输入 http:// 或 https:// 开头的地址"

#### Scenario: 网页加载超时
- **WHEN** 目标网页在 30 秒内未能加载完成
- **THEN** 系统返回错误提示"网页加载超时，请检查 URL 或网络连接"

#### Scenario: 网页无法访问
- **WHEN** 目标网页返回 4xx/5xx 状态码或网络不可达
- **THEN** 系统返回错误提示，包含具体的失败原因

### Requirement: 智能文本提取
系统 SHALL 从网页 DOM 中优先提取文章主体内容，过滤导航栏、广告、侧边栏等无关内容。

#### Scenario: 提取文章区域内容
- **WHEN** 页面包含 `<article>`、`<main>` 或常见内容 CSS 选择器
- **THEN** 系统优先从这些元素中提取文本

#### Scenario: 无明确文章区域
- **WHEN** 页面没有语义化的文章区域标签
- **THEN** 系统回退到 `<body>` 全文提取

#### Scenario: 内容过长截断
- **WHEN** 提取的文本超过 4000 字符
- **THEN** 系统截断到 4000 字符并标记已截断

### Requirement: 抓取结果返回
系统 SHALL 返回抓取的结构化结果，包含页面标题、提取的文本内容、来源 URL 和抓取时间。

#### Scenario: 成功返回抓取结果
- **WHEN** 网页抓取和文本提取完成
- **THEN** 返回 JSON 对象，包含 title、content、url、capturedAt 字段
