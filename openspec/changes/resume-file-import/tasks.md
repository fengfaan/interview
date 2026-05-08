## 1. 后端依赖和配置

- [x] 1.1 在 `backend/pom.xml` 中添加 Apache PDFBox 依赖（`org.apache.pdfbox:pdfbox`）和 Apache POI 依赖（`org.apache.poi:poi-ooxml`）
- [x] 1.2 在 `backend/src/main/resources/application.yml` 中添加 `spring.servlet.multipart.max-file-size: 5MB` 和 `max-request-size: 10MB` 配置

## 2. 后端 DTO

- [x] 2.1 创建 `ImportFileResponse` DTO（text, fileName, pageCount, warning 可选字段）

## 3. 后端文件解析服务

- [x] 3.1 创建 `ResumeFileParser` 服务类，实现 `parsePdf(byte[])` 方法：使用 PDFBox 提取文本，按页拼接，返回原始文本
- [x] 3.2 在 `ResumeFileParser` 中实现 `parseDocx(byte[])` 方法：使用 Apache POI XWPF 提取段落和表格文本，保留段落结构
- [x] 3.3 在 `ResumeFileParser` 中实现 `cleanText(String)` 方法：合并连续空行（>2→2）、移除页码模式行（"第 X 页"、"Page X of Y"、独立数字行）、trim 首尾空白
- [x] 3.4 在 `ResumeFileParser` 中实现 `parse(byte[], String fileName)` 统一入口：根据文件扩展名分发到 PDF/DOCX 解析器，对不支持的格式抛出 IllegalArgumentException

## 4. 后端 Controller 层

- [x] 4.1 在 `ResumeController` 中新增 `POST /api/resume/import-file` 端点，接收 `@RequestParam MultipartFile file`，调用 `ResumeFileParser.parse`，返回 `ApiResponse<ImportFileResponse>`
- [x] 4.2 在端点中添加文件扩展名校验（仅允许 .pdf 和 .docx），不匹配时返回 400 错误
- [x] 4.3 在端点中添加空文件和空文本检测：解析后文本为空时在 response 中包含 warning 字段提示可能为扫描件

## 5. 前端 API

- [x] 5.1 在 `frontend/src/api/resumeApi.ts` 中新增 `importFile(file: File)` 函数，构造 `FormData` 发送 `multipart/form-data` 请求到 `/api/resume/import-file`

## 6. 前端视图

- [x] 6.1 在 `ResumeOptimizerView.vue` 简历输入区（textarea 上方或旁边）添加文件上传区域：支持点击选择文件和拖拽上传，仅接受 `.pdf` 和 `.docx` 文件
- [x] 6.2 实现上传交互逻辑：上传成功后自动填充 `store.resume` 为解析出的文本；上传中显示 loading 状态；上传失败显示错误提示

## 7. 编译验证

- [x] 7.1 后端编译通过（`./mvnw compile`）
- [x] 7.2 前端构建通过（`npm run build`）
