package com.example.android.interviewassistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val question: String,
    val answer: String,
    val topic: String,
    val source: String,
    val createdAt: Long,
    val easeFactor: Double = 2.5,
    val interval: Int = 1,
    val repetitions: Int = 0,
    val nextReview: Long,
    val lastReviewed: Long? = null
) {
    val isDueToday: Boolean get() = nextReview <= System.currentTimeMillis()
}
