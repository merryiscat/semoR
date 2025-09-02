package com.semo.alarm.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.semo.alarm.data.entities.TimerRound

@Dao
interface TimerRoundDao {
    
    @Query("SELECT * FROM timer_rounds WHERE templateId = :templateId ORDER BY roundIndex ASC")
    suspend fun getRoundsByTemplateId(templateId: Int): List<TimerRound>
    
    @Query("SELECT * FROM timer_rounds WHERE templateId = :templateId ORDER BY roundIndex ASC")
    fun getRoundsByTemplateIdLiveData(templateId: Int): LiveData<List<TimerRound>>
    
    @Query("SELECT * FROM timer_rounds WHERE id = :id")
    suspend fun getRoundById(id: Int): TimerRound?
    
    @Query("SELECT COUNT(*) FROM timer_rounds WHERE templateId = :templateId")
    suspend fun getRoundCountByTemplateId(templateId: Int): Int
    
    @Query("SELECT SUM(duration * cycles) FROM timer_rounds WHERE templateId = :templateId")
    suspend fun getTotalDurationByTemplateId(templateId: Int): Int?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRound(round: TimerRound): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRounds(rounds: List<TimerRound>): List<Long>
    
    @Update
    suspend fun updateRound(round: TimerRound)
    
    @Delete
    suspend fun deleteRound(round: TimerRound)
    
    @Query("DELETE FROM timer_rounds WHERE id = :id")
    suspend fun deleteRoundById(id: Int)
    
    @Query("DELETE FROM timer_rounds WHERE templateId = :templateId")
    suspend fun deleteRoundsByTemplateId(templateId: Int)
    
    @Query("UPDATE timer_rounds SET roundIndex = :newIndex WHERE id = :id")
    suspend fun updateRoundIndex(id: Int, newIndex: Int)
    
    // 라운드 순서 재정렬
    @Transaction
    suspend fun reorderRounds(templateId: Int, roundIds: List<Int>) {
        roundIds.forEachIndexed { index, roundId ->
            updateRoundIndex(roundId, index)
        }
    }
    
    // 특정 템플릿의 라운드 통계
    @Query("""
        SELECT 
            COUNT(*) as totalRounds,
            SUM(duration * cycles) as totalDuration,
            AVG(duration) as avgDuration,
            MIN(duration) as minDuration,
            MAX(duration) as maxDuration
        FROM timer_rounds 
        WHERE templateId = :templateId
    """)
    suspend fun getRoundStats(templateId: Int): com.semo.alarm.data.entities.RoundStats?
    
    // 라운드 타입별 통계 (운동/휴식 등)
    @Query("""
        SELECT name, COUNT(*) as count, SUM(duration) as totalDuration, AVG(duration) as avgDuration
        FROM timer_rounds 
        WHERE templateId = :templateId
        GROUP BY name
    """)
    suspend fun getRoundTypeStats(templateId: Int): List<com.semo.alarm.data.entities.RoundTypeStats>
}