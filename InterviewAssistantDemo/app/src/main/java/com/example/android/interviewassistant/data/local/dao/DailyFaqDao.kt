package com.example.android.interviewassistant.data.local.dao

import androidx.room.*
import com.example.android.interviewassistant.data.local.entity.DailyFaqEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyFaqDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(faqs: List<DailyFaqEntity>)

    @Update
    suspend fun update(faq: DailyFaqEntity)

    @Query("SELECT * FROM daily_faqs ORDER BY fetchedAt DESC")
    fun getAll(): Flow<List<DailyFaqEntity>>

    @Query("SELECT * FROM daily_faqs WHERE fetchedAt >= :since ORDER BY fetchedAt DESC")
    suspend fun getSince(since: Long): List<DailyFaqEntity>

    @Query("SELECT COUNT(*) FROM daily_faqs WHERE fetchedAt >= :since")
    suspend fun countSince(since: Long): Int
}
