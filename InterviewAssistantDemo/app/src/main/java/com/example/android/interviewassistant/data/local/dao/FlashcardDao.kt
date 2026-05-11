package com.example.android.interviewassistant.data.local.dao

import androidx.room.*
import com.example.android.interviewassistant.data.local.entity.FlashcardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<FlashcardEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: FlashcardEntity)

    @Update
    suspend fun update(card: FlashcardEntity)

    @Delete
    suspend fun delete(card: FlashcardEntity)

    @Query("SELECT * FROM flashcards WHERE nextReview <= :now ORDER BY nextReview ASC")
    suspend fun getDueCards(now: Long): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards ORDER BY createdAt DESC")
    fun getAll(): Flow<List<FlashcardEntity>>

    @Query("SELECT COUNT(*) FROM flashcards WHERE nextReview <= :now")
    fun getDueCount(now: Long): Flow<Int>

    @Query("SELECT * FROM flashcards WHERE lastReviewed IS NOT NULL ORDER BY lastReviewed DESC LIMIT :limit")
    suspend fun getRecentlyReviewed(limit: Int): List<FlashcardEntity>
}
