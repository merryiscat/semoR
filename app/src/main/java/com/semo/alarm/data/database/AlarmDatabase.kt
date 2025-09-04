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
import com.semo.alarm.data.entities.Alarm
import com.semo.alarm.data.entities.TimerTemplate
import com.semo.alarm.data.entities.TimerRound
import com.semo.alarm.data.entities.TimerCategory

@Database(
    entities = [Alarm::class, TimerTemplate::class, TimerRound::class, TimerCategory::class],
    version = 5,
    exportSchema = false
)
abstract class AlarmDatabase : RoomDatabase() {
    
    abstract fun alarmDao(): AlarmDao
    abstract fun timerTemplateDao(): TimerTemplateDao
    abstract fun timerRoundDao(): TimerRoundDao
    abstract fun timerCategoryDao(): TimerCategoryDao
    
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
        
        fun getDatabase(context: Context): AlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "alarm_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}