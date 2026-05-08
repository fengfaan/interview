## Context

当前简历调优台（ResumeOptimizerView）仅支持手动粘贴文本到 textarea。后端所有接口（analyze、structure-analysis、polish/stream、rewrite/stream）都接收 String 类型的简历文本，通过 JSON body 传递。项目目前没有任何文件上传基础设施（无 MultipartFile 处理、无 FormData 前端代码、无 multipart 配置）。

后端使用 Spring Boot 3.3 + Maven，前端使用 Vue 3 + TypeScript + Vite。项目面向本地单用户部署，无需考虑并发文件上传、存储持久化或文件管理。

## Goals / Non-Goals

**Goals:**
- 支持 PDF 和 DOCX 两种简历格式的文件导入，解析后提取纯文本
- 前端提供拖拽上传 + 点击上传两种交互方式，解析结果自动填充到简历 textarea
- 文件解析在服务端完成（PDF/DOCX 解析库成熟稳定）
- 上传→解析→填充流程对现有分析/润色接口透明，不改变任何现有 API 行为

**Non-Goals:**
- 不支持其他格式（TXT、RTF、HTML 等，用户可以直接粘贴）
- 不做 OCR（扫描版 PDF 不支持，需要文字版 PDF）
- 不做文件持久化存储（解析后丢弃文件，只返回文本）
- 不做批量文件上传
- 不修改现有 analyze / structure-analysis / polish 等接口的行为

## Decisions

### 1. 服务端解析而非前端解析

**选择**: 后端接收 MultipartFile，用 Java 库解析 PDF/DOCX

**理由**: PDFBox 和 POI 是成熟的 Java 库，解析质量高且稳定。前端 PDF.js 对中文简历支持一般，DOCX 前端解析方案不成熟。服务端解析还可以做统一的文本清洗（去页眉页脚、多余空白）。

**替代方案**: 前端 PDF.js + docx.js — 中文兼容性差，增加前端 bundle 大小。

### 2. 解析结果返回纯文本而非结构化数据

**选择**: 端点返回 `{ text: string, fileName: string, pageCount: number }`，前端拿到 text 直接填入 textarea

**理由**: 现有所有分析接口都接收纯文本。返回结构化数据（按段落/模块拆分）增加复杂度但没有消费者。用户可以在 textarea 中查看和编辑解析结果后再提交分析。

**替代方案**: 返回分段结构化 JSON — 现有接口不支持，过度设计。

### 3. 文件大小限制 5MB

**选择**: 单文件最大 5MB，总请求最大 10MB

**理由**: 普通简历 PDF/DOCX 通常在 100KB-2MB 之间。5MB 上限足够覆盖含图片的简历，同时防止滥用。Spring Boot 默认 1MB 太小需要调大。

### 4. 文本清洗策略：基础清洗

**选择**: 去除连续空行（>2 行合并为 2 行）、trim 首尾空白、去除常见页眉页脚模式（如"第 X 页"）

**理由**: 过度清洗可能丢失用户简历中的有意义内容。基础清洗足够让后续 AI 分析更准确。用户可以在 textarea 中进一步编辑。

### 5. 同步接口而非流式

**选择**: `POST /api/resume/import-file` 返回同步 JSON

**理由**: 文件解析速度快（通常 <1 秒），不需要流式。与文件上传的标准模式一致。

## Risks / Trade-offs

- **扫描版 PDF 无法提取文本** → 端点返回明确错误提示"该 PDF 为扫描件，暂不支持文字提取，请直接粘贴简历内容"
- **复杂排版 PDF 提取文本乱序** → PDFBox 按页顺序提取，对于常见的简历排版（单栏、少量双栏）效果可接受。极端排版场景用户可在 textarea 中手动调整
- **依赖体积增加** → PDFBox ~3MB + POI ~15MB。对于本地部署项目可接受
