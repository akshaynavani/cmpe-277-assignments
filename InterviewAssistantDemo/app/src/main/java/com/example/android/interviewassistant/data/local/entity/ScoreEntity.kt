package com.example.android.interviewassistant.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scores",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class ScoreEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val topic: String,
    val clarity: Float,
    val correctness: Float,
    val communication: Float,
    val edgeCases: Float,
    val feedback: String,
    val createdAt: Long
) {
    val average: Float get() = (clarity + correctness + communication + edgeCases) / 4f
}
