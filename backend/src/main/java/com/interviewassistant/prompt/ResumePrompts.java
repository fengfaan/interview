package com.interviewassistant.prompt;

public class ResumePrompts {

    public static final String SYSTEM_PROMPT = """
            你是一位技术招聘视角的简历优化顾问。你帮助求职者优化简历，使其更好地匹配目标职位。

            约束：
            - 只基于用户提供的 JD 和简历内容进行分析
            - 不编造不存在的经历
            - 可以使用占位符引导用户补充真实数据
            - 建议要能直接转化为简历修改动作
            - 使用 STAR 原则（Situation, Task, Action, Result）改写简历描述
            """;

    public static final String ANALYZE_PROMPT = """
            请分析简历与目标职位的匹配度。

            ## 目标职位 JD
            %s

            ## 简历内容
            %s

            请以 JSON 格式返回：
            {
              "score": 72,
              "dimensions": [
                {"name": "维度名称", "score": 90, "reason": "评分理由"}
              ],
              "suggestions": [
                {"id": "s_001", "priority": "HIGH", "title": "建议标题", "reason": "建议理由", "sourceText": "简历中对应的原文"}
              ]
            }

            要求：
            - score: 0-100 的整体匹配度
            - dimensions: 3-5 个核心能力维度评分
            - suggestions: 2-5 条优化建议，priority 为 HIGH/MEDIUM/LOW
            - sourceText 必须是简历中实际存在的文本片段
            - suggestion id 使用 "s_" + 递增数字
            """;

    public static final String REWRITE_PROMPT = """
            请根据以下建议，用 STAR 原则改写简历描述。

            ## 目标职位 JD
            %s

            ## 完整简历
            %s

            ## 需要改写的建议
            - 标题：%s
            - 原始描述：%s

            请按以下 Markdown 格式输出：

            ### 原始描述
            （显示原始描述文本）

            ### STAR 改写范例
            （使用 STAR 框架改写。对于不确定的具体数据，使用 [XX] 格式的占位符，例如：通过 [XX技术] 解决了 [XX问题]，使 [核心指标] 提升 [XX%%]）

            ### 填写建议
            （列出需要用户替换的占位符及建议填入的内容类型）
            """;
}
