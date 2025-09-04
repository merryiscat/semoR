package com.semo.alarm.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.semo.alarm.data.entities.TimerTemplate

@Dao
interface TimerTemplateDao {
    
    @Query("SELECT * FROM timer_templates ORDER BY usageCount DESC, name ASC")
    fun getAllTemplates(): LiveData<List<TimerTemplate>>
    
    @Query("SELECT * FROM timer_templates WHERE categoryId = :categoryId ORDER BY usageCount DESC, name ASC")
    fun getTemplatesByCategory(categoryId: Int): LiveData<List<TimerTemplate>>
    
    @Query("SELECT * FROM timer_templates WHERE categoryId = :categoryId ORDER BY usageCount DESC, name ASC")
    suspend fun getTemplatesByCategorySync(categoryId: Int): List<TimerTemplate>
    
    @Query("SELECT * FROM timer_templates WHERE isDefault = 1 ORDER BY categoryId, name ASC")
    fun getDefaultTemplates(): LiveData<List<TimerTemplate>>
    
    @Query("SELECT * FROM timer_templates WHERE createdBy = 'user' ORDER BY createdAt DESC")
    fun getCustomTemplates(): LiveData<List<TimerTemplate>>
    
    @Query("SELECT * FROM timer_templates WHERE id = :id")
    suspend fun getTemplateById(id: Int): TimerTemplate?
    
    @Query("SELECT * FROM timer_templates ORDER BY usageCount DESC LIMIT :limit")
    suspend fun getMostUsedTemplates(limit: Int = 5): List<TimerTemplate>
    
    @Query("SELECT * FROM timer_templates ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentTemplates(limit: Int = 5): List<TimerTemplate>
    
    @Query("SELECT COUNT(*) FROM timer_templates")
    suspend fun getTemplateCount(): Int
    
    @Query("SELECT COUNT(*) FROM timer_templates WHERE categoryId = :categoryId")
    suspend fun getTemplateCountByCategory(categoryId: Int): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TimerTemplate): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<TimerTemplate>): List<Long>
    
    @Update
    suspend fun updateTemplate(template: TimerTemplate)
    
    @Delete
    suspend fun deleteTemplate(template: TimerTemplate)
    
    @Query("DELETE FROM timer_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: Int)
    
    @Query("UPDATE timer_templates SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsageCount(id: Int)
    
    @Query("UPDATE timer_templates SET rating = :rating WHERE id = :id")
    suspend fun updateRating(id: Int, rating: Float)
    
    @Query("UPDATE timer_templates SET isActive = :isActive WHERE id = :id")
    suspend fun updateActiveState(id: Int, isActive: Boolean)
    
    @Query("UPDATE timer_templates SET isRunning = :isRunning, remainingSeconds = :remainingSeconds WHERE id = :id")
    suspend fun updateTimerState(id: Int, isRunning: Boolean, remainingSeconds: Int)
    
    @Query("SELECT * FROM timer_templates WHERE name LIKE :query OR description LIKE :query ORDER BY usageCount DESC")
    suspend fun searchTemplates(query: String): List<TimerTemplate>
    
    // 카테고리별 통계 (TimerCategory 테이블과 조인)
    @Query("""
        SELECT c.name as category, COUNT(t.id) as count, SUM(t.usageCount) as totalUsage, AVG(CAST(t.usageCount AS REAL)) as avgUsage
        FROM timer_categories c 
        LEFT JOIN timer_templates t ON c.id = t.categoryId 
        GROUP BY c.id, c.name
    """)
    suspend fun getCategoryStats(): List<com.semo.alarm.data.entities.CategoryStats>
}