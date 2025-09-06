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
        return emptyList() // 기본 템플릿 없음
    }
    
}