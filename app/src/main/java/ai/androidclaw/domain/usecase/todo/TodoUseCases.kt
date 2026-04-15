package ai.androidclaw.domain.usecase.todo

import ai.androidclaw.domain.model.todo.*
import ai.androidclaw.domain.service.todo.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CreateTodoUseCase @Inject constructor(private val todoService: TodoService, private val conflictEngine: ConflictEngine) {
    suspend operator fun invoke(todo: Todo) = todoService.createTodo(todo)
}

class UpdateTodoUseCase @Inject constructor(private val todoService: TodoService) {
    suspend operator fun invoke(todo: Todo) = todoService.updateTodo(todo)
}

class DeleteTodoUseCase @Inject constructor(private val todoService: TodoService) {
    suspend operator fun invoke(todoId: String) = todoService.deleteTodo(todoId)
}

class GetSortedTodosUseCase @Inject constructor(private val todoService: TodoService) {
    operator fun invoke(): Flow<List<Todo>> = todoService.getSortedTodos()
}

class DetectConflictUseCase @Inject constructor(private val todoService: TodoService) {
    suspend operator fun invoke(todo: Todo) = todoService.detectConflict(todo)
}

class GetTodoContextUseCase @Inject constructor(private val todoService: TodoService) {
    suspend operator fun invoke() = todoService.getTodoContext()
}

class ClassifyIntentUseCase @Inject constructor(private val intentClassifier: TodoIntentClassifier, private val todoService: TodoService) {
    suspend operator fun invoke(userInput: String): IntentResult {
        val intent = intentClassifier.classify(userInput)
        val todoId = intentClassifier.extractTodoId(userInput)
        val createParams = intentClassifier.extractCreateParams(userInput)
        return IntentResult(intent = intent, todoId = todoId, createParams = createParams, todoContext = if (needsContext(intent)) todoService.getTodoContext() else null)
    }
    private fun needsContext(intent: TodoIntent) = intent in listOf(TodoIntent.QUERY_TODOS, TodoIntent.SCHEDULE_TODO, TodoIntent.DETECT_CONFLICT, TodoIntent.SUGGEST_OPTIMIZATION)
}

data class IntentResult(val intent: TodoIntent, val todoId: String?, val createParams: Map<String, Any>?, val todoContext: TodoContext?)

class CompleteTodoUseCase @Inject constructor(private val todoService: TodoService) {
    suspend operator fun invoke(todoId: String, actualDuration: Int? = null) = todoService.markComplete(todoId, actualDuration)
}

class AddSubTaskUseCase @Inject constructor(private val todoService: TodoService) {
    suspend operator fun invoke(todoId: String, title: String, order: Int = 0) = todoService.addSubTask(todoId, SubTask(title = title, order = order))
}

class AddMilestoneUseCase @Inject constructor(private val todoService: TodoService) {
    suspend operator fun invoke(todoId: String, title: String, targetProgress: Int, targetTime: Long? = null) = todoService.addMilestone(todoId, Milestone(title = title, targetProgress = targetProgress, targetTime = targetTime))
}

class AddBlockReasonUseCase @Inject constructor(private val todoService: TodoService) {
    suspend operator fun invoke(todoId: String, reason: String, category: BlockCategory = BlockCategory.UNKNOWN) = todoService.addBlockReason(todoId, BlockReason(reason = reason, category = category))
}
