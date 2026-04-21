## Why

当前智谱 AI 的 API Key 硬编码在 `application.yml` 中，用户无法通过界面修改。每次更换 Key 都需要手动编辑配置文件并重启后端服务，体验差且不安全。需要一个设置页面让用户在运行时配置 API Key，保存后自动生效。

## What Changes

- 新增前端「设置」页面（侧边栏入口），提供 API Key 输入表单
- 新增后端 `/api/settings` 接口，支持读取和保存 API Key 到配置文件
- 后端动态刷新 AI 客户端配置，使新 Key 无需重启即可生效
- API Key 在前端和后端均做脱敏显示（仅展示后 4 位）
- 隐藏当前侧边栏的"设置"占位入口，接入真实页面

## Capabilities

### New Capabilities
- `settings-page`: 前端设置页面，包含 API Key 配置表单、保存状态反馈、脱敏显示
- `settings-api`: 后端设置管理接口，支持 Key 的读写、动态刷新 AI 客户端、配置持久化

### Modified Capabilities
- `app-shell`: 侧边栏"设置"入口从占位改为路由到真实设置页面

## Impact

- 前端：新增 SettingsView 页面、settingsStore、settingsApi、路由配置
- 后端：新增 SettingsController、SettingsService，修改 AiConfig 支持动态刷新
- 配置：`application.yml` 中 `api-key` 改为可被运行时覆盖的动态值
- 依赖：无新依赖引入
