package com.example.android.interviewassistant.domain

import com.example.android.interviewassistant.data.local.AppDatabase
import com.example.android.interviewassistant.data.remote.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MemoryBuilder(private val db: AppDatabase) {

    private companion object {
        const val MEMORY_DEPTH = 5
    }

    /**
     * Reads local Room data and assembles the context-build request payload.
     * Must be called from a coroutine (suspend).
     */
    suspend fun buildContextRequest(): BuildContextRequest {
        val profile = db.userProfileDao().getProfile().first()
            ?: throw IllegalStateException("No user profile found — navigate to onboarding")

        val recentMemories = db.episodicMemoryDao()
            .getRecent(MEMORY_DEPTH)
            .map { it.summary }

        val allScores = db.sessionDao().getAllScores().first()

        // Aggregate historical scores per topic (oldest -> newest)
        val topicPerformance = allScores
            .groupBy { it.topic }
            .map { (topic, scores) ->
                TopicPerformanceDto(
                    topic = topic,
                    scores = scores.sortedBy { it.createdAt }.map { it.average }
                )
            }

        // Aggregate study activity from flashcard ease factors
        val allCards = db.flashcardDao().getAll().first()
        val studyActivity = allCards
            .filter { it.lastReviewed != null }
            .groupBy { it.topic }
            .map { (topic, cards) ->
                StudyActivityDto(
                    topic = topic,
                    times_asked = cards.count { it.repetitions > 0 },
                    avg_self_grade = cards.map { it.easeFactor.toFloat() }.average().toFloat()
                )
            }

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val interviewDateStr = dateFormatter.format(Date(profile.interviewDate))

        return BuildContextRequest(
            user_profile = mapOf(
                "name" to profile.name,
                "role" to profile.role,
                "level" to profile.level,
                "target" to profile.targetCompany,
                "interview_date" to interviewDateStr
            ),
            topic_scores = topicPerformance,
            episodic_memories = recentMemories,
            faq_activity = studyActivity
        )
    }

    /**
     * Selects up to 5 relevant flashcards for a given topic to send with study questions.
     */
    suspend fun getRelevantFlashcards(topic: String?): List<FlashcardRefDto> {
        val cards = if (topic.isNullOrBlank()) {
            db.flashcardDao().getRecentlyReviewed(5)
        } else {
            db.flashcardDao().getAll().first()
                .filter { it.topic.contains(topic, ignoreCase = true) }
                .sortedByDescending { it.lastReviewed ?: 0L }
                .take(5)
        }
        return cards.map { FlashcardRefDto(it.question, it.answer, it.topic) }
    }
}
