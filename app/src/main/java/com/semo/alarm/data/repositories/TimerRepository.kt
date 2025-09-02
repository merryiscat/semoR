package com.semo.alarm.data.repositories

import androidx.lifecycle.LiveData
import com.semo.alarm.data.dao.TimerTemplateDao
import com.semo.alarm.data.dao.TimerRoundDao
import com.semo.alarm.data.entities.IntervalTimer
import com.semo.alarm.data.entities.TimerTemplate
import com.semo.alarm.data.entities.TimerRound
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerRepository @Inject constructor(
    private val templateDao: TimerTemplateDao,
    private val roundDao: TimerRoundDao
) {
    
    // Template operations
    fun getAllTemplates(): LiveData<List<TimerTemplate>> {
        return templateDao.getAllTemplates()
    }
    
    fun getTemplatesByCategory(categoryId: Int): LiveData<List<TimerTemplate>> {
        return templateDao.getTemplatesByCategory(categoryId)
    }
    
    fun getDefaultTemplates(): LiveData<List<TimerTemplate>> {
        return templateDao.getDefaultTemplates()
    }
    
    fun getCustomTemplates(): LiveData<List<TimerTemplate>> {
        return templateDao.getCustomTemplates()
    }
    
    suspend fun getTemplateById(id: Int): TimerTemplate? {
        return templateDao.getTemplateById(id)
    }
    
    suspend fun getMostUsedTemplates(limit: Int = 5): List<TimerTemplate> {
        return templateDao.getMostUsedTemplates(limit)
    }
    
    suspend fun getRecentTemplates(limit: Int = 5): List<TimerTemplate> {
        return templateDao.getRecentTemplates(limit)
    }
    
    suspend fun insertTemplate(template: TimerTemplate): Long {
        return templateDao.insertTemplate(template)
    }
    
    suspend fun insertTemplateWithRounds(template: TimerTemplate, rounds: List<TimerRound>): Long {
        val templateId = templateDao.insertTemplate(template)
        val roundsWithTemplateId = rounds.mapIndexed { index, round ->
            round.copy(templateId = templateId.toInt(), roundIndex = index)
        }
        roundDao.insertRounds(roundsWithTemplateId)
        return templateId
    }
    
    suspend fun updateTemplate(template: TimerTemplate) {
        templateDao.updateTemplate(template)
    }
    
    suspend fun deleteTemplate(template: TimerTemplate) {
        templateDao.deleteTemplate(template)
    }
    
    suspend fun incrementUsageCount(templateId: Int) {
        templateDao.incrementUsageCount(templateId)
    }
    
    suspend fun searchTemplates(query: String): List<TimerTemplate> {
        return templateDao.searchTemplates("%$query%")
    }
    
    // Round operations
    suspend fun getRoundsByTemplateId(templateId: Int): List<TimerRound> {
        return roundDao.getRoundsByTemplateId(templateId)
    }
    
    fun getRoundsByTemplateIdLiveData(templateId: Int): LiveData<List<TimerRound>> {
        return roundDao.getRoundsByTemplateIdLiveData(templateId)
    }
    
    suspend fun insertRound(round: TimerRound): Long {
        return roundDao.insertRound(round)
    }
    
    suspend fun insertRounds(rounds: List<TimerRound>): List<Long> {
        return roundDao.insertRounds(rounds)
    }
    
    suspend fun updateRound(round: TimerRound) {
        roundDao.updateRound(round)
    }
    
    suspend fun deleteRound(round: TimerRound) {
        roundDao.deleteRound(round)
    }
    
    suspend fun reorderRounds(templateId: Int, roundIds: List<Int>) {
        roundDao.reorderRounds(templateId, roundIds)
    }
    
    // Combined operations
    suspend fun getIntervalTimer(templateId: Int): IntervalTimer? {
        val template = templateDao.getTemplateById(templateId) ?: return null
        val rounds = roundDao.getRoundsByTemplateId(templateId)
        
        return IntervalTimer(
            template = template,
            rounds = rounds,
            totalCycles = 1, // 기본값, 나중에 설정으로 변경 가능
            restBetweenCycles = 0,
            remainingTime = rounds.firstOrNull()?.duration ?: 0
        )
    }
    
    suspend fun createIntervalTimerFromTemplate(templateId: Int, totalCycles: Int = 1, restBetweenCycles: Int = 0): IntervalTimer? {
        val template = templateDao.getTemplateById(templateId) ?: return null
        val rounds = roundDao.getRoundsByTemplateId(templateId)
        
        return IntervalTimer(
            template = template,
            rounds = rounds,
            totalCycles = totalCycles,
            restBetweenCycles = restBetweenCycles,
            remainingTime = rounds.firstOrNull()?.duration ?: 0
        )
    }
    
    // Statistics and analytics
    suspend fun getTemplateStats(templateId: Int): com.semo.alarm.data.entities.RoundStats? {
        return roundDao.getRoundStats(templateId)
    }
    
    suspend fun getRoundTypeStats(templateId: Int): List<com.semo.alarm.data.entities.RoundTypeStats> {
        return roundDao.getRoundTypeStats(templateId)
    }
    
    suspend fun getCategoryStats(): List<com.semo.alarm.data.entities.CategoryStats> {
        return templateDao.getCategoryStats()
    }
}