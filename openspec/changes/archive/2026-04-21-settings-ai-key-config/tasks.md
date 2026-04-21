## 1. 后端 — 设置接口与动态刷新

- [x] 1.1 创建 `SettingsController`，实现 `GET /api/settings/apikey`（返回掩码 Key）和 `POST /api/settings/apikey`（保存新 Key）
- [x] 1.2 创建 `SettingsService`，实现 Key 掩码生成、写入 `settings.properties`、读取当前 Key
- [x] 1.3 修改 `AiConfig`，添加 `refreshApiKey(String newKey)` 方法，重建 `ChatClient` Bean
- [x] 1.4 在 `SettingsService` 保存后调用 `AiConfig.refreshApiKey()` 实现动态刷新
- [x] 1.5 添加请求 DTO（`ApiKeyRequest`）和响应 DTO（`ApiKeyResponse`），包含参数校验

## 2. 前端 — 设置页面

- [x] 2.1 创建 `frontend/src/types/settings.ts`，定义请求/响应类型
- [x] 2.2 创建 `frontend/src/api/settingsApi.ts`，封装 `getApiKeyMasked()` 和 `saveApiKey()` 接口调用
- [x] 2.3 创建 `frontend/src/stores/settingsStore.ts`，管理 Key 掩码、加载状态、错误信息
- [x] 2.4 创建 `frontend/src/views/SettingsView.vue`，包含 Key 输入框（密码/明文切换）、保存按钮、状态提示

## 3. 前后端联调

- [x] 3.1 在 `router/index.ts` 注册 `/settings` 路由，指向 `SettingsView`
- [x] 3.2 修改 `AppSidebar.vue`，将"设置"入口连接到 `/settings` 路由
- [x] 3.3 验证完整流程：TypeScript 类型检查通过，Java 无编译错误，application.yml 移除硬编码 Key
