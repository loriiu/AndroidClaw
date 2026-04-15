package ai.androidclaw.infrastructure.todo.service

import ai.androidclaw.domain.model.todo.TodoIntent
import ai.androidclaw.domain.service.todo.TodoIntentClassifier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodoIntentClassifierImpl @Inject constructor() : TodoIntentClassifier {
    
    private val patterns = mapOf(
        TodoIntent.QUERY_TODOS to listOf("有什么待办", "待办列表", "今天.*待办", "我的.*任务", "还没.*做", "要做.*什么", "待办", "任务"),
        TodoIntent.CREATE_TODO to listOf("创建.*待办", "新增.*待办", "添加.*待办", "新建.*任务", "记一下", "提醒我"),
        TodoIntent.UPDATE_TODO to listOf("更新.*待办", "修改.*待办", "编辑.*待办", "标记.*完成"),
        TodoIntent.DELETE_TODO to listOf("删除.*待办", "移除.*待办", "取消.*待办", "删掉"),
        TodoIntent.CHECK_PROGRESS to listOf("进度", "完成了多少", "进行.*怎样", "还有多少"),
        TodoIntent.SCHEDULE_TODO to listOf("安排", "日程", "计划", "调度", "调整.*时间"),
        TodoIntent.DETECT_CONFLICT to listOf("冲突", "重叠", "时间.*问题", "来得及.*吗"),
        TodoIntent.SUGGEST_OPTIMIZATION to listOf("建议", "优化", "怎么.*做", "顺序", "优先级")
    )
    
    override suspend fun classify(userInput: String): TodoIntent {
        val lowerInput = userInput.lowercase()
        for ((intent, keywords) in patterns) {
            for (keyword in keywords) {
                if (Regex(keyword).containsMatchIn(lowerInput)) return intent
            }
        }
        return TodoIntent.UNKNOWN
    }
    
    override suspend fun extractTodoId(userInput: String): String? {
        val match = Regex("""([\\w-]+)""").find(userInput)
        return match?.groupValues?.getOrNull(1)
    }
    
    override suspend fun extractCreateParams(userInput: String): Map<String, Any>? {
        val params = mutableMapOf<String, Any>()
        val titleMatch = Regex("""(?:创建|新增|添加).*?叫["']?(.*?)["']?(?:的|)待办""").find(userInput)
        titleMatch?.groupValues?.getOrNull(1)?.let { params["title"] = it }
        when {
            userInput.contains("很重要") -> params["importance"] = 5
            userInput.contains("重要") -> params["importance"] = 4
            userInput.contains("不急") -> params["urgency"] = 2
        }
        return params.takeIf { it.isNotEmpty() }
    }
}
