package ai.androidclaw.ui.screens.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ai.androidclaw.domain.model.Skill
import ai.androidclaw.domain.repository.SkillRepository
import javax.inject.Inject

/**
 * 技能列表 ViewModel
 */
@HiltViewModel
class SkillsViewModel @Inject constructor(
    private val skillRepository: SkillRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SkillsUiState())
    val uiState: StateFlow<SkillsUiState> = _uiState.asStateFlow()
    
    init {
        loadSkills()
    }
    
    private fun loadSkills() {
        viewModelScope.launch {
            skillRepository.getAllSkills().collect { skills ->
                _uiState.value = _uiState.value.copy(skills = skills, isLoading = false)
            }
        }
    }
    
    fun toggleSkillEnabled(skillId: String) {
        viewModelScope.launch {
            val skill = skillRepository.getSkillById(skillId)
            skill?.let {
                if (it.enabled) {
                    skillRepository.disableSkill(skillId)
                } else {
                    skillRepository.enableSkill(skillId)
                }
            }
        }
    }
    
    fun uninstallSkill(skillId: String) {
        viewModelScope.launch {
            skillRepository.uninstallSkill(skillId)
        }
    }
}

/**
 * 技能列表 UI 状态
 */
data class SkillsUiState(
    val skills: List<Skill> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
