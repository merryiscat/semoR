package com.semo.alarm.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semo.alarm.data.entities.TimerTemplate
import com.semo.alarm.data.entities.TimerCategory
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
    
    private val _categories = MutableLiveData<List<TimerCategory>>()
    val categories: LiveData<List<TimerCategory>> get() = _categories
    
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
}