package com.semo.alarm.ui.viewmodels

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.semo.alarm.data.entities.TimerTemplate
import com.semo.alarm.data.entities.TimerCategory
import com.semo.alarm.data.entities.TimerRound
import com.semo.alarm.data.repositories.TimerRepository
import com.semo.alarm.utils.NotificationAlarmManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomTimerViewModel @Inject constructor(
    application: Application,
    private val timerRepository: TimerRepository
) : AndroidViewModel(application) {
    
    private val _templates = MutableLiveData<List<TimerTemplate>>()
    val templates: LiveData<List<TimerTemplate>> get() = _templates
    
    private val _categories = MutableLiveData<List<TimerCategory>>()
    val categories: LiveData<List<TimerCategory>> get() = _categories
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error
    
    // Timer management
    private val activeTimers = mutableMapOf<Int, CountDownTimer>()
    private val timerStates = mutableMapOf<Int, TimerState>()
    private val notificationAlarmManager = NotificationAlarmManager(getApplication())
    
    data class TimerState(
        val templateId: Int,
        val remainingSeconds: Int,
        val isRunning: Boolean
    )
    
    fun loadTemplatesByCategory(categoryId: Int) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                
                timerRepository.getTemplatesByCategory(categoryId).observeForever { templates ->
                    _templates.value = templates ?: emptyList()
                    _loading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _loading.value = false
                _templates.value = emptyList()
            }
        }
    }
    
    fun loadAllTemplates() {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                
                timerRepository.getAllTemplates().observeForever { templates ->
                    _templates.value = templates ?: emptyList()
                    _loading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _loading.value = false
                _templates.value = emptyList()
            }
        }
    }
    
    fun incrementTemplateUsage(templateId: Int) {
        viewModelScope.launch {
            try {
                timerRepository.incrementUsageCount(templateId)
            } catch (e: Exception) {
                _error.value = "사용 횟수 업데이트에 실패했습니다: ${e.message}"
            }
        }
    }
    
    fun updateTemplateActiveState(templateId: Int, isActive: Boolean) {
        viewModelScope.launch {
            try {
                timerRepository.updateTemplateActiveState(templateId, isActive)
            } catch (e: Exception) {
                _error.value = "타이머 상태 업데이트에 실패했습니다: ${e.message}"
            }
        }
    }
    
    fun getMostUsedTemplates(limit: Int = 5) {
        viewModelScope.launch {
            try {
                _loading.value = true
                val mostUsedTemplates = timerRepository.getMostUsedTemplates(limit)
                _templates.value = mostUsedTemplates
                _loading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _loading.value = false
                _templates.value = emptyList()
            }
        }
    }
    
    fun getRecentTemplates(limit: Int = 5) {
        viewModelScope.launch {
            try {
                _loading.value = true
                val recentTemplates = timerRepository.getRecentTemplates(limit)
                _templates.value = recentTemplates
                _loading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _loading.value = false
                _templates.value = emptyList()
            }
        }
    }
    
    fun searchTemplates(query: String) {
        if (query.isBlank()) {
            loadAllTemplates()
            return
        }
        
        viewModelScope.launch {
            try {
                _loading.value = true
                val searchResults = timerRepository.searchTemplates(query)
                _templates.value = searchResults
                _loading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _loading.value = false
                _templates.value = emptyList()
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    // Category operations
    fun loadAllCategories() {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                
                // Initialize default categories if needed
                timerRepository.initializeDefaultCategoriesIfNeeded()
                
                timerRepository.getAllCategories().observeForever { categories ->
                    _categories.value = categories ?: emptyList()
                    _loading.value = false
                }
            } catch (e: Exception) {
                _error.value = "카테고리를 불러오는데 실패했습니다: ${e.message}"
                _loading.value = false
                _categories.value = emptyList()
            }
        }
    }
    
    suspend fun getCategoryById(id: Int): TimerCategory? {
        return try {
            timerRepository.getCategoryById(id)
        } catch (e: Exception) {
            _error.value = "카테고리 정보를 불러오는데 실패했습니다: ${e.message}"
            null
        }
    }
    
    suspend fun getTemplatesByCategory(categoryId: Int): List<TimerTemplate> {
        return try {
            timerRepository.getTemplatesByCategorySync(categoryId)
        } catch (e: Exception) {
            _error.value = "카테고리별 템플릿을 불러오는데 실패했습니다: ${e.message}"
            emptyList()
        }
    }
    
    fun addCategory(category: TimerCategory) {
        viewModelScope.launch {
            try {
                // 현재 카테고리 중 가장 큰 sortOrder를 찾아서 +1
                val allCategories = timerRepository.getAllCategoriesSync()
                val maxSortOrder = allCategories.maxOfOrNull { it.sortOrder } ?: 0
                val categoryWithSortOrder = category.copy(sortOrder = maxSortOrder + 1)
                
                timerRepository.insertCategory(categoryWithSortOrder)
                // Refresh categories after adding
                loadAllCategories()
            } catch (e: Exception) {
                _error.value = "카테고리 추가에 실패했습니다: ${e.message}"
            }
        }
    }
    
    fun updateCategory(category: TimerCategory) {
        viewModelScope.launch {
            try {
                timerRepository.updateCategory(category)
                // Refresh categories after updating
                loadAllCategories()
            } catch (e: Exception) {
                _error.value = "카테고리 수정에 실패했습니다: ${e.message}"
            }
        }
    }
    
    fun deleteCategory(category: TimerCategory) {
        viewModelScope.launch {
            try {
                timerRepository.deleteCategory(category)
                // Refresh categories after deleting
                loadAllCategories()
            } catch (e: Exception) {
                _error.value = "카테고리 삭제에 실패했습니다: ${e.message}"
            }
        }
    }
    
    fun deleteTemplate(templateId: Int) {
        viewModelScope.launch {
            try {
                timerRepository.deleteTemplate(templateId)
                // Refresh current template list
                _templates.value = _templates.value?.filter { it.id != templateId } ?: emptyList()
            } catch (e: Exception) {
                _error.value = "템플릿 삭제에 실패했습니다: ${e.message}"
            }
        }
    }
    
    fun insertTimerTemplate(template: TimerTemplate, rounds: List<TimerRound>) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                
                timerRepository.insertTimerTemplateWithRounds(template, rounds)
                
                _loading.value = false
            } catch (e: Exception) {
                _error.value = "타이머 저장에 실패했습니다: ${e.message}"
                _loading.value = false
            }
        }
    }
    
    // Timer control methods
    fun startTimer(templateId: Int) {
        viewModelScope.launch {
            try {
                val template = timerRepository.getTemplateById(templateId)
                if (template != null && !template.isRunning) {
                    val remainingTime = if (template.remainingSeconds > 0) template.remainingSeconds else template.totalDuration
                    startCountDownTimer(template, remainingTime)
                }
            } catch (e: Exception) {
                _error.value = "타이머 시작에 실패했습니다: ${e.message}"
            }
        }
    }
    
    fun pauseTimer(templateId: Int) {
        activeTimers[templateId]?.cancel()
        activeTimers.remove(templateId)
        
        viewModelScope.launch {
            try {
                val state = timerStates[templateId]
                if (state != null) {
                    timerRepository.updateTimerState(templateId, isRunning = false, remainingSeconds = state.remainingSeconds)
                    refreshTemplates()
                }
            } catch (e: Exception) {
                _error.value = "타이머 일시정지에 실패했습니다: ${e.message}"
            }
        }
    }
    
    fun resetTimer(templateId: Int) {
        activeTimers[templateId]?.cancel()
        activeTimers.remove(templateId)
        timerStates.remove(templateId)
        
        viewModelScope.launch {
            try {
                timerRepository.updateTimerState(templateId, isRunning = false, remainingSeconds = 0)
                refreshTemplates()
            } catch (e: Exception) {
                _error.value = "타이머 리셋에 실패했습니다: ${e.message}"
            }
        }
    }
    
    private fun startCountDownTimer(template: TimerTemplate, remainingSeconds: Int) {
        val timer = object : CountDownTimer(remainingSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                timerStates[template.id] = TimerState(template.id, secondsLeft, true)
                
                viewModelScope.launch {
                    timerRepository.updateTimerState(template.id, isRunning = true, remainingSeconds = secondsLeft)
                    refreshTemplates()
                }
            }
            
            override fun onFinish() {
                timerStates.remove(template.id)
                activeTimers.remove(template.id)
                
                viewModelScope.launch {
                    timerRepository.updateTimerState(template.id, isRunning = false, remainingSeconds = 0)
                    // TODO: Trigger alarm notification
                    onTimerComplete(template)
                    refreshTemplates()
                }
            }
        }
        
        activeTimers[template.id] = timer
        timerStates[template.id] = TimerState(template.id, remainingSeconds, true)
        timer.start()
        
        viewModelScope.launch {
            timerRepository.updateTimerState(template.id, isRunning = true, remainingSeconds = remainingSeconds)
            refreshTemplates()
        }
    }
    
    private fun onTimerComplete(template: TimerTemplate) {
        // Show persistent timer complete notification with alarm sound
        notificationAlarmManager.showTimerCompleteNotification(template.name)
    }
    
    private suspend fun refreshTemplates() {
        // Refresh the current templates list to update UI
        val currentTemplates = _templates.value
        if (currentTemplates != null) {
            val updatedTemplates = currentTemplates.map { template ->
                val state = timerStates[template.id]
                if (state != null) {
                    template.copy(isRunning = state.isRunning, remainingSeconds = state.remainingSeconds)
                } else {
                    template
                }
            }
            _templates.postValue(updatedTemplates)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Cancel all active timers when ViewModel is destroyed
        activeTimers.values.forEach { it.cancel() }
        activeTimers.clear()
        timerStates.clear()
    }
}