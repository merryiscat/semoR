package com.semo.alarm.data.enums

enum class TimerType(val displayName: String, val description: String) {
    SIMPLE("단순 타이머", "하나의 시간만 설정하는 기본 타이머"),
    INTERVAL("인터벌 타이머", "운동과 휴식을 반복하는 다단계 타이머");
    
    companion object {
        fun fromString(value: String): TimerType {
            return values().find { it.name == value } ?: SIMPLE
        }
    }
}