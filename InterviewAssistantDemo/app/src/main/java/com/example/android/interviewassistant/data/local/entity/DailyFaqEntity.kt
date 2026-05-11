package com.example.android.interviewassistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_faqs")
data class DailyFaqEntity(
    @PrimaryKey val id: String,
    val topic: String,
    val question: String,
    val answer: String,
    val source: String,         // "glassdoor", "leetcode", "interviewbit", "internet"
    val fetchedAt: Long,        // epoch millis — used to group by day
    val savedAsFlashcard: Boolean = false
)
