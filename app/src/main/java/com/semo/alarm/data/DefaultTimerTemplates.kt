package com.semo.alarm.data

import com.semo.alarm.data.entities.TimerTemplate
import com.semo.alarm.data.entities.TimerRound

object DefaultTimerTemplates {
    
    // 기본 카테고리 ID (하나만)
    const val DEFAULT_CATEGORY_ID = 1
    
    // 기본 템플릿들 (기본 카테고리에만)
    fun getAllDefaultTemplates(): List<Pair<TimerTemplate, List<TimerRound>>> {
        return getBasicTemplates()
    }
    
    // 기본 카테고리의 템플릿들
    fun getBasicTemplates(): List<Pair<TimerTemplate, List<TimerRound>>> {
        return listOf(
            createPomodoroTemplate(),
            createSimpleTimerTemplate(),
            createBreakTimerTemplate()
        )
    }
    
    // === 기본 템플릿 구현 ===
    private fun createSimpleTimerTemplate(): Pair<TimerTemplate, List<TimerRound>> {
        val template = TimerTemplate(
            categoryId = DEFAULT_CATEGORY_ID,
            name = "단순 타이머",
            description = "5분 기본 타이머",
            totalDuration = 300, // 5분
            isDefault = true,
            createdBy = "system"
        )
        
        val rounds = listOf(
            TimerRound(templateId = 0, roundIndex = 0, name = "타이머", duration = 300, color = "#3B82F6")
        )
        
        return template to rounds
    }
    
    private fun createBreakTimerTemplate(): Pair<TimerTemplate, List<TimerRound>> {
        val template = TimerTemplate(
            categoryId = DEFAULT_CATEGORY_ID,
            name = "짧은 휴식",
            description = "10분 휴식 타이머",
            totalDuration = 600, // 10분
            isDefault = true,
            createdBy = "system"
        )
        
        val rounds = listOf(
            TimerRound(templateId = 0, roundIndex = 0, name = "휴식", duration = 600, color = TimerRound.COLOR_REST)
        )
        
        return template to rounds
    }
    
    // === 기본 템플릿 구현 지속 ===
    private fun createPomodoroTemplate(): Pair<TimerTemplate, List<TimerRound>> {
        val template = TimerTemplate(
            categoryId = DEFAULT_CATEGORY_ID,
            name = "뽀모도로",
            description = "25분 집중 + 5분 휴식 × 4사이클 집중 학습법",
            totalDuration = 7200, // 2시간
            isDefault = true,
            createdBy = "system"
        )
        
        val rounds = listOf(
            TimerRound(templateId = 0, roundIndex = 0, name = "집중", duration = 1500, cycles = 4, color = "#10B981"), // 25분
            TimerRound(templateId = 0, roundIndex = 1, name = "짧은 휴식", duration = 300, cycles = 3, color = TimerRound.COLOR_REST), // 5분
            TimerRound(templateId = 0, roundIndex = 2, name = "긴 휴식", duration = 1800, color = TimerRound.COLOR_COMPLETE) // 30분
        )
        
        return template to rounds
    }
}