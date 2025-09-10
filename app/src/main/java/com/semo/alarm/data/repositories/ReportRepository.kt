package com.semo.alarm.data.repositories

import androidx.lifecycle.LiveData
import com.semo.alarm.data.dao.ReportDao
import com.semo.alarm.data.dao.AlarmDao
import com.semo.alarm.data.dao.AlarmHistoryDao
import com.semo.alarm.data.dao.SleepRecordDao
import com.semo.alarm.data.dao.TimerTemplateDao
import com.semo.alarm.data.entities.ReportData
import com.semo.alarm.data.entities.AlarmHistory
import com.semo.alarm.data.dao.SleepTrendPoint
import com.semo.alarm.data.dao.TimerTrendPoint
import com.semo.alarm.data.dao.LifestyleTrendPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val reportDao: ReportDao,
    private val alarmDao: AlarmDao,
    private val alarmHistoryDao: AlarmHistoryDao,
    private val sleepRecordDao: SleepRecordDao,
    private val timerTemplateDao: TimerTemplateDao
) {
    
    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        private fun getTodayString(): String = dateFormat.format(Date())
        private fun getDateString(daysAgo: Int): String {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            return dateFormat.format(calendar.time)
        }
    }
    
    // LiveData 조회
    fun getAllReportData(): LiveData<List<ReportData>> = reportDao.getAllReportData()
    
    fun getTodaysReport(): LiveData<ReportData?> = reportDao.getReportByDateLive(getTodayString())
    
    fun getRecentReports(limit: Int = 7): LiveData<List<ReportData>> = reportDao.getRecentReports(limit)
    
    fun getWeeklyReports(): LiveData<List<ReportData>> {
        val startDate = getDateString(6) // 7일 전
        val endDate = getTodayString()
        return reportDao.getReportsByDateRangeLive(startDate, endDate)
    }
    
    fun getMonthlyReports(): LiveData<List<ReportData>> {
        val startDate = getDateString(29) // 30일 전
        val endDate = getTodayString()
        return reportDao.getReportsByDateRangeLive(startDate, endDate)
    }
    
    // 개별 데이터 조회
    suspend fun getReportByDate(date: String): ReportData? = reportDao.getReportByDate(date)
    
    // 통계 데이터 조회
    suspend fun getWeeklyStats(): WeeklyStats = withContext(Dispatchers.IO) {
        val startDate = getDateString(6)
        val endDate = getTodayString()
        
        WeeklyStats(
            averageSleepDuration = reportDao.getAverageSleepDuration(startDate, endDate) ?: 0f,
            averageAlarmDismissalTime = reportDao.getAverageAlarmDismissalTime(startDate, endDate) ?: 0f,
            averageTimerUsage = reportDao.getAverageTimerUsage(startDate, endDate) ?: 0f,
            averageTimerCompletionRate = reportDao.getAverageTimerCompletionRate(startDate, endDate) ?: 0f,
            averageSleepQuality = reportDao.getAverageSleepQuality(startDate, endDate) ?: 0f,
            averageSnoringPercentage = reportDao.getAverageSnoringPercentage(startDate, endDate) ?: 0f,
            averageProductivityScore = reportDao.getAverageProductivityScore(startDate, endDate) ?: 0f,
            averageLifestyleScore = reportDao.getAverageLifestyleScore(startDate, endDate) ?: 0f,
            reportCount = reportDao.getReportCountByDateRange(startDate, endDate)
        )
    }
    
    suspend fun getMonthlyStats(): MonthlyStats = withContext(Dispatchers.IO) {
        val startDate = getDateString(29)
        val endDate = getTodayString()
        
        MonthlyStats(
            averageSleepDuration = reportDao.getAverageSleepDuration(startDate, endDate) ?: 0f,
            averageAlarmDismissalTime = reportDao.getAverageAlarmDismissalTime(startDate, endDate) ?: 0f,
            averageTimerUsage = reportDao.getAverageTimerUsage(startDate, endDate) ?: 0f,
            averageTimerCompletionRate = reportDao.getAverageTimerCompletionRate(startDate, endDate) ?: 0f,
            averageSleepQuality = reportDao.getAverageSleepQuality(startDate, endDate) ?: 0f,
            averageSnoringPercentage = reportDao.getAverageSnoringPercentage(startDate, endDate) ?: 0f,
            averageProductivityScore = reportDao.getAverageProductivityScore(startDate, endDate) ?: 0f,
            averageLifestyleScore = reportDao.getAverageLifestyleScore(startDate, endDate) ?: 0f,
            maxSleepDuration = reportDao.getMaxSleepDuration(startDate, endDate) ?: 0,
            minSleepDuration = reportDao.getMinSleepDuration(startDate, endDate) ?: 0,
            maxTimerUsage = reportDao.getMaxTimerUsage(startDate, endDate) ?: 0,
            reportCount = reportDao.getReportCountByDateRange(startDate, endDate)
        )
    }
    
    // 차트 데이터 조회
    suspend fun getSleepTrendData(days: Int = 7): List<SleepTrendPoint> = withContext(Dispatchers.IO) {
        val startDate = getDateString(days - 1)
        val endDate = getTodayString()
        reportDao.getSleepTrendData(startDate, endDate)
    }
    
    suspend fun getTimerTrendData(days: Int = 7): List<TimerTrendPoint> = withContext(Dispatchers.IO) {
        val startDate = getDateString(days - 1)
        val endDate = getTodayString()
        reportDao.getTimerTrendData(startDate, endDate)
    }
    
    suspend fun getLifestyleTrendData(days: Int = 7): List<LifestyleTrendPoint> = withContext(Dispatchers.IO) {
        val startDate = getDateString(days - 1)
        val endDate = getTodayString()
        reportDao.getLifestyleTrendData(startDate, endDate)
    }
    
    // 데이터 생성 및 업데이트
    suspend fun generateTodaysReport(): ReportData = withContext(Dispatchers.IO) {
        val today = getTodayString()
        val existingReport = reportDao.getReportByDate(today)
        
        // 각 데이터 소스에서 오늘 데이터 수집
        val alarmStats = calculateAlarmStats(today)
        val timerStats = calculateTimerStats(today)
        val sleepStats = calculateSleepStats(today)
        
        // 종합 점수 계산
        val productivityScore = calculateProductivityScore(timerStats, alarmStats)
        val lifestyleScore = calculateLifestyleScore(sleepStats, alarmStats, timerStats)
        
        val reportData = ReportData(
            id = existingReport?.id ?: 0,
            date = today,
            alarmDismissalTime = alarmStats.averageDismissalTime,
            snoozeCount = alarmStats.totalSnoozeCount,
            timerUsageMinutes = timerStats.totalUsageMinutes,
            timerCompletionRate = timerStats.completionRate,
            sleepDuration = sleepStats.duration,
            sleepQualityScore = sleepStats.qualityScore,
            snoringPercentage = sleepStats.snoringPercentage,
            productivityScore = productivityScore,
            lifestyleScore = lifestyleScore,
            createdAt = System.currentTimeMillis()
        )
        
        reportDao.insertReportData(reportData)
        reportData
    }
    
    suspend fun insertReportData(reportData: ReportData) = reportDao.insertReportData(reportData)
    
    suspend fun updateReportData(reportData: ReportData) = reportDao.updateReportData(reportData)
    
    suspend fun deleteReportByDate(date: String) = reportDao.deleteReportByDate(date)
    
    suspend fun deleteOldReports(daysToKeep: Int = 90) {
        val cutoffDate = getDateString(daysToKeep)
        reportDao.deleteOldReports(cutoffDate)
    }
    
    // 내부 계산 메서드들
    private suspend fun calculateAlarmStats(date: String): AlarmStats {
        // TODO: 실제 알람 해제 데이터 계산 로직 구현
        // 현재는 더미 데이터 반환
        return AlarmStats(
            averageDismissalTime = 120, // 2분
            totalSnoozeCount = 1
        )
    }
    
    private suspend fun calculateTimerStats(date: String): TimerStats {
        // TODO: 실제 타이머 사용 데이터 계산 로직 구현
        // 현재는 더미 데이터 반환
        return TimerStats(
            totalUsageMinutes = 180, // 3시간
            completionRate = 0.85f   // 85% 완주율
        )
    }
    
    private suspend fun calculateSleepStats(date: String): SleepStats {
        // TODO: 실제 수면 데이터 계산 로직 구현
        // 현재는 더미 데이터 반환
        return SleepStats(
            duration = 450,        // 7시간 30분
            qualityScore = 4.0f,   // 좋음
            snoringPercentage = 12.5f // 12.5%
        )
    }
    
    private fun calculateProductivityScore(timerStats: TimerStats, alarmStats: AlarmStats): Float {
        val timerScore = (timerStats.completionRate * 50f) // 최대 50점
        val alarmScore = if (alarmStats.averageDismissalTime <= 180) 50f else 30f // 3분 이내면 50점, 아니면 30점
        
        return (timerScore + alarmScore).coerceIn(0f, 100f)
    }
    
    private fun calculateLifestyleScore(sleepStats: SleepStats, alarmStats: AlarmStats, timerStats: TimerStats): Float {
        val sleepScore = (sleepStats.qualityScore / 5f) * 40f // 최대 40점
        val consistencyScore = if (alarmStats.averageDismissalTime <= 180) 30f else 15f // 최대 30점
        val activityScore = (timerStats.completionRate * 30f) // 최대 30점
        
        return (sleepScore + consistencyScore + activityScore).coerceIn(0f, 100f)
    }
    
    // AlarmHistory 관련 메서드들
    suspend fun getAlarmHistoryByDate(date: String): List<AlarmHistory> = withContext(Dispatchers.IO) {
        alarmHistoryDao.getAlarmHistoryByDate(date)
    }
    
    fun getAlarmHistoryByDateLive(date: String): LiveData<List<AlarmHistory>> = 
        alarmHistoryDao.getAlarmHistoryByDateLive(date)
    
    suspend fun insertAlarmHistory(alarmHistory: AlarmHistory): Long = 
        alarmHistoryDao.insertAlarmHistory(alarmHistory)
    
    suspend fun deleteAlarmHistory(alarmHistory: AlarmHistory) = 
        alarmHistoryDao.deleteAlarmHistory(alarmHistory)
    
    suspend fun deleteOldAlarmHistory(daysToKeep: Int = 31) {
        val cutoffDate = getDateString(daysToKeep)
        alarmHistoryDao.deleteOldAlarmHistory(cutoffDate)
    }
}

// 통계 데이터 클래스들
data class WeeklyStats(
    val averageSleepDuration: Float,
    val averageAlarmDismissalTime: Float,
    val averageTimerUsage: Float,
    val averageTimerCompletionRate: Float,
    val averageSleepQuality: Float,
    val averageSnoringPercentage: Float,
    val averageProductivityScore: Float,
    val averageLifestyleScore: Float,
    val reportCount: Int
)

data class MonthlyStats(
    val averageSleepDuration: Float,
    val averageAlarmDismissalTime: Float,
    val averageTimerUsage: Float,
    val averageTimerCompletionRate: Float,
    val averageSleepQuality: Float,
    val averageSnoringPercentage: Float,
    val averageProductivityScore: Float,
    val averageLifestyleScore: Float,
    val maxSleepDuration: Int,
    val minSleepDuration: Int,
    val maxTimerUsage: Int,
    val reportCount: Int
)

// 내부 계산용 데이터 클래스들
private data class AlarmStats(
    val averageDismissalTime: Int,
    val totalSnoozeCount: Int
)

private data class TimerStats(
    val totalUsageMinutes: Int,
    val completionRate: Float
)

private data class SleepStats(
    val duration: Int,
    val qualityScore: Float,
    val snoringPercentage: Float
)