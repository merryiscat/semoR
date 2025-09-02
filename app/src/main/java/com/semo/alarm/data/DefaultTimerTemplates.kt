package com.semo.alarm.data

import com.semo.alarm.data.entities.TimerTemplate
import com.semo.alarm.data.entities.TimerRound

object DefaultTimerTemplates {
    
    // 카테고리 ID 상수 (데이터베이스 마이그레이션에서 생성된 ID와 일치)
    const val EXERCISE_CATEGORY_ID = 1
    const val COOKING_CATEGORY_ID = 2
    const val STUDY_CATEGORY_ID = 3
    const val DRINK_CATEGORY_ID = 4
    
    // 모든 기본 템플릿 반환
    fun getAllDefaultTemplates(): List<Pair<TimerTemplate, List<TimerRound>>> {
        return getExerciseTemplates() + getCookingTemplates() + getStudyTemplates() + getDrinkTemplates()
    }
    
    // 운동 카테고리 템플릿들
    fun getExerciseTemplates(): List<Pair<TimerTemplate, List<TimerRound>>> {
        return listOf(
            createTabataTemplate(),
            createHiitTemplate(),
            createStrengthTemplate()
        )
    }
    
    // 요리 카테고리 템플릿들
    fun getCookingTemplates(): List<Pair<TimerTemplate, List<TimerRound>>> {
        return listOf(
            createEggTemplate(),
            createPastaTemplate(),
            createRiceTemplate()
        )
    }
    
    // 학습 카테고리 템플릿들
    fun getStudyTemplates(): List<Pair<TimerTemplate, List<TimerRound>>> {
        return listOf(
            createPomodoroTemplate(),
            createFocusTemplate()
        )
    }
    
    // 음료 카테고리 템플릿들
    fun getDrinkTemplates(): List<Pair<TimerTemplate, List<TimerRound>>> {
        return listOf(
            createCoffeeTemplate(),
            createTeaTemplate()
        )
    }
    
    // === 운동 템플릿 구현 ===
    private fun createTabataTemplate(): Pair<TimerTemplate, List<TimerRound>> {
        val template = TimerTemplate(
            categoryId = EXERCISE_CATEGORY_ID,
            name = "타바타",
            description = "20초 운동, 10초 휴식 × 8라운드 고강도 인터벌 트레이닝",
            totalDuration = 240, // 4분
            isDefault = true,
            createdBy = "system"
        )
        
        val rounds = listOf(
            TimerRound(templateId = 0, roundIndex = 0, name = "준비", duration = 10, color = TimerRound.COLOR_PREPARE),
            TimerRound(templateId = 0, roundIndex = 1, name = "운동", duration = 20, cycles = 8, color = TimerRound.COLOR_EXERCISE),
            TimerRound(templateId = 0, roundIndex = 2, name = "휴식", duration = 10, cycles = 7, color = TimerRound.COLOR_REST),
            TimerRound(templateId = 0, roundIndex = 3, name = "완료", duration = 30, color = TimerRound.COLOR_COMPLETE)
        )
        
        return template to rounds
    }
    
    private fun createHiitTemplate(): Pair<TimerTemplate, List<TimerRound>> {
        val template = TimerTemplate(
            categoryId = EXERCISE_CATEGORY_ID,
            name = "HIIT",
            description = "45초 운동, 15초 휴식 × 6라운드 고강도 인터벌 트레이닝",
            totalDuration = 360, // 6분
            isDefault = true,
            createdBy = "system"
        )
        
        val rounds = listOf(
            TimerRound(templateId = 0, roundIndex = 0, name = "준비", duration = 15, color = TimerRound.COLOR_PREPARE),
            TimerRound(templateId = 0, roundIndex = 1, name = "운동", duration = 45, cycles = 6, color = TimerRound.COLOR_EXERCISE),
            TimerRound(templateId = 0, roundIndex = 2, name = "휴식", duration = 15, cycles = 5, color = TimerRound.COLOR_REST),
            TimerRound(templateId = 0, roundIndex = 3, name = "완료", duration = 60, color = TimerRound.COLOR_COMPLETE)
        )
        
        return template to rounds
    }
    
    private fun createStrengthTemplate(): Pair<TimerTemplate, List<TimerRound>> {
        val template = TimerTemplate(
            categoryId = EXERCISE_CATEGORY_ID,
            name = "근력운동",
            description = "60초 운동, 90초 휴식 × 4세트 근력 트레이닝",
            totalDuration = 600, // 10분
            isDefault = true,
            createdBy = "system"
        )
        
        val rounds = listOf(
            TimerRound(templateId = 0, roundIndex = 0, name = "준비", duration = 30, color = TimerRound.COLOR_PREPARE),
            TimerRound(templateId = 0, roundIndex = 1, name = "운동", duration = 60, cycles = 4, color = TimerRound.COLOR_EXERCISE),
            TimerRound(templateId = 0, roundIndex = 2, name = "휴식", duration = 90, cycles = 3, color = TimerRound.COLOR_REST),
            TimerRound(templateId = 0, roundIndex = 3, name = "완료", duration = 60, color = TimerRound.COLOR_COMPLETE)
        )
        
        return template to rounds
    }
    
    // === 요리 템플릿 구현 ===
    private fun createEggTemplate(): Pair<TimerTemplate, List<TimerRound>> {
        val template = TimerTemplate(
            categoryId = COOKING_CATEGORY_ID,
            name = "달걀 요리",
            description = "반숙 6분, 완숙 8분 달걀 삶기 타이머",
            totalDuration = 480, // 8분
            isDefault = true,
            createdBy = "system"
        )
        
        val rounds = listOf(
            TimerRound(templateId = 0, roundIndex = 0, name = "물 끓이기", duration = 120, color = TimerRound.COLOR_PREPARE),
            TimerRound(templateId = 0, roundIndex = 1, name = "반숙", duration = 360, color = "#F59E0B"),
            TimerRound(templateId = 0, roundIndex = 2, name = "완숙", duration = 120, color = TimerRound.COLOR_COMPLETE)
        )
        
        return template to rounds
    }
    
    private fun createPastaTemplate(): Pair<TimerTemplate, List<TimerRound>> {
        val template = TimerTemplate(
            categoryId = COOKING_CATEGORY_ID,
            name = "파스타",
            description = "8-12분 파스타 삶기 타이머",
            totalDuration = 720, // 12분
            isDefault = true,
            createdBy = "system"
        )
        
        val rounds = listOf(
            TimerRound(templateId = 0, roundIndex = 0, name = "물 끓이기", duration = 240, color = TimerRound.COLOR_PREPARE),
            TimerRound(templateId = 0, roundIndex = 1, name = "파스타 삶기", duration = 480, color = "#F59E0B"),
            TimerRound(templateId = 0, roundIndex = 2, name = "완료", duration = 0, color = TimerRound.COLOR_COMPLETE)
        )
        
        return template to rounds
    }
    
    private fun createRiceTemplate(): Pair<TimerTemplate, List<TimerRound>> {
        val template = TimerTemplate(
            categoryId = COOKING_CATEGORY_ID,
            name = "밥 짓기",
            description = "20분 밥 짓기 + 10분 뜸 들이기",
            totalDuration = 1800, // 30분
            isDefault = true,
            createdBy = "system"
        )
        
        val rounds = listOf(
            TimerRound(templateId = 0, roundIndex = 0, name = "밥 짓기", duration = 1200, color = "#F59E0B"),
            TimerRound(templateId = 0, roundIndex = 1, name = "뜸 들이기", duration = 600, color = TimerRound.COLOR_REST),
            TimerRound(templateId = 0, roundIndex = 2, name = "완료", duration = 0, color = TimerRound.COLOR_COMPLETE)
        )
        
        return template to rounds
    }
    
    // === 학습 템플릿 구현 ===
    private fun createPomodoroTemplate(): Pair<TimerTemplate, List<TimerRound>> {
        val template = TimerTemplate(
            categoryId = STUDY_CATEGORY_ID,
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
    
    private fun createFocusTemplate(): Pair<TimerTemplate, List<TimerRound>> {
        val template = TimerTemplate(
            categoryId = STUDY_CATEGORY_ID,
            name = "집중 학습",
            description = "50분 집중 + 10분 휴식 딥워크 세션",
            totalDuration = 3600, // 1시간
            isDefault = true,
            createdBy = "system"
        )
        
        val rounds = listOf(
            TimerRound(templateId = 0, roundIndex = 0, name = "집중", duration = 3000, color = "#10B981"), // 50분
            TimerRound(templateId = 0, roundIndex = 1, name = "휴식", duration = 600, color = TimerRound.COLOR_REST) // 10분
        )
        
        return template to rounds
    }
    
    // === 음료 템플릿 구현 ===
    private fun createCoffeeTemplate(): Pair<TimerTemplate, List<TimerRound>> {
        val template = TimerTemplate(
            categoryId = DRINK_CATEGORY_ID,
            name = "커피 드립",
            description = "4분 드립 커피 추출 타이머",
            totalDuration = 240, // 4분
            isDefault = true,
            createdBy = "system"
        )
        
        val rounds = listOf(
            TimerRound(templateId = 0, roundIndex = 0, name = "뜸 들이기", duration = 30, color = TimerRound.COLOR_PREPARE),
            TimerRound(templateId = 0, roundIndex = 1, name = "1차 추출", duration = 60, color = "#8B5CF6"),
            TimerRound(templateId = 0, roundIndex = 2, name = "2차 추출", duration = 90, color = "#8B5CF6"),
            TimerRound(templateId = 0, roundIndex = 3, name = "마무리", duration = 60, color = TimerRound.COLOR_COMPLETE)
        )
        
        return template to rounds
    }
    
    private fun createTeaTemplate(): Pair<TimerTemplate, List<TimerRound>> {
        val template = TimerTemplate(
            categoryId = DRINK_CATEGORY_ID,
            name = "차 우리기",
            description = "3분 홍차 우리기 타이머",
            totalDuration = 180, // 3분
            isDefault = true,
            createdBy = "system"
        )
        
        val rounds = listOf(
            TimerRound(templateId = 0, roundIndex = 0, name = "차 우리기", duration = 180, color = "#8B5CF6"),
            TimerRound(templateId = 0, roundIndex = 1, name = "완료", duration = 0, color = TimerRound.COLOR_COMPLETE)
        )
        
        return template to rounds
    }
}