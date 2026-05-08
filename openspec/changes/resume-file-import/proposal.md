## Why

当前简历调优台只支持手动粘贴文本，用户如果有 PDF 或 Word 格式的简历，需要先手动复制内容再粘贴，体验割裂。对于排版复杂的 PDF 简历，手动复制经常丢失格式、乱序或混入页眉页脚噪声。直接支持文件导入可以大幅降低使用门槛，实现"上传即分析"的一站式流程。

## What Changes

- 新增后端文件上传端点 `POST /api/resume/import-file`，接受 PDF 和 DOCX 格式的 `MultipartFile`，解析后返回纯文本
- 新增 `ResumeFileParser` 服务，封装 PDF 解析（Apache PDFBox）和 DOCX 解析（Apache POI）逻辑，提取简历纯文本并做基础清洗（去除页眉页脚、多余空白）
- 新增前端文件上传 UI：简历输入区新增上传按钮 + 拖拽区域，支持点击选择或拖拽 PDF/DOCX 文件，上传后自动填充到简历文本框
- 后端新增 `spring.servlet.multipart` 配置，限制文件大小（5MB）和请求大小（10MB）
- 后端 `pom.xml` 新增 Apache PDFBox 和 Apache POI 依赖

## Capabilities

### New Capabilities
- `resume-file-import`: 上传 PDF/DOCX 简历文件并提取纯文本，返回给前端填充到简历输入区

### Modified Capabilities
_(无已有 spec 需要修改，现有 analyze / structure-analysis / polish 接口行为不变，文件导入是独立的上传→解析→填充流程)_

## Impact

- **后端**: 新增 1 个 Controller 端点、1 个 FileParser 服务、2 个 Maven 依赖（PDFBox、POI）、multipart 配置
- **前端**: ResumeOptimizerView 简历输入区新增上传组件（按钮+拖拽），resumeApi 新增 `importFile()` 函数
- **API**: 纯新增端点，不影响现有接口
