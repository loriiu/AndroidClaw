package ai.androidclaw.domain.model

import java.time.Instant

/**
 * 技能领域模型
 * 
 * 表示一个可加载的技能包，包含工具定义和系统提示
 *
 * @property id 技能唯一标识
 * @property name 技能名称
 * @property description 技能描述
 * @property version 技能版本
 * @property instructions 系统指令
 * @property tools 工具定义列表
 * @property enabled 是否启用
 * @property installedAt 安装时间
 */
data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val instructions: String,
    val tools: List<ToolDefinition>,
    val enabled: Boolean = true,
    val installedAt: Instant = Instant.now()
) {
    companion object {
        /**
         * 从 SKILL.md 内容解析技能
         */
        fun fromSkillMd(
            id: String,
            name: String,
            description: String,
            version: String,
            instructions: String,
            tools: List<ToolDefinition>
        ): Skill {
            return Skill(
                id = id,
                name = name,
                description = description,
                version = version,
                instructions = instructions,
                tools = tools
            )
        }
    }
}

/**
 * 工具定义
 * 
 * 表示技能中定义的一个可调用工具
 */
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, String> = emptyMap()
)
