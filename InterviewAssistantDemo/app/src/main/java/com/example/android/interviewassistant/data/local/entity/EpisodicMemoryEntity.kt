package com.example.android.interviewassistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "episodic_memory")
data class EpisodicMemoryEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val summary: String,
    val createdAt: Long
)
