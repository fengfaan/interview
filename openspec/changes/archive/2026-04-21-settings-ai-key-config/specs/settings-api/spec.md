## ADDED Requirements

### Requirement: 获取 API Key 掩码
系统 SHALL 提供 `GET /api/settings/apikey` 接口，返回当前 API Key 的掩码值。

#### Scenario: 已配置 Key 时返回掩码
- **WHEN** 后端已配置 API Key（环境变量或配置文件）
- **THEN** 返回 `{ success: true, data: { masked: "****abcd", configured: true } }`

#### Scenario: 未配置 Key 时返回空
- **WHEN** 后端未配置 API Key
- **THEN** 返回 `{ success: true, data: { masked: "", configured: false } }`

### Requirement: 保存 API Key
系统 SHALL 提供 `POST /api/settings/apikey` 接口，接收新 API Key 并持久化。

#### Scenario: 保存有效 Key
- **WHEN** 请求 body 为 `{ apiKey: "valid_key_string" }` 且 Key 非空
- **THEN** 系统将 Key 写入 `application.yml`，刷新 AI 客户端，返回 `{ success: true }`

#### Scenario: 保存空 Key
- **WHEN** 请求 body 的 `apiKey` 为空字符串
- **THEN** 返回 400 错误，提示 "API Key 不能为空"

### Requirement: AI 客户端动态刷新
系统 SHALL 在 API Key 更新后立即重建 ChatClient，无需重启应用。

#### Scenario: 更新 Key 后 AI 功能正常
- **WHEN** 用户通过设置页面保存新 API Key
- **THEN** 后续的面试/简历 AI 接口调用使用新 Key，无需重启服务

### Requirement: 配置持久化
系统 SHALL 将 API Key 写入后端配置文件，应用重启后仍然有效。

#### Scenario: 重启后 Key 仍可用
- **WHEN** 用户保存 Key 后重启后端服务
- **THEN** 服务启动后使用保存的 Key 初始化 AI 客户端，设置页面显示该 Key 的掩码
