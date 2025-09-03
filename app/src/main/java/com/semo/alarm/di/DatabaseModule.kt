package com.semo.alarm.di

import android.content.Context
import androidx.room.Room
import com.semo.alarm.data.dao.AlarmDao
import com.semo.alarm.data.dao.TimerTemplateDao
import com.semo.alarm.data.dao.TimerRoundDao
import com.semo.alarm.data.dao.TimerCategoryDao
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
}