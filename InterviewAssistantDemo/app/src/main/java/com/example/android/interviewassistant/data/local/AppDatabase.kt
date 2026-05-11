package com.example.android.interviewassistant.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.android.interviewassistant.data.local.dao.*
import com.example.android.interviewassistant.data.local.entity.*

@Database(
    entities = [
        UserProfileEntity::class,
        SessionEntity::class,
        MessageEntity::class,
        ScoreEntity::class,
        EpisodicMemoryEntity::class,
        FlashcardEntity::class,
        DailyFaqEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun sessionDao(): SessionDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun episodicMemoryDao(): EpisodicMemoryDao
    abstract fun dailyFaqDao(): DailyFaqDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_faqs` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `topic` TEXT NOT NULL,
                        `question` TEXT NOT NULL,
                        `answer` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `fetchedAt` INTEGER NOT NULL,
                        `savedAsFlashcard` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "interview_assistant_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
