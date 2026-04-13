package ai.androidclaw.data.repository

import ai.androidclaw.data.local.db.EntityMapper.toDomain
import ai.androidclaw.data.local.db.EntityMapper.toEntity
import ai.androidclaw.data.local.db.SkillDao
import ai.androidclaw.domain.model.Skill
import ai.androidclaw.domain.repository.SkillRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 技能仓储实现
 */
@Singleton
class SkillRepositoryImpl @Inject constructor(
    private val skillDao: SkillDao
) : SkillRepository {
    
    override fun getAllSkills(): Flow<List<Skill>> {
        return skillDao.getAllSkills().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getEnabledSkills(): Flow<List<Skill>> {
        return skillDao.getEnabledSkills().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getSkillById(skillId: String): Skill? {
        return skillDao.getSkillById(skillId)?.toDomain()
    }
    
    override suspend fun installSkill(skill: Skill): Skill {
        val skillToSave = skill.copy(installedAt = Instant.now())
        skillDao.insertSkill(skillToSave.toEntity())
        return skillToSave
    }
    
    override suspend fun uninstallSkill(skillId: String) {
        skillDao.deleteSkill(skillId)
    }
    
    override suspend fun updateSkill(skill: Skill) {
        skillDao.updateSkill(skill.toEntity())
    }
    
    override suspend fun enableSkill(skillId: String) {
        skillDao.enableSkill(skillId)
    }
    
    override suspend fun disableSkill(skillId: String) {
        skillDao.disableSkill(skillId)
    }
    
    override suspend fun loadSkillFromFile(filePath: String): Skill? {
        // TODO: 实现从文件加载技能（解析 SKILL.md）
        // 1. 读取文件内容
        // 2. 解析 Markdown
        // 3. 提取 name, description, instructions, tools
        // 4. 构建 Skill 对象
        return null
    }
}
