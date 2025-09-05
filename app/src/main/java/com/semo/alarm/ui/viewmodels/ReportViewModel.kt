package com.semo.alarm.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.semo.alarm.data.entities.ReportData
import com.semo.alarm.data.repositories.ReportRepository
import com.semo.alarm.data.repositories.WeeklyStats
import com.semo.alarm.data.repositories.MonthlyStats
import com.semo.alarm.data.dao.SleepTrendPoint
import com.semo.alarm.data.dao.TimerTrendPoint
import com.semo.alarm.data.dao.LifestyleTrendPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val application: Application,
    private val reportRepository: ReportRepository
) : AndroidViewModel(application) {
    
    // LiveData for UI observing
    val todaysReport: LiveData<ReportData?> = reportRepository.getTodaysReport()
    val weeklyReports: LiveData<List<ReportData>> = reportRepository.getWeeklyReports()
    val monthlyReports: LiveData<List<ReportData>> = reportRepository.getMonthlyReports()
    
    // Private MutableLiveData for loading states and errors
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _weeklyStats = MutableLiveData<WeeklyStats?>()
    val weeklyStats: LiveData<WeeklyStats?> = _weeklyStats
    
    private val _monthlyStats = MutableLiveData<MonthlyStats?>()
    val monthlyStats: LiveData<MonthlyStats?> = _monthlyStats
    
    // Chart data
    private val _sleepTrendData = MutableLiveData<List<SleepTrendPoint>>()
    val sleepTrendData: LiveData<List<SleepTrendPoint>> = _sleepTrendData
    
    private val _timerTrendData = MutableLiveData<List<TimerTrendPoint>>()
    val timerTrendData: LiveData<List<TimerTrendPoint>> = _timerTrendData
    
    private val _lifestyleTrendData = MutableLiveData<List<LifestyleTrendPoint>>()
    val lifestyleTrendData: LiveData<List<LifestyleTrendPoint>> = _lifestyleTrendData
    
    // Insights data
    private val _personalityType = MutableLiveData<PersonalityType>()
    val personalityType: LiveData<PersonalityType> = _personalityType
    
    private val _suggestions = MutableLiveData<List<Suggestion>>()
    val suggestions: LiveData<List<Suggestion>> = _suggestions
    
    init {
        refreshAllData()
    }
    
    fun refreshAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Generate today's report if not exists
                generateTodaysReportIfNeeded()
                
                // Load statistics
                loadWeeklyStats()
                loadMonthlyStats()
                
                // Load chart data
                loadChartData()
                
                // Generate insights
                generateInsights()
                
            } catch (e: Exception) {
                _errorMessage.value = "데이터를 불러오는 중 오류가 발생했습니다: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun generateTodaysReportIfNeeded() {
        viewModelScope.launch {
            try {
                reportRepository.generateTodaysReport()
            } catch (e: Exception) {
                _errorMessage.value = "오늘 리포트 생성 중 오류가 발생했습니다: ${e.message}"
            }
        }
    }
    
    private suspend fun loadWeeklyStats() {
        try {
            val stats = reportRepository.getWeeklyStats()
            _weeklyStats.value = stats
        } catch (e: Exception) {
            _errorMessage.value = "주간 통계 로딩 오류: ${e.message}"
        }
    }
    
    private suspend fun loadMonthlyStats() {
        try {
            val stats = reportRepository.getMonthlyStats()
            _monthlyStats.value = stats
        } catch (e: Exception) {
            _errorMessage.value = "월간 통계 로딩 오류: ${e.message}"
        }
    }
    
    private suspend fun loadChartData() {
        try {
            // Load 7 days trend data
            val sleepData = reportRepository.getSleepTrendData(7)
            _sleepTrendData.value = sleepData
            
            val timerData = reportRepository.getTimerTrendData(7)
            _timerTrendData.value = timerData
            
            val lifestyleData = reportRepository.getLifestyleTrendData(7)
            _lifestyleTrendData.value = lifestyleData
            
        } catch (e: Exception) {
            _errorMessage.value = "차트 데이터 로딩 오류: ${e.message}"
        }
    }
    
    private suspend fun generateInsights() {
        try {
            val weeklyStats = _weeklyStats.value ?: return
            
            // Analyze personality type
            val personality = analyzePersonality(weeklyStats)
            _personalityType.value = personality
            
            // Generate suggestions
            val suggestions = generateSuggestions(weeklyStats)
            _suggestions.value = suggestions
            
        } catch (e: Exception) {
            _errorMessage.value = "인사이트 생성 오류: ${e.message}"
        }
    }
    
    private fun analyzePersonality(stats: WeeklyStats): PersonalityType {
        // TODO: Implement more sophisticated analysis
        return when {
            stats.averageSleepDuration >= 450 && stats.averageAlarmDismissalTime <= 120 -> PersonalityType.EARLY_BIRD
            stats.averageSleepDuration < 360 && stats.averageAlarmDismissalTime > 300 -> PersonalityType.NIGHT_OWL
            else -> PersonalityType.NORMAL
        }
    }
    
    private fun generateSuggestions(stats: WeeklyStats): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        
        // Sleep improvement suggestions
        if (stats.averageSleepDuration < 420) { // Less than 7 hours
            suggestions.add(
                Suggestion(
                    type = SuggestionType.SLEEP,
                    title = "수면 시간 늘리기",
                    description = "평균 수면 시간이 부족합니다. 30분 일찍 잠자리에 들어보세요.",
                    priority = SuggestionPriority.HIGH
                )
            )
        }
        
        // Alarm consistency suggestions
        if (stats.averageAlarmDismissalTime > 180) { // More than 3 minutes
            suggestions.add(
                Suggestion(
                    type = SuggestionType.ALARM,
                    title = "기상 습관 개선",
                    description = "알람을 더 빨리 해제하는 습관을 만들어보세요.",
                    priority = SuggestionPriority.MEDIUM
                )
            )
        }
        
        // Timer usage suggestions
        if (stats.averageTimerCompletionRate < 0.8) { // Less than 80%
            suggestions.add(
                Suggestion(
                    type = SuggestionType.TIMER,
                    title = "타이머 완주율 향상",
                    description = "더 짧은 시간부터 시작해서 점진적으로 늘려보세요.",
                    priority = SuggestionPriority.MEDIUM
                )
            )
        }
        
        // Productivity suggestions
        if (stats.averageProductivityScore < 70) {
            suggestions.add(
                Suggestion(
                    type = SuggestionType.PRODUCTIVITY,
                    title = "생산성 향상",
                    description = "규칙적인 타이머 사용으로 집중력을 높여보세요.",
                    priority = SuggestionPriority.LOW
                )
            )
        }
        
        return suggestions
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    // Chart data methods for different time ranges
    fun loadSleepTrendData(days: Int) {
        viewModelScope.launch {
            try {
                val data = reportRepository.getSleepTrendData(days)
                _sleepTrendData.value = data
            } catch (e: Exception) {
                _errorMessage.value = "수면 트렌드 데이터 로딩 오류: ${e.message}"
            }
        }
    }
    
    fun loadTimerTrendData(days: Int) {
        viewModelScope.launch {
            try {
                val data = reportRepository.getTimerTrendData(days)
                _timerTrendData.value = data
            } catch (e: Exception) {
                _errorMessage.value = "타이머 트렌드 데이터 로딩 오류: ${e.message}"
            }
        }
    }
    
    fun loadLifestyleTrendData(days: Int) {
        viewModelScope.launch {
            try {
                val data = reportRepository.getLifestyleTrendData(days)
                _lifestyleTrendData.value = data
            } catch (e: Exception) {
                _errorMessage.value = "라이프스타일 트렌드 데이터 로딩 오류: ${e.message}"
            }
        }
    }
}

// Enums and Data Classes for Insights
enum class PersonalityType(val displayName: String, val description: String) {
    EARLY_BIRD("아침형 인간", "일찍 자고 일찍 일어나는 규칙적인 생활을 하고 있어요"),
    NIGHT_OWL("저녁형 인간", "밤늦게 활동하고 늦게 일어나는 패턴이에요"),
    NORMAL("일반형", "평균적인 수면 패턴을 보이고 있어요")
}

data class Suggestion(
    val type: SuggestionType,
    val title: String,
    val description: String,
    val priority: SuggestionPriority
)

enum class SuggestionType(val displayName: String, val emoji: String) {
    SLEEP("수면", "😴"),
    ALARM("알람", "⏰"),
    TIMER("타이머", "⏱️"),
    PRODUCTIVITY("생산성", "⚡")
}

enum class SuggestionPriority {
    HIGH, MEDIUM, LOW
}