## 1. ContextCompactor 工具类

- [ ] 1.1 创建 `ContextCompactor.java`，实现 `compact(String text, int maxChars, List<String> keywords)` 方法：按 `[。？！\n]` 拆分句子，按关键词命中数评分，选取得分最高句子直到 maxChars，无相关句子时 fallback 到头尾各保留一半
- [ ] 1.2 实现 fallback 头尾保留策略：所有句子得分为 0 时，保留前半部分标记"原文前半"和后半部分标记"原文后半"，中间标注"省略"
- [ ] 1.3 编写 `ContextCompactorTest.java` 单元测试覆盖：短文本不变、长文本关键词保留、无关键词 fallback、null/empty 输入

## 2. buildDeepDivePrompt 集成压缩

- [ ] 2.1 修改 `InterviewAiService.buildDeepDivePrompt()`：contextContent 超过 4000 字符时调用 `ContextCompactor.compact()`
- [ ] 2.2 实现从最新追问消息提取关键词的逻辑，合并到 expectedKeywords 用于压缩评分
- [ ] 2.3 实现 messages null → 空列表处理
- [ ] 2.4 实现对话历史超过 12 条时截断到最近 12 条
- [ ] 2.5 实现单条消息超过 600 字符时截断并追加 `[已压缩，原文 N 字]` 标记
- [ ] 2.6 验证 `InterviewAiServiceDeepDiveTest.java` 全部 15 个测试通过

## 3. 验证与提交

- [ ] 3.1 运行后端全部测试确认无回归
- [ ] 3.2 前端 build 确认无影响
- [ ] 3.3 提交并推送
