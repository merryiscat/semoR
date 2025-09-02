package com.semo.alarm.data.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IntervalTimer(
    val template: TimerTemplate,
    val rounds: List<TimerRound>,
    val totalCycles: Int = 1,          // 전체 사이클 반복 횟수
    val restBetweenCycles: Int = 0,    // 사이클 간 휴식 시간 (초)
    val currentRound: Int = 0,         // 현재 실행 중인 라운드 (0부터 시작)
    val currentCycle: Int = 0,         // 현재 사이클 (0부터 시작)
    val isRunning: Boolean = false,    // 실행 중인지 여부
    val isPaused: Boolean = false,     // 일시정지 상태
    val remainingTime: Int = 0         // 현재 라운드의 남은 시간
) : Parcelable {
    
    /**
     * 전체 예상 소요 시간 계산 (초)
     */
    fun getTotalEstimatedTime(): Int {
        val singleCycleTime = rounds.sumOf { it.duration * it.cycles }
        val totalRestTime = if (totalCycles > 1) (totalCycles - 1) * restBetweenCycles else 0
        return (singleCycleTime * totalCycles) + totalRestTime
    }
    
    /**
     * 현재 진행률 계산 (0.0 ~ 1.0)
     */
    fun getProgress(): Float {
        val totalTime = getTotalEstimatedTime()
        if (totalTime == 0) return 0f
        
        val elapsedTime = calculateElapsedTime()
        return (elapsedTime.toFloat() / totalTime).coerceIn(0f, 1f)
    }
    
    /**
     * 현재까지 경과 시간 계산
     */
    private fun calculateElapsedTime(): Int {
        // 완료된 사이클들의 시간
        val completedCyclesTime = currentCycle * rounds.sumOf { it.duration * it.cycles }
        
        // 현재 사이클에서 완료된 라운드들의 시간
        val completedRoundsTime = rounds.take(currentRound).sumOf { it.duration * it.cycles }
        
        // 현재 라운드에서 경과한 시간
        val currentRoundElapsed = if (currentRound < rounds.size) {
            rounds[currentRound].duration - remainingTime
        } else 0
        
        // 사이클 간 휴식 시간
        val restTime = currentCycle * restBetweenCycles
        
        return completedCyclesTime + completedRoundsTime + currentRoundElapsed + restTime
    }
    
    /**
     * 현재 라운드 정보 가져오기
     */
    fun getCurrentRound(): TimerRound? {
        return if (currentRound < rounds.size) rounds[currentRound] else null
    }
    
    /**
     * 다음 라운드 정보 가져오기
     */
    fun getNextRound(): TimerRound? {
        val nextIndex = currentRound + 1
        return if (nextIndex < rounds.size) {
            rounds[nextIndex]
        } else if (currentCycle + 1 < totalCycles) {
            // 다음 사이클의 첫 번째 라운드
            rounds.firstOrNull()
        } else {
            null
        }
    }
    
    /**
     * 타이머가 완료되었는지 확인
     */
    fun isCompleted(): Boolean {
        return currentCycle >= totalCycles && currentRound >= rounds.size
    }
    
    /**
     * 다음 라운드로 진행
     */
    fun moveToNextRound(): IntervalTimer {
        return if (currentRound + 1 < rounds.size) {
            // 같은 사이클 내 다음 라운드
            copy(
                currentRound = currentRound + 1,
                remainingTime = rounds[currentRound + 1].duration
            )
        } else if (currentCycle + 1 < totalCycles) {
            // 다음 사이클의 첫 번째 라운드
            copy(
                currentRound = 0,
                currentCycle = currentCycle + 1,
                remainingTime = rounds[0].duration
            )
        } else {
            // 완료
            copy(
                isRunning = false,
                isPaused = false
            )
        }
    }
    
    /**
     * 타이머 시작
     */
    fun start(): IntervalTimer {
        return copy(
            isRunning = true,
            isPaused = false,
            remainingTime = if (remainingTime == 0 && rounds.isNotEmpty()) rounds[0].duration else remainingTime
        )
    }
    
    /**
     * 타이머 일시정지
     */
    fun pause(): IntervalTimer {
        return copy(isRunning = false, isPaused = true)
    }
    
    /**
     * 타이머 재시작
     */
    fun resume(): IntervalTimer {
        return copy(isRunning = true, isPaused = false)
    }
    
    /**
     * 타이머 중단
     */
    fun stop(): IntervalTimer {
        return copy(
            isRunning = false,
            isPaused = false,
            currentRound = 0,
            currentCycle = 0,
            remainingTime = if (rounds.isNotEmpty()) rounds[0].duration else 0
        )
    }
    
    /**
     * 시간 감소 (1초)
     */
    fun tick(): IntervalTimer {
        return if (remainingTime > 0) {
            copy(remainingTime = remainingTime - 1)
        } else {
            moveToNextRound()
        }
    }
}