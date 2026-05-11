package com.example.android.interviewassistant.data.local.dao

import androidx.room.*
import com.example.android.interviewassistant.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: SessionEntity)

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: String): SessionEntity?

    @Insert
    suspend fun insertMessage(msg: MessageEntity)

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessages(sessionId: String): Flow<List<MessageEntity>>

    @Insert
    suspend fun insertScore(score: ScoreEntity)

    @Query("SELECT * FROM scores WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getScoresForSession(sessionId: String): List<ScoreEntity>

    @Query("SELECT * FROM scores ORDER BY createdAt ASC")
    fun getAllScores(): Flow<List<ScoreEntity>>
}
