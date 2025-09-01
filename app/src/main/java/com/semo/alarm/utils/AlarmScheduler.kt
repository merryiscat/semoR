package com.semo.alarm.utils

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.semo.alarm.data.entities.Alarm
import com.semo.alarm.receivers.AlarmReceiver
import java.util.*

class AlarmScheduler(private val context: Context) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarm(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm", alarm)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        
        if (alarm.isRepeating()) {
            scheduleRepeatingAlarm(alarm, pendingIntent)
        } else {
            scheduleOnceAlarm(alarm, pendingIntent)
        }
    }
    
    private fun scheduleOnceAlarm(alarm: Alarm, pendingIntent: PendingIntent) {
        val calendar = getNextAlarmTime(alarm)
        
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
                else -> {
                    @Suppress("DEPRECATION")
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            }
        } catch (e: SecurityException) {
            // 정확한 알람 권한이 없는 경우 일반 알람으로 대체
            try {
                @Suppress("DEPRECATION")
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun scheduleRepeatingAlarm(alarm: Alarm, pendingIntent: PendingIntent) {
        if (alarm.isDailyAlarm()) {
            // 매일 반복
            val calendar = getNextAlarmTime(alarm)
            scheduleRepeatingAlarmWithInterval(alarm, calendar, AlarmManager.INTERVAL_DAY, pendingIntent)
        } else {
            // 요일별 반복 - 각 요일마다 개별 알람 설정
            val selectedDays = alarm.getDaysAsList()
            selectedDays.forEach { day ->
                val dayAlarm = alarm.copy(id = alarm.id + getDayOffset(day))
                scheduleWeeklyAlarm(dayAlarm, day, pendingIntent)
            }
        }
    }
    
    private fun scheduleWeeklyAlarm(alarm: Alarm, day: String, pendingIntent: PendingIntent) {
        val calendar = getNextAlarmTimeForDay(alarm, day)
        scheduleRepeatingAlarmWithInterval(alarm, calendar, 7 * AlarmManager.INTERVAL_DAY, pendingIntent)
    }
    
    private fun scheduleRepeatingAlarmWithInterval(
        alarm: Alarm,
        calendar: Calendar,
        interval: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        interval,
                        pendingIntent
                    )
                }
                else -> {
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        interval,
                        pendingIntent
                    )
                }
            }
        } catch (e: SecurityException) {
            // 권한 문제로 반복 알람을 설정할 수 없는 경우 일회성 알람으로 대체
            scheduleOnceAlarm(
                alarm = Alarm(
                    id = alarm.id,
                    time = "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}",
                    label = alarm.label
                ),
                pendingIntent = pendingIntent
            )
        }
    }
    
    fun cancelAlarm(alarmId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        
        alarmManager.cancel(pendingIntent)
    }
    
    private fun getNextAlarmTime(alarm: Alarm): Calendar {
        val calendar = Calendar.getInstance().apply {
            val (hour, minute) = alarm.getTimeAsHourMinute()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // 현재 시간보다 이전이면 다음 날로 설정
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return calendar
    }
    
    private fun getNextAlarmTimeForDay(alarm: Alarm, day: String): Calendar {
        val calendar = Calendar.getInstance().apply {
            val (hour, minute) = alarm.getTimeAsHourMinute()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val targetDayOfWeek = getDayOfWeek(day)
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        var daysToAdd = targetDayOfWeek - currentDayOfWeek
        if (daysToAdd < 0) {
            daysToAdd += 7
        } else if (daysToAdd == 0 && calendar.timeInMillis <= System.currentTimeMillis()) {
            daysToAdd = 7
        }
        
        calendar.add(Calendar.DAY_OF_MONTH, daysToAdd)
        return calendar
    }
    
    private fun getDayOfWeek(day: String): Int {
        return when (day) {
            "sun" -> Calendar.SUNDAY
            "mon" -> Calendar.MONDAY
            "tue" -> Calendar.TUESDAY
            "wed" -> Calendar.WEDNESDAY
            "thu" -> Calendar.THURSDAY
            "fri" -> Calendar.FRIDAY
            "sat" -> Calendar.SATURDAY
            else -> Calendar.MONDAY
        }
    }
    
    private fun getDayOffset(day: String): Int {
        return when (day) {
            "sun" -> 0
            "mon" -> 1
            "tue" -> 2
            "wed" -> 3
            "thu" -> 4
            "fri" -> 5
            "sat" -> 6
            else -> 0
        }
    }
}