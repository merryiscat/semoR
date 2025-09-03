package com.semo.alarm.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semo.alarm.data.entities.TimerCategory
import com.semo.alarm.data.repositories.TimerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: TimerRepository
) : ViewModel() {
    
    val allCategories: LiveData<List<TimerCategory>> = repository.getAllCategories()
    
    fun addCategory(category: TimerCategory) {
        viewModelScope.launch {
            repository.insertCategory(category)
        }
    }
    
    fun updateCategory(category: TimerCategory) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }
    
    fun deleteCategory(category: TimerCategory) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }
}