package com.semo.alarm.data

import android.content.Context
import android.content.SharedPreferences
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
    }
    
    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 앱 시작 시 기본 템플릿들을 확인하고 필요시 초기화
     */
    fun initializeDefaultTemplatesIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
            if (!isDefaultTemplatesInitialized()) {
                initializeDefaultTemplates()
                markDefaultTemplatesInitialized()
            }
        }
    }
    
    /**
     * 강제로 기본 템플릿들을 다시 초기화 (개발/테스트용)
     */
    fun reinitializeDefaultTemplates() {
        CoroutineScope(Dispatchers.IO).launch {
            initializeDefaultTemplates()
            markDefaultTemplatesInitialized()
        }
    }
    
    /**
     * 기본 템플릿 초기화 여부 확인
     */
    private fun isDefaultTemplatesInitialized(): Boolean {
        return sharedPrefs.getBoolean(KEY_DEFAULT_TEMPLATES_INITIALIZED, false)
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
     * 실제 기본 템플릿들을 데이터베이스에 삽입
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
     * 카테고리별 템플릿 개수 확인
     */
    suspend fun getTemplateCounts(): Map<String, Int> {
        return mapOf(
            "exercise" to DefaultTimerTemplates.getExerciseTemplates().size,
            "cooking" to DefaultTimerTemplates.getCookingTemplates().size,
            "study" to DefaultTimerTemplates.getStudyTemplates().size,
            "drink" to DefaultTimerTemplates.getDrinkTemplates().size
        )
    }
    
    /**
     * 특정 카테고리의 기본 템플릿만 초기화
     */
    suspend fun initializeCategoryTemplates(category: String) {
        try {
            val templates = when (category) {
                "exercise" -> DefaultTimerTemplates.getExerciseTemplates()
                "cooking" -> DefaultTimerTemplates.getCookingTemplates()
                "study" -> DefaultTimerTemplates.getStudyTemplates()
                "drink" -> DefaultTimerTemplates.getDrinkTemplates()
                else -> emptyList()
            }
            
            templates.forEach { (template, rounds) ->
                timerRepository.insertTemplateWithRounds(template, rounds)
            }
            
            android.util.Log.d("DatabaseInitializer", "Category '$category' templates initialized: ${templates.size}")
            
        } catch (e: Exception) {
            android.util.Log.e("DatabaseInitializer", "Failed to initialize $category templates", e)
        }
    }
    
    /**
     * 데이터베이스 초기화 상태 리셋 (개발/테스트용)
     */
    fun resetInitializationState() {
        sharedPrefs.edit()
            .remove(KEY_DEFAULT_TEMPLATES_INITIALIZED)
            .apply()
        
        android.util.Log.d("DatabaseInitializer", "Initialization state reset")
    }
    
    /**
     * 초기화 통계 정보 반환
     */
    suspend fun getInitializationInfo(): Map<String, Any> {
        return mapOf(
            "isInitialized" to isDefaultTemplatesInitialized(),
            "templateCounts" to getTemplateCounts(),
            "totalTemplates" to DefaultTimerTemplates.getAllDefaultTemplates().size,
            "categories" to listOf("exercise", "cooking", "study", "drink")
        )
    }
}