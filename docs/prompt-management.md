# 提示词管理说明

后端提示词已经从 Java 代码中提取到外部文件，默认目录为：

```text
backend/prompts/
```

## 目录结构

```text
backend/prompts/
  interview/
    system.md
    question.md
    feedback-json.md
    feedback-stream.md
  resume/
    system.md
    analyze.md
    rewrite.md
```

## 动态修改方式

服务运行时会在每次调用 AI 前重新读取对应提示词文件，不做内存缓存。

因此开发阶段可以直接修改 `backend/prompts/` 下的 `.md` 文件，下一次请求就会使用新提示词，不需要重新编译 Java。

## 自定义提示词目录

默认读取相对后端启动目录的 `prompts` 文件夹。为了兼容从仓库根目录启动后端的情况，如果当前目录下不存在 `prompts`，但存在 `backend/prompts`，系统会自动使用 `backend/prompts`。

如果需要把提示词放到其他目录，可以设置环境变量：

```bash
PROMPT_DIR=/absolute/path/to/prompts
```

也可以在配置中修改：

```yaml
app:
  prompts:
    directory: ${PROMPT_DIR:prompts}
```

## 占位符规则

提示词模板使用 `{{变量名}}` 作为占位符，后端会在调用前替换。

示例：

```text
方向：{{direction}}
题型：{{questionType}}
用户回答：{{answer}}
```

当前支持的变量：

- 面试题生成：`direction`、`questionType`、`history`
- 面试点评：`direction`、`questionType`、`question`、`answer`、`expectedKeywords`
- 面试流式点评：`direction`、`questionType`、`question`、`answer`、`expectedKeywords`、`followUpQuestion`
- 简历分析：`jobDescription`、`resume`
- STAR 改写：`jobDescription`、`resume`、`suggestionTitle`、`sourceText`

## 注意事项

- 不要删除 JSON 接口提示词中的返回格式要求，否则后端结构化解析可能失败。
- 修改评分规则后，建议用明显低质量输入和正常输入各测一次，确认评分符合预期。
- 生产环境建议把提示词目录放到应用外部，并通过 `PROMPT_DIR` 指向该目录，避免发布新包时覆盖线上调优内容。
