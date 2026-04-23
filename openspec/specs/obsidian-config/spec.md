## ADDED Requirements

### Requirement: Vault 路径配置
系统 SHALL 在设置页面提供 Obsidian Vault 路径配置项，允许用户输入本地 Vault 目录的绝对路径。

#### Scenario: 成功配置 Vault 路径
- **WHEN** 用户在设置页输入一个有效的本地目录路径并点击保存
- **THEN** 后端校验该路径存在、是目录、且有读写权限，保存到 settings 配置中，返回成功提示

#### Scenario: 配置无效路径
- **WHEN** 用户输入的路径不存在或不是目录
- **THEN** 后端返回错误提示"路径不存在或不是有效目录"，不保存配置

#### Scenario: 路径无读写权限
- **WHEN** 用户输入的路径存在但后端无读写权限
- **THEN** 后端返回错误提示"路径无读写权限"，不保存配置

### Requirement: Vault 路径持久化
系统 SHALL 将 Vault 路径持久化存储，与现有 API Key 等设置使用相同的 settings.properties 文件。

#### Scenario: 配置在重启后保留
- **WHEN** 用户配置了 Vault 路径后重启后端服务
- **THEN** Vault 路径配置依然存在，无需重新配置

### Requirement: Vault 路径状态查询
系统 SHALL 提供 API 查询当前 Vault 配置状态（是否已配置、路径值）。

#### Scenario: 查询已配置的 Vault
- **WHEN** 前端请求 Vault 配置状态且路径已配置
- **THEN** 返回 `{configured: true, path: "/Users/xxx/vault"}`

#### Scenario: 查询未配置的 Vault
- **WHEN** 前端请求 Vault 配置状态且路径未配置
- **THEN** 返回 `{configured: false, path: null}`
