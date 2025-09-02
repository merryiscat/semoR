package com.semo.alarm.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semo.alarm.data.entities.TimerTemplate
import com.semo.alarm.data.repositories.TimerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomTimerViewModel @Inject constructor(
    private val timerRepository: TimerRepository
) : ViewModel() {
    
    private val _templates = MutableLiveData<List<TimerTemplate>>()
    val templates: LiveData<List<TimerTemplate>> get() = _templates
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error
    
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
}