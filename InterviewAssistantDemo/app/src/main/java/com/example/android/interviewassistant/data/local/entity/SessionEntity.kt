package com.example.android.interviewassistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val role: String,
    val level: String,
    val domain: String,
    val status: String,
    val createdAt: Long,
    val endedAt: Long? = null,
    val overallScore: Float? = null,
    val summary: String? = null,
    val weakSpots: String? = null
)
