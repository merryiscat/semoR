package com.semo.alarm.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.semo.alarm.data.entities.SleepRecord
import com.semo.alarm.data.repositories.SleepRepository
import com.semo.alarm.services.SleepTrackingService
import com.semo.alarm.utils.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SleepViewModel @Inject constructor(
    private val application: Application,
    private val sleepRepository: SleepRepository
) : AndroidViewModel(application) {
    
    // LiveData for observing active sleep record
    val activeSleepRecord: LiveData<SleepRecord?> = sleepRepository.getActiveSleepRecord()
    
    // LiveData for recent sleep records
    val recentSleepRecords: LiveData<List<SleepRecord>> = sleepRepository.getRecentSleepRecords(7)
    
    // Private mutable live data for UI states
    private val _sleepTrackingState = MutableLiveData<SleepTrackingState>(SleepTrackingState.IDLE)
    val sleepTrackingState: LiveData<SleepTrackingState> = _sleepTrackingState
    
    private val _todaysSleepSummary = MutableLiveData<SleepSummary?>()
    val todaysSleepSummary: LiveData<SleepSummary?> = _todaysSleepSummary
    
    private val _averageStats = MutableLiveData<AverageStats?>()
    val averageStats: LiveData<AverageStats?> = _averageStats
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    // Current active sleep record ID
    private var currentSleepRecordId: Long = -1
    
    init {
        loadTodaysSummary()
        loadAverageStats()
        // 앱 재시작 시 수면 추적 상태 동기화
        checkCurrentSleepTrackingState()
    }
    
    /**
     * 앱 재시작 시 실제 수면 추적 상태를 확인하고 UI를 동기화
     */
    private fun checkCurrentSleepTrackingState() {
        viewModelScope.launch {
            try {
                val activeSleep = sleepRepository.getActiveSleepRecordSync()
                if (activeSleep != null) {
                    // 활성화된 수면 기록이 있으면 TRACKING 상태로 설정
                    currentSleepRecordId = activeSleep.id
                    _sleepTrackingState.value = SleepTrackingState.TRACKING
                } else {
                    // 활성화된 수면 기록이 없으면 IDLE 상태로 설정
                    currentSleepRecordId = -1L
                    _sleepTrackingState.value = SleepTrackingState.IDLE
                }
            } catch (e: Exception) {
                // 오류 발생 시 IDLE 상태로 설정
                _sleepTrackingState.value = SleepTrackingState.IDLE
                _errorMessage.value = "수면 추적 상태 확인 실패: ${e.message}"
            }
        }
    }
    
    fun startSleepTracking() {
        viewModelScope.launch {
            try {
                _sleepTrackingState.value = SleepTrackingState.STARTING
                
                // Check if there's already an active session
                val activeSleep = sleepRepository.getActiveSleepRecordSync()
                if (activeSleep != null) {
                    _errorMessage.value = "이미 수면 추적 중입니다."
                    _sleepTrackingState.value = SleepTrackingState.TRACKING
                    currentSleepRecordId = activeSleep.id
                    return@launch
                }
                
                // 배터리 최적화 상태 체크 및 경고 (중요한 기능이므로 확인)
                checkBatteryOptimizationForSleepTracking()
                
                // Start new sleep tracking session
                currentSleepRecordId = sleepRepository.startSleepTracking()
                
                // Start foreground service
                SleepTrackingService.startSleepTracking(
                    application,
                    currentSleepRecordId,
                    System.currentTimeMillis(),
                    true // 코골이 감지 활성화
                )
                
                _sleepTrackingState.value = SleepTrackingState.TRACKING
                
            } catch (e: Exception) {
                _errorMessage.value = "수면 추적을 시작할 수 없습니다: ${e.message}"
                _sleepTrackingState.value = SleepTrackingState.IDLE
            }
        }
    }
    
    fun stopSleepTracking() {
        viewModelScope.launch {
            try {
                _sleepTrackingState.value = SleepTrackingState.STOPPING
                
                if (currentSleepRecordId == -1L) {
                    // Try to find active sleep record
                    val activeSleep = sleepRepository.getActiveSleepRecordSync()
                    if (activeSleep == null) {
                        _errorMessage.value = "활성화된 수면 추적이 없습니다."
                        _sleepTrackingState.value = SleepTrackingState.IDLE
                        return@launch
                    }
                    currentSleepRecordId = activeSleep.id
                }
                
                // Stop foreground service first
                SleepTrackingService.stopSleepTracking(application)
                
                // Stop sleep tracking
                val completedRecord = sleepRepository.stopSleepTracking(currentSleepRecordId)
                if (completedRecord != null) {
                    _sleepTrackingState.value = SleepTrackingState.IDLE
                    currentSleepRecordId = -1L
                    
                    // Refresh today's summary
                    loadTodaysSummary()
                    loadAverageStats()
                } else {
                    _errorMessage.value = "수면 추적을 종료할 수 없습니다."
                    _sleepTrackingState.value = SleepTrackingState.TRACKING
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "수면 추적을 종료할 수 없습니다: ${e.message}"
                _sleepTrackingState.value = SleepTrackingState.TRACKING
            }
        }
    }
    
    private fun loadTodaysSummary() {
        viewModelScope.launch {
            try {
                val todaysRecord = sleepRepository.getTodaysSleepRecord()
                if (todaysRecord != null) {
                    _todaysSleepSummary.value = SleepSummary(
                        sleepDuration = todaysRecord.getFormattedDuration(),
                        snoringPercentage = calculateSnoringPercentage(todaysRecord),
                        qualityScore = todaysRecord.qualityScore,
                        bedtime = todaysRecord.bedtime,
                        wakeupTime = todaysRecord.wakeupTime
                    )
                } else {
                    _todaysSleepSummary.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = "오늘 수면 데이터를 불러올 수 없습니다: ${e.message}"
            }
        }
    }
    
    private fun loadAverageStats() {
        viewModelScope.launch {
            try {
                val avgDuration = sleepRepository.getAverageSleepDuration()
                val avgSnoring = sleepRepository.getAverageSnoringPercentage()
                val recordCount = sleepRepository.getSleepRecordsCountLast7Days()
                
                _averageStats.value = AverageStats(
                    averageSleepDuration = formatDuration(avgDuration),
                    averageSnoringPercentage = avgSnoring.toInt(),
                    recordCount = recordCount
                )
            } catch (e: Exception) {
                _errorMessage.value = "평균 통계를 불러올 수 없습니다: ${e.message}"
            }
        }
    }
    
    fun updateSnoringData(snoringData: String, snoringDetected: Boolean) {
        if (currentSleepRecordId != -1L) {
            viewModelScope.launch {
                try {
                    sleepRepository.updateSnoringData(currentSleepRecordId, snoringData, snoringDetected)
                } catch (e: Exception) {
                    _errorMessage.value = "코골이 데이터 업데이트 실패: ${e.message}"
                }
            }
        }
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    /**
     * Fragment의 onResume 시점에 호출되어 상태를 다시 동기화
     */
    fun refreshTrackingState() {
        checkCurrentSleepTrackingState()
    }
    
    /**
     * 오늘의 수면 기록을 삭제합니다
     */
    fun deleteTodaysSleepRecord() {
        viewModelScope.launch {
            try {
                val todaysRecord = sleepRepository.getTodaysSleepRecord()
                if (todaysRecord != null) {
                    sleepRepository.deleteSleepRecordById(todaysRecord.id)
                    // UI 새로고침
                    loadTodaysSummary()
                    loadAverageStats()
                } else {
                    _errorMessage.value = "삭제할 수면 기록이 없습니다."
                }
            } catch (e: Exception) {
                _errorMessage.value = "수면 기록 삭제 실패: ${e.message}"
            }
        }
    }
    
    private fun calculateSnoringPercentage(sleepRecord: SleepRecord): Int {
        // TODO: Parse snoringData JSON and calculate actual percentage
        // For now, return simple boolean-based percentage
        return if (sleepRecord.snoringDetected) 15 else 0
    }
    
    private fun checkBatteryOptimizationForSleepTracking() {
        val powerManager = application.getSystemService(Application.POWER_SERVICE) as android.os.PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(application.packageName)) {
            _errorMessage.value = "수면 추적의 정확성을 위해 배터리 최적화를 해제해 주세요. 설정에서 변경할 수 있습니다."
        }
    }
    
    private fun formatDuration(millis: Long): String {
        val hours = (millis / (1000 * 60 * 60)).toInt()
        val minutes = ((millis % (1000 * 60 * 60)) / (1000 * 60)).toInt()
        
        return when {
            hours > 0 -> "${hours}시간 ${minutes}분"
            else -> "${minutes}분"
        }
    }
}

enum class SleepTrackingState {
    IDLE,           // 대기 상태
    STARTING,       // 수면 추적 시작 중
    TRACKING,       // 수면 추적 중
    STOPPING        // 수면 추적 종료 중
}

data class SleepSummary(
    val sleepDuration: String,
    val snoringPercentage: Int,
    val qualityScore: Float,
    val bedtime: Long,
    val wakeupTime: Long?
)

data class AverageStats(
    val averageSleepDuration: String,
    val averageSnoringPercentage: Int,
    val recordCount: Int
)