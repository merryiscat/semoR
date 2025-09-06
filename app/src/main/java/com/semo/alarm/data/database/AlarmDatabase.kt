package com.semo.alarm.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.semo.alarm.data.dao.AlarmDao
import com.semo.alarm.data.dao.TimerTemplateDao
import com.semo.alarm.data.dao.TimerRoundDao
import com.semo.alarm.data.dao.TimerCategoryDao
import com.semo.alarm.data.dao.SleepRecordDao
import com.semo.alarm.data.dao.ReportDao
import com.semo.alarm.data.entities.Alarm
import com.semo.alarm.data.entities.TimerTemplate
import com.semo.alarm.data.entities.TimerRound
import com.semo.alarm.data.entities.TimerCategory
import com.semo.alarm.data.entities.SleepRecord
import com.semo.alarm.data.entities.ReportData

@Database(
    entities = [Alarm::class, TimerTemplate::class, TimerRound::class, TimerCategory::class, SleepRecord::class, ReportData::class],
    version = 10,
    exportSchema = false
)
abstract class AlarmDatabase : RoomDatabase() {
    
    abstract fun alarmDao(): AlarmDao
    abstract fun timerTemplateDao(): TimerTemplateDao
    abstract fun timerRoundDao(): TimerRoundDao
    abstract fun timerCategoryDao(): TimerCategoryDao
    abstract fun sleepRecordDao(): SleepRecordDao
    abstract fun reportDao(): ReportDao
    
    companion object {
        @Volatile
        private var INSTANCE: AlarmDatabase? = null
        
        // 🔧 마이그레이션 복잡성 제거: Room이 엔티티 기반으로 자동 생성
        // fallbackToDestructiveMigration()으로 깨끗한 스키마 보장
        
        fun getDatabase(context: Context): AlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "alarm_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}