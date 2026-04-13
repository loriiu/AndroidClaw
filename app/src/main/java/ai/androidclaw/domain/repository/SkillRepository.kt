package ai.androidclaw.domain.repository

import ai.androidclaw.domain.model.Skill
import kotlinx.coroutines.flow.Flow

/**
 * 技能仓储接口
 * 
 * 定义技能的管理操作
 */
interface SkillRepository {
    
    /**
     * 获取所有已安装的技能
     */
    fun getAllSkills(): Flow<List<Skill>>
    
    /**
     * 获取已启用的技能
     */
    fun getEnabledSkills(): Flow<List<Skill>>
    
    /**
     * 获取单个技能
     */
    suspend fun getSkillById(skillId: String): Skill?
    
    /**
     * 安装技能
     */
    suspend fun installSkill(skill: Skill): Skill
    
    /**
     * 卸载技能
     */
    suspend fun uninstallSkill(skillId: String)
    
    /**
     * 更新技能
     */
    suspend fun updateSkill(skill: Skill)
    
    /**
     * 启用技能
     */
    suspend fun enableSkill(skillId: String)
    
    /**
     * 禁用技能
     */
    suspend fun disableSkill(skillId: String)
    
    /**
     * 从文件加载技能（解析 SKILL.md）
     */
    suspend fun loadSkillFromFile(filePath: String): Skill?
}
