package com.semo.alarm.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.semo.alarm.data.entities.TimerTemplate
import com.semo.alarm.data.entities.TimerCategory
import com.semo.alarm.data.entities.TimerRound
import com.semo.alarm.data.repositories.TimerRepository
import com.semo.alarm.services.TimerForegroundService
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
    
    // Timer management - TimerForegroundService 사용으로 간소화됨
    private val notificationAlarmManager = NotificationAlarmManager(getApplication())
    
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
                // 사용자 카테고리는 기본 카테고리 이후에 배치
                // 기본 카테고리의 최대 sortOrder를 찾고, 사용자 카테고리 중 최대값 + 1 설정
                val allCategories = timerRepository.getAllCategoriesSync()
                val defaultCategories = allCategories.filter { it.isDefault }
                val userCategories = allCategories.filter { !it.isDefault }
                
                val maxDefaultOrder = defaultCategories.maxOfOrNull { it.sortOrder } ?: 0
                val maxUserOrder = userCategories.maxOfOrNull { it.sortOrder } ?: maxDefaultOrder
                
                // 새 사용자 카테고리는 기존 사용자 카테고리 다음에 배치
                val newSortOrder = if (userCategories.isEmpty()) {
                    maxDefaultOrder + 1
                } else {
                    maxUserOrder + 1
                }
                
                val categoryWithSortOrder = category.copy(
                    sortOrder = newSortOrder,
                    isDefault = false,  // 사용자 추가 카테고리는 기본 카테고리가 아님
                    createdBy = "user"  // 사용자가 생성한 카테고리
                )
                
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
                    
                    // 기존 CountDownTimer 방식 대신 TimerForegroundService 사용
                    startTimerService(template, remainingTime)
                    
                    // 데이터베이스 상태 업데이트
                    timerRepository.updateTimerState(templateId, isRunning = true, remainingSeconds = remainingTime)
                    refreshTemplates()
                }
            } catch (e: Exception) {
                _error.value = "타이머 시작에 실패했습니다: ${e.message}"
            }
        }
    }
    
    fun pauseTimer(templateId: Int) {
        // TimerForegroundService에 일시정지 명령 전송
        val intent = Intent(getApplication(), TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_PAUSE_TIMER
            putExtra(TimerForegroundService.EXTRA_TIMER_ID, templateId)
        }
        getApplication<Application>().startService(intent)
        
        // TimerForegroundService가 알아서 DB 업데이트할 것이므로 여기서는 하지 않음
        // viewModelScope.launch {
        //     refreshTemplates() // 필요시 새로고침만
        // }
    }
    
    fun resetTimer(templateId: Int) {
        // TimerForegroundService 중지
        val intent = Intent(getApplication(), TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_STOP_TIMER
            putExtra(TimerForegroundService.EXTRA_TIMER_ID, templateId)
        }
        getApplication<Application>().startService(intent)
        
        viewModelScope.launch {
            try {
                timerRepository.updateTimerState(templateId, isRunning = false, remainingSeconds = 0)
                refreshTemplates()
            } catch (e: Exception) {
                _error.value = "타이머 리셋에 실패했습니다: ${e.message}"
            }
        }
    }
    
    private fun startTimerService(template: TimerTemplate, remainingSeconds: Int) {
        // TimerForegroundService 시작
        val intent = Intent(getApplication(), TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_START_TIMER
            putExtra(TimerForegroundService.EXTRA_TIMER_NAME, template.name)
            putExtra(TimerForegroundService.EXTRA_TIMER_DURATION, remainingSeconds)
            putExtra(TimerForegroundService.EXTRA_TIMER_ID, template.id)
        }
        
        // API 26 이상에서는 startForegroundService, 이하에서는 startService 사용
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }
    
    private suspend fun refreshTemplates() {
        // TimerForegroundService 사용으로 상태 관리가 서비스로 이동됨
        // 데이터베이스에서 최신 상태를 다시 로드
        val currentCategoryId = _templates.value?.firstOrNull()?.categoryId
        if (currentCategoryId != null) {
            loadTemplatesByCategory(currentCategoryId)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // TimerForegroundService는 별도로 실행되므로 여기서 정리할 필요 없음
    }
}