package com.semo.alarm.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.semo.alarm.data.dao.AlarmDao
import com.semo.alarm.data.dao.AlarmHistoryDao
import com.semo.alarm.data.dao.TimerTemplateDao
import com.semo.alarm.data.dao.TimerRoundDao
import com.semo.alarm.data.dao.TimerCategoryDao
import com.semo.alarm.data.dao.SleepRecordDao
import com.semo.alarm.data.dao.ReportDao
import com.semo.alarm.data.database.AlarmDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAlarmDatabase(@ApplicationContext context: Context): AlarmDatabase {
        return AlarmDatabase.getDatabase(context)
    }
    
    @Provides
    fun provideAlarmDao(database: AlarmDatabase): AlarmDao {
        return database.alarmDao()
    }
    
    @Provides
    fun provideAlarmHistoryDao(database: AlarmDatabase): AlarmHistoryDao {
        return database.alarmHistoryDao()
    }
    
    @Provides
    fun provideTimerTemplateDao(database: AlarmDatabase): TimerTemplateDao {
        return database.timerTemplateDao()
    }
    
    @Provides
    fun provideTimerRoundDao(database: AlarmDatabase): TimerRoundDao {
        return database.timerRoundDao()
    }
    
    @Provides
    fun provideTimerCategoryDao(database: AlarmDatabase): TimerCategoryDao {
        return database.timerCategoryDao()
    }
    
    @Provides
    fun provideSleepRecordDao(database: AlarmDatabase): SleepRecordDao {
        return database.sleepRecordDao()
    }
    
    @Provides
    fun provideReportDao(database: AlarmDatabase): ReportDao {
        return database.reportDao()
    }
    
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("semo_alarm_prefs", Context.MODE_PRIVATE)
    }
}