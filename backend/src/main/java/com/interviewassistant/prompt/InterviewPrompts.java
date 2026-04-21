package com.interviewassistant.prompt;

public class InterviewPrompts {

    public static final String SYSTEM_PROMPT = """
            你是一位资深技术面试官。你的目标是帮助用户练习技术面试的表达能力，而不仅仅是给出标准答案。

            约束：
            - 问题要贴近真实面试场景
            - 点评要指出命中点、遗漏点和表达问题
            - 示范回答不超过 300 字，使用口语化风格
            - 深度追问必须基于用户刚才的回答内容生成
            - 不要过于严厉，鼓励用户的优点
            """;

    public static final String QUESTION_PROMPT = """
            请生成一道技术面试题。

            方向：%s
            难度：%s
            已有对话历史（请避免重复）：%s

            请以 JSON 格式返回：
            {
              "questionId": "q_<3位数字>",
              "question": "面试题目（一句话）",
              "expectedKeywords": ["关键词1", "关键词2", "关键词3", "关键词4"]
            }

            要求：
            - questionId 使用 "q_" + 递增数字格式
            - question 要具体、有深度
            - expectedKeywords 包含 4-6 个该问题的核心知识点关键词
            """;

    public static final String FEEDBACK_PROMPT = """
            请分析以下面试回答。

            方向：%s
            难度：%s
            面试题目：%s
            用户回答：%s
            期望关键词：%s

            请以 JSON 格式返回：
            {
              "keywordHits": {
                "hit": ["用户回答中包含的关键词"],
                "miss": ["用户回答中遗漏的关键词"]
              },
              "score": 75
            }

            评分规则：
            - 命中所有关键词且表达清晰：80-100
            - 命中大部分关键词：60-79
            - 命中少量关键词：40-59
            - 基本未命中：0-39
            """;

    public static final String FEEDBACK_STREAM_PROMPT = """
            请对以下面试回答给出详细点评。

            方向：%s
            难度：%s
            面试题目：%s
            用户回答：%s
            期望关键词：%s

            请按以下 Markdown 格式输出：

            ## 点评
            （指出用户回答的优缺点，语言简洁有力）

            ## 口语化示范
            （提供一个不超过 300 字的口语化示范回答）

            ## 深度追问
            （基于用户的回答，提出一个相关的进阶问题。格式：先给问题，再简要说明为什么问这个问题）

            追问问题：
            %s
            """;

    public static String difficultyLabel(String level) {
        return switch (level) {
            case "BASIC" -> "基础八股";
            case "DEEP_PRINCIPLE" -> "深度原理";
            case "PROJECT_PRACTICE" -> "项目实战";
            default -> level;
        };
    }

    public static String directionLabel(String direction) {
        return switch (direction) {
            case "GO_BACKEND" -> "Go后端";
            case "REACT_FRONTEND" -> "React前端";
            case "SYSTEM_DESIGN" -> "系统设计";
            default -> direction;
        };
    }
}
