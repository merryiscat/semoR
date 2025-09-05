package com.semo.alarm.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.semo.alarm.data.entities.ReportData

@Dao
interface ReportDao {
    
    @Query("SELECT * FROM report_data ORDER BY date DESC")
    fun getAllReportData(): LiveData<List<ReportData>>
    
    @Query("SELECT * FROM report_data WHERE date = :date LIMIT 1")
    suspend fun getReportByDate(date: String): ReportData?
    
    @Query("SELECT * FROM report_data WHERE date = :date LIMIT 1")
    fun getReportByDateLive(date: String): LiveData<ReportData?>
    
    @Query("SELECT * FROM report_data ORDER BY date DESC LIMIT :limit")
    fun getRecentReports(limit: Int): LiveData<List<ReportData>>
    
    @Query("SELECT * FROM report_data WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getReportsByDateRange(startDate: String, endDate: String): List<ReportData>
    
    @Query("SELECT * FROM report_data WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getReportsByDateRangeLive(startDate: String, endDate: String): LiveData<List<ReportData>>
    
    @Query("SELECT AVG(sleepDuration) FROM report_data WHERE date >= :startDate AND date <= :endDate")
    suspend fun getAverageSleepDuration(startDate: String, endDate: String): Float?
    
    @Query("SELECT AVG(alarmDismissalTime) FROM report_data WHERE date >= :startDate AND date <= :endDate")
    suspend fun getAverageAlarmDismissalTime(startDate: String, endDate: String): Float?
    
    @Query("SELECT AVG(timerUsageMinutes) FROM report_data WHERE date >= :startDate AND date <= :endDate")
    suspend fun getAverageTimerUsage(startDate: String, endDate: String): Float?
    
    @Query("SELECT AVG(timerCompletionRate) FROM report_data WHERE date >= :startDate AND date <= :endDate")
    suspend fun getAverageTimerCompletionRate(startDate: String, endDate: String): Float?
    
    @Query("SELECT AVG(sleepQualityScore) FROM report_data WHERE date >= :startDate AND date <= :endDate")
    suspend fun getAverageSleepQuality(startDate: String, endDate: String): Float?
    
    @Query("SELECT AVG(snoringPercentage) FROM report_data WHERE date >= :startDate AND date <= :endDate")
    suspend fun getAverageSnoringPercentage(startDate: String, endDate: String): Float?
    
    @Query("SELECT AVG(productivityScore) FROM report_data WHERE date >= :startDate AND date <= :endDate")
    suspend fun getAverageProductivityScore(startDate: String, endDate: String): Float?
    
    @Query("SELECT AVG(lifestyleScore) FROM report_data WHERE date >= :startDate AND date <= :endDate")
    suspend fun getAverageLifestyleScore(startDate: String, endDate: String): Float?
    
    @Query("SELECT COUNT(*) FROM report_data WHERE date >= :startDate AND date <= :endDate")
    suspend fun getReportCountByDateRange(startDate: String, endDate: String): Int
    
    @Query("SELECT MAX(sleepDuration) FROM report_data WHERE date >= :startDate AND date <= :endDate")
    suspend fun getMaxSleepDuration(startDate: String, endDate: String): Int?
    
    @Query("SELECT MIN(sleepDuration) FROM report_data WHERE date >= :startDate AND date <= :endDate")
    suspend fun getMinSleepDuration(startDate: String, endDate: String): Int?
    
    @Query("SELECT MAX(timerUsageMinutes) FROM report_data WHERE date >= :startDate AND date <= :endDate")
    suspend fun getMaxTimerUsage(startDate: String, endDate: String): Int?
    
    @Query("SELECT date, sleepDuration FROM report_data WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getSleepTrendData(startDate: String, endDate: String): List<SleepTrendPoint>
    
    @Query("SELECT date, timerUsageMinutes FROM report_data WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getTimerTrendData(startDate: String, endDate: String): List<TimerTrendPoint>
    
    @Query("SELECT date, lifestyleScore FROM report_data WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getLifestyleTrendData(startDate: String, endDate: String): List<LifestyleTrendPoint>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReportData(reportData: ReportData): Long
    
    @Update
    suspend fun updateReportData(reportData: ReportData)
    
    @Delete
    suspend fun deleteReportData(reportData: ReportData)
    
    @Query("DELETE FROM report_data WHERE date = :date")
    suspend fun deleteReportByDate(date: String)
    
    @Query("DELETE FROM report_data WHERE date < :cutoffDate")
    suspend fun deleteOldReports(cutoffDate: String)
    
    // 오늘 리포트 데이터 생성/업데이트를 위한 메서드
    @Query("""
        INSERT OR REPLACE INTO report_data (
            date, alarmDismissalTime, snoozeCount, timerUsageMinutes, 
            timerCompletionRate, sleepDuration, sleepQualityScore, 
            snoringPercentage, productivityScore, lifestyleScore, createdAt
        ) VALUES (
            :date, :alarmDismissalTime, :snoozeCount, :timerUsageMinutes,
            :timerCompletionRate, :sleepDuration, :sleepQualityScore,
            :snoringPercentage, :productivityScore, :lifestyleScore, :createdAt
        )
    """)
    suspend fun upsertDailyReport(
        date: String,
        alarmDismissalTime: Int,
        snoozeCount: Int,
        timerUsageMinutes: Int,
        timerCompletionRate: Float,
        sleepDuration: Int,
        sleepQualityScore: Float,
        snoringPercentage: Float,
        productivityScore: Float,
        lifestyleScore: Float,
        createdAt: Long
    )
}

// 차트용 데이터 클래스들
data class SleepTrendPoint(
    val date: String,
    val sleepDuration: Int
)

data class TimerTrendPoint(
    val date: String,
    val timerUsageMinutes: Int
)

data class LifestyleTrendPoint(
    val date: String,
    val lifestyleScore: Float
)