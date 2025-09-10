package com.semo.alarm.data

import android.content.Context
import android.content.SharedPreferences
import com.semo.alarm.data.entities.TimerCategory
import com.semo.alarm.data.repositories.TimerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer @Inject constructor(
    private val timerRepository: TimerRepository,
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val PREF_NAME = "timer_templates_prefs"
        private const val KEY_DEFAULT_TEMPLATES_INITIALIZED = "default_templates_initialized"
        private const val KEY_DEFAULT_CATEGORIES_INITIALIZED = "default_categories_initialized"
    }
    
    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 앱 시작 시 기본 카테고리와 템플릿들을 확인하고 필요시 초기화
     */
    fun initializeDefaultDataIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
            // 항상 중복 카테고리 정리 먼저 실행
            cleanupDuplicateCategoriesSync()
            
            if (!isDefaultCategoriesInitialized()) {
                initializeDefaultCategories()
                markDefaultCategoriesInitialized()
            }
            
            if (!isDefaultTemplatesInitialized()) {
                initializeDefaultTemplates()
                markDefaultTemplatesInitialized()
            }
        }
    }
    
    /**
     * 강제로 기본 데이터들을 다시 초기화 (개발/테스트용)
     */
    fun reinitializeDefaultData() {
        CoroutineScope(Dispatchers.IO).launch {
            cleanupDuplicateCategoriesSync() // 중복 카테고리 정리
            initializeDefaultCategories()
            initializeDefaultTemplates()
            markDefaultCategoriesInitialized()
            markDefaultTemplatesInitialized()
        }
    }
    
    /**
     * 중복된 기본 카테고리를 정리 (비동기)
     */
    fun cleanupDuplicateCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            cleanupDuplicateCategoriesSync()
        }
    }
    
    /**
     * 중복된 기본 카테고리를 정리 (suspend 함수)
     */
    private suspend fun cleanupDuplicateCategoriesSync() {
        try {
            val allCategories = timerRepository.getAllCategoriesSync()
            val defaultCategories = allCategories.filter { it.name == "기본" && it.isDefault }
            
            if (defaultCategories.size > 1) {
                // 첫 번째 것만 남기고 나머지는 삭제
                defaultCategories.drop(1).forEach { category ->
                    timerRepository.deleteCategory(category)
                    android.util.Log.d("DatabaseInitializer", "Removed duplicate category: ${category.id}")
                }
                android.util.Log.d("DatabaseInitializer", "Cleaned up ${defaultCategories.size - 1} duplicate categories")
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseInitializer", "Failed to cleanup duplicate categories", e)
        }
    }
    
    /**
     * 기본 카테고리 초기화 여부 확인
     */
    private fun isDefaultCategoriesInitialized(): Boolean {
        return sharedPrefs.getBoolean(KEY_DEFAULT_CATEGORIES_INITIALIZED, false)
    }
    
    /**
     * 기본 템플릿 초기화 여부 확인
     */
    private fun isDefaultTemplatesInitialized(): Boolean {
        return sharedPrefs.getBoolean(KEY_DEFAULT_TEMPLATES_INITIALIZED, false)
    }
    
    /**
     * 기본 카테고리 초기화 완료 표시
     */
    private fun markDefaultCategoriesInitialized() {
        sharedPrefs.edit()
            .putBoolean(KEY_DEFAULT_CATEGORIES_INITIALIZED, true)
            .apply()
    }
    
    /**
     * 기본 템플릿 초기화 완료 표시
     */
    private fun markDefaultTemplatesInitialized() {
        sharedPrefs.edit()
            .putBoolean(KEY_DEFAULT_TEMPLATES_INITIALIZED, true)
            .apply()
    }
    
    /**
     * 기본 카테고리들을 데이터베이스에 삽입
     */
    private suspend fun initializeDefaultCategories() {
        try {
            val defaultCategories = TimerCategory.getDefaultCategories()
            
            defaultCategories.forEach { category ->
                try {
                    // 이미 존재하는 카테고리인지 확인
                    val existingCategory = timerRepository.getCategoryByName(category.name)
                    if (existingCategory == null) {
                        timerRepository.insertCategory(category)
                        android.util.Log.d("DatabaseInitializer", "Inserted default category: ${category.name}")
                    } else {
                        android.util.Log.d("DatabaseInitializer", "Category already exists: ${category.name}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DatabaseInitializer", "Failed to process category: ${category.name}", e)
                }
            }
            
            android.util.Log.d("DatabaseInitializer", "Default categories initialization completed")
            
        } catch (e: Exception) {
            android.util.Log.e("DatabaseInitializer", "Failed to initialize default categories", e)
        }
    }
    
    /**
     * 기본 템플릿들을 데이터베이스에 삽입
     */
    private suspend fun initializeDefaultTemplates() {
        try {
            val allTemplates = DefaultTimerTemplates.getAllDefaultTemplates()
            
            allTemplates.forEach { (template, rounds) ->
                try {
                    // 템플릿과 라운드들을 함께 삽입
                    timerRepository.insertTemplateWithRounds(template, rounds)
                } catch (e: Exception) {
                    // 개별 템플릿 삽입 실패시 로그 출력 후 계속 진행
                    android.util.Log.e("DatabaseInitializer", "Failed to insert template: ${template.name}", e)
                }
            }
            
            android.util.Log.d("DatabaseInitializer", "Default templates initialized successfully. Total: ${allTemplates.size}")
            
        } catch (e: Exception) {
            android.util.Log.e("DatabaseInitializer", "Failed to initialize default templates", e)
        }
    }
    
    /**
     * 기본 카테고리 템플릿 개수 확인
     */
    suspend fun getTemplateCounts(): Map<String, Int> {
        return mapOf(
            "basic" to DefaultTimerTemplates.getBasicTemplates().size
        )
    }
    
    /**
     * 기본 카테고리의 템플릿들만 초기화
     */
    suspend fun initializeBasicTemplates() {
        try {
            val templates = DefaultTimerTemplates.getBasicTemplates()
            
            templates.forEach { (template, rounds) ->
                timerRepository.insertTemplateWithRounds(template, rounds)
            }
            
            android.util.Log.d("DatabaseInitializer", "Basic templates initialized: ${templates.size}")
            
        } catch (e: Exception) {
            android.util.Log.e("DatabaseInitializer", "Failed to initialize basic templates", e)
        }
    }
    
    /**
     * 데이터베이스 초기화 상태 리셋 (개발/테스트용)
     */
    fun resetInitializationState() {
        sharedPrefs.edit()
            .remove(KEY_DEFAULT_TEMPLATES_INITIALIZED)
            .remove(KEY_DEFAULT_CATEGORIES_INITIALIZED)
            .apply()
        
        android.util.Log.d("DatabaseInitializer", "Initialization state reset")
    }
    
    /**
     * 초기화 통계 정보 반환
     */
    suspend fun getInitializationInfo(): Map<String, Any> {
        return mapOf(
            "categoriesInitialized" to isDefaultCategoriesInitialized(),
            "templatesInitialized" to isDefaultTemplatesInitialized(),
            "templateCounts" to getTemplateCounts(),
            "totalTemplates" to DefaultTimerTemplates.getAllDefaultTemplates().size,
            "totalCategories" to TimerCategory.getDefaultCategories().size,
            "categories" to listOf("basic")
        )
    }
}