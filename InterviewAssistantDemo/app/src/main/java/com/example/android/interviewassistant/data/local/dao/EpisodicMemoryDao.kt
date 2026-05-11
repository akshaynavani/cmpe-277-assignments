package com.example.android.interviewassistant.data.local.dao

import androidx.room.*
import com.example.android.interviewassistant.data.local.entity.EpisodicMemoryEntity

@Dao
interface EpisodicMemoryDao {
    @Insert
    suspend fun insert(memory: EpisodicMemoryEntity)

    @Query("SELECT * FROM episodic_memory ORDER BY createdAt DESC LIMIT :n")
    suspend fun getRecent(n: Int): List<EpisodicMemoryEntity>
}
