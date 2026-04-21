## ADDED Requirements

### Requirement: API Key 配置表单
系统 SHALL 提供一个设置页面，包含 API Key 输入框（密码类型）、显示/隐藏切换按钮和保存按钮。

#### Scenario: 页面加载时显示掩码 Key
- **WHEN** 用户进入设置页面
- **THEN** 系统调用后端获取当前 Key 的掩码值（`****` + 后 4 位），并填入输入框

#### Scenario: 切换 Key 明文显示
- **WHEN** 用户点击输入框旁的显示/隐藏图标
- **THEN** 输入框在 password 和 text 类型之间切换

#### Scenario: 保存 Key 成功
- **WHEN** 用户输入新 Key 并点击保存按钮
- **THEN** 系统调用后端保存接口，成功后显示成功提示，输入框更新为新的掩码值

#### Scenario: 保存 Key 失败
- **WHEN** 后端保存接口返回错误
- **THEN** 页面显示错误提示信息，输入框保留用户输入内容

### Requirement: 未配置 Key 时的提示
系统 SHALL 在后端未配置 API Key 时，在设置页面提示用户配置。

#### Scenario: 首次进入未配置 Key
- **WHEN** 后端返回当前 Key 为空
- **THEN** 输入框显示 placeholder "请输入智谱 AI API Key"，页面显示提示文案

### Requirement: 设置页面路由
系统 SHALL 在 `/settings` 路径提供设置页面，侧边栏"设置"入口导航到该路径。

#### Scenario: 通过侧边栏进入设置
- **WHEN** 用户点击侧边栏"设置"项
- **THEN** 路由跳转到 `/settings`，显示设置页面
