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
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE alarms ADD COLUMN vibration_enabled INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE alarms ADD COLUMN silent_mode INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE alarms ADD COLUMN vibration_pattern TEXT NOT NULL DEFAULT 'default'")
            }
        }
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create timer_templates table
                database.execSQL("""
                    CREATE TABLE timer_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        category TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        iconResName TEXT NOT NULL DEFAULT '',
                        totalDuration INTEGER NOT NULL,
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        createdBy TEXT NOT NULL DEFAULT 'system',
                        rating REAL NOT NULL DEFAULT 0.0,
                        usageCount INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """)
                
                // Create timer_rounds table
                database.execSQL("""
                    CREATE TABLE timer_rounds (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        templateId INTEGER NOT NULL,
                        roundIndex INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        duration INTEGER NOT NULL,
                        soundUri TEXT NOT NULL DEFAULT '',
                        vibrationPattern TEXT NOT NULL DEFAULT '',
                        color TEXT NOT NULL DEFAULT '#3B82F6',
                        cycles INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY(templateId) REFERENCES timer_templates(id) ON DELETE CASCADE
                    )
                """)
                
                // Create index for better query performance
                database.execSQL("CREATE INDEX index_timer_rounds_templateId ON timer_rounds (templateId)")
            }
        }
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create timer_categories table
                database.execSQL("""
                    CREATE TABLE timer_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        icon TEXT NOT NULL DEFAULT '⏰',
                        color TEXT NOT NULL DEFAULT '#3B82F6',
                        description TEXT NOT NULL DEFAULT '',
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        createdBy TEXT NOT NULL DEFAULT 'user',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
                
                // Insert default categories
                val currentTime = System.currentTimeMillis()
                database.execSQL("INSERT INTO timer_categories (name, icon, color, description, isDefault, sortOrder, createdBy, createdAt, updatedAt) VALUES ('운동', '🏃', '#EF4444', '운동 및 피트니스 타이머', 1, 0, 'system', $currentTime, $currentTime)")
                database.execSQL("INSERT INTO timer_categories (name, icon, color, description, isDefault, sortOrder, createdBy, createdAt, updatedAt) VALUES ('요리', '👨‍🍳', '#F59E0B', '요리 및 베이킹 타이머', 1, 1, 'system', $currentTime, $currentTime)")
                database.execSQL("INSERT INTO timer_categories (name, icon, color, description, isDefault, sortOrder, createdBy, createdAt, updatedAt) VALUES ('학습', '📚', '#10B981', '공부 및 집중 타이머', 1, 2, 'system', $currentTime, $currentTime)")
                database.execSQL("INSERT INTO timer_categories (name, icon, color, description, isDefault, sortOrder, createdBy, createdAt, updatedAt) VALUES ('음료', '☕', '#8B5CF6', '차, 커피 우리기 타이머', 1, 3, 'system', $currentTime, $currentTime)")
                
                // Create new timer_templates table with correct schema
                database.execSQL("""
                    CREATE TABLE timer_templates_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        categoryId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        totalDuration INTEGER NOT NULL,
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        rating REAL NOT NULL DEFAULT 0.0,
                        usageCount INTEGER NOT NULL DEFAULT 0,
                        createdBy TEXT NOT NULL DEFAULT 'user',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(categoryId) REFERENCES timer_categories(id) ON DELETE CASCADE
                    )
                """)
                
                // Copy data from old table to new table with category mapping
                database.execSQL("""
                    INSERT INTO timer_templates_new (
                        id, categoryId, name, description, totalDuration, isDefault, rating, usageCount, createdBy, createdAt, updatedAt
                    )
                    SELECT 
                        id,
                        CASE category
                            WHEN 'exercise' THEN 1
                            WHEN 'cooking' THEN 2  
                            WHEN 'study' THEN 3
                            WHEN 'drink' THEN 4
                            ELSE 1
                        END as categoryId,
                        name,
                        description,
                        totalDuration,
                        isDefault,
                        rating,
                        usageCount,
                        createdBy,
                        createdAt,
                        $currentTime as updatedAt
                    FROM timer_templates
                """)
                
                // Drop old table and rename new one
                database.execSQL("DROP TABLE timer_templates")
                database.execSQL("ALTER TABLE timer_templates_new RENAME TO timer_templates")
                
                // Create foreign key index
                database.execSQL("CREATE INDEX index_timer_templates_categoryId ON timer_templates (categoryId)")
            }
        }
        
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add timerType column to timer_templates table
                database.execSQL("ALTER TABLE timer_templates ADD COLUMN timerType TEXT NOT NULL DEFAULT 'SIMPLE'")
            }
        }
        
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add isActive column to timer_templates table
                database.execSQL("ALTER TABLE timer_templates ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
            }
        }
        
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add timer runtime columns to timer_templates table
                database.execSQL("ALTER TABLE timer_templates ADD COLUMN isRunning INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE timer_templates ADD COLUMN remainingSeconds INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add sound & vibration columns to timer_templates table
                database.execSQL("ALTER TABLE timer_templates ADD COLUMN soundUri TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE timer_templates ADD COLUMN volume REAL NOT NULL DEFAULT 0.7")
                database.execSQL("ALTER TABLE timer_templates ADD COLUMN vibrationEnabled INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create sleep_records table for sleep tracking
                database.execSQL("""
                    CREATE TABLE sleep_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bedtime INTEGER NOT NULL,
                        wakeupTime INTEGER,
                        totalDuration INTEGER NOT NULL DEFAULT 0,
                        qualityScore REAL NOT NULL DEFAULT 0.0,
                        snoringDetected INTEGER NOT NULL DEFAULT 0,
                        snoringData TEXT NOT NULL DEFAULT '',
                        movementData TEXT NOT NULL DEFAULT '',
                        notes TEXT NOT NULL DEFAULT '',
                        isActive INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """)
                
                // Create index for better query performance
                database.execSQL("CREATE INDEX index_sleep_records_createdAt ON sleep_records (createdAt)")
                database.execSQL("CREATE INDEX index_sleep_records_isActive ON sleep_records (isActive)")
            }
        }
        
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create report_data table for analytics and reporting
                database.execSQL("""
                    CREATE TABLE report_data (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        alarmDismissalTime INTEGER NOT NULL DEFAULT 0,
                        snoozeCount INTEGER NOT NULL DEFAULT 0,
                        timerUsageMinutes INTEGER NOT NULL DEFAULT 0,
                        timerCompletionRate REAL NOT NULL DEFAULT 0.0,
                        sleepDuration INTEGER NOT NULL DEFAULT 0,
                        sleepQualityScore REAL NOT NULL DEFAULT 0.0,
                        snoringPercentage REAL NOT NULL DEFAULT 0.0,
                        productivityScore REAL NOT NULL DEFAULT 0.0,
                        lifestyleScore REAL NOT NULL DEFAULT 0.0,
                        createdAt INTEGER NOT NULL
                    )
                """)
                
                // Create indexes for better query performance
                database.execSQL("CREATE UNIQUE INDEX index_report_data_date ON report_data (date)")
                database.execSQL("CREATE INDEX index_report_data_createdAt ON report_data (createdAt)")
            }
        }
        
        fun getDatabase(context: Context): AlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "alarm_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}