package com.example.android.interviewassistant.domain

import com.example.android.interviewassistant.data.local.AppDatabase
import com.example.android.interviewassistant.data.local.entity.*
import com.example.android.interviewassistant.data.remote.*
import java.util.Calendar
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

class AppRepository(
    private val db: AppDatabase,
    private val api: ApiService
) {
    private val gson = Gson()
    private val memoryBuilder = MemoryBuilder(db)

    // ── Profile ──────────────────────────────────────────────────────────────

    fun getProfile(): Flow<UserProfileEntity?> = db.userProfileDao().getProfile()

    suspend fun hasProfile(): Boolean = db.userProfileDao().count() > 0

    suspend fun saveProfile(
        name: String,
        role: String,
        level: String,
        targetCompany: String,
        interviewDate: Long
    ) {
        db.userProfileDao().upsert(
            UserProfileEntity(
                name = name,
                role = role,
                level = level,
                targetCompany = targetCompany,
                interviewDate = interviewDate
            )
        )
    }

    // ── Session ──────────────────────────────────────────────────────────────

    fun getAllSessions(): Flow<List<SessionEntity>> = db.sessionDao().getAllSessions()

    fun getSessionMessages(sessionId: String): Flow<List<MessageEntity>> =
        db.sessionDao().getMessages(sessionId)

    suspend fun addMessage(sessionId: String, role: String, content: String) {
        db.sessionDao().insertMessage(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                role = role,
                content = content,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    /**
     * Builds coaching context, then starts a new interview session.
     * Returns the opening question on success.
     */
    suspend fun startSession(role: String, level: String, domain: String): Result<Pair<String, String>> {
        return try {
            val contextReq = memoryBuilder.buildContextRequest()
            val contextResp = api.buildContext(contextReq)
            val context = contextResp.context_summary

            val sessionResp = api.beginSession(
                BeginSessionRequest(role = role, level = level, domain = domain, context = context)
            )

            val sessionId = UUID.randomUUID().toString()
            db.sessionDao().insertSession(
                SessionEntity(
                    id = sessionId,
                    role = role,
                    level = level,
                    domain = domain,
                    status = "active",
                    createdAt = System.currentTimeMillis()
                )
            )

            // Store the first interviewer message
            addMessage(sessionId, "interviewer", sessionResp.question)

            Result.Success(Pair(sessionId, context))
        } catch (e: Exception) {
            Result.Error(e.toUserMessage())
        }
    }

    /**
     * Evaluates a candidate answer. Returns scores + next question.
     */
    suspend fun evaluateAnswer(
        sessionId: String,
        role: String,
        level: String,
        domain: String,
        context: String,
        sessionDelta: String,
        history: List<ConversationMessageDto>,
        answer: String
    ): Result<EvaluateAnswerResponse> {
        return try {
            val resp = api.evaluateAnswer(
                EvaluateAnswerRequest(
                    role = role,
                    level = level,
                    domain = domain,
                    context = context,
                    session_delta = sessionDelta,
                    history = history,
                    answer = answer
                )
            )

            // Persist the score
            db.sessionDao().insertScore(
                ScoreEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    topic = resp.topic,
                    clarity = resp.scores.clarity,
                    correctness = resp.scores.correctness,
                    communication = resp.scores.communication,
                    edgeCases = resp.scores.edge_cases,
                    feedback = resp.feedback,
                    createdAt = System.currentTimeMillis()
                )
            )

            Result.Success(resp)
        } catch (e: Exception) {
            Result.Error(e.toUserMessage())
        }
    }

    /**
     * Wraps up the interview session and persists the summary.
     */
    suspend fun wrapSession(
        sessionId: String,
        role: String,
        level: String,
        context: String,
        sessionDelta: String
    ): Result<WrapSessionResponse> {
        return try {
            val scores = db.sessionDao().getScoresForSession(sessionId)
            val scorePayloads = scores.map {
                ScorePayload(
                    topic = it.topic,
                    clarity = it.clarity,
                    correctness = it.correctness,
                    communication = it.communication,
                    edge_cases = it.edgeCases
                )
            }

            val resp = api.wrapSession(
                WrapSessionRequest(
                    role = role,
                    level = level,
                    context = context,
                    session_delta = sessionDelta,
                    scores = scorePayloads
                )
            )

            // Update session record
            val session = db.sessionDao().getById(sessionId)
            if (session != null) {
                db.sessionDao().updateSession(
                    session.copy(
                        status = "completed",
                        endedAt = System.currentTimeMillis(),
                        overallScore = resp.overall_score,
                        summary = resp.summary,
                        weakSpots = gson.toJson(resp.weak_spots)
                    )
                )
            }

            // Save episodic memory
            db.episodicMemoryDao().insert(
                EpisodicMemoryEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    summary = resp.summary,
                    createdAt = System.currentTimeMillis()
                )
            )

            Result.Success(resp)
        } catch (e: Exception) {
            Result.Error(e.toUserMessage())
        }
    }

    fun getAllScores(): Flow<List<ScoreEntity>> = db.sessionDao().getAllScores()

    // ── Flashcards ───────────────────────────────────────────────────────────

    fun getAllFlashcards(): Flow<List<FlashcardEntity>> = db.flashcardDao().getAll()

    fun getDueCardCount(): Flow<Int> = db.flashcardDao().getDueCount(System.currentTimeMillis())

    suspend fun getDueFlashcards(): List<FlashcardEntity> =
        db.flashcardDao().getDueCards(System.currentTimeMillis())

    suspend fun reviewCard(card: FlashcardEntity, grade: Int): Result<Unit> {
        return try {
            val updated = SpacedRepetition.review(card, grade)
            db.flashcardDao().update(updated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to save review")
        }
    }

    suspend fun saveFlashcards(cards: List<FlashcardEntity>) {
        db.flashcardDao().insertAll(cards)
    }

    // ── Study ────────────────────────────────────────────────────────────────

    suspend fun askStudy(question: String, topic: String?): Result<AskStudyResponse> {
        return try {
            val contextReq = memoryBuilder.buildContextRequest()
            val contextResp = api.buildContext(contextReq)
            val relevantCards = memoryBuilder.getRelevantFlashcards(topic)

            val resp = api.askStudy(
                AskStudyRequest(
                    question = question,
                    topic = topic,
                    context = contextResp.context_summary,
                    relevant_flashcards = relevantCards
                )
            )

            // Auto-save flashcard if flagged
            if (resp.save_as_flashcard) {
                val tomorrow = System.currentTimeMillis() + 86_400_000L
                db.flashcardDao().insert(
                    FlashcardEntity(
                        id = UUID.randomUUID().toString(),
                        question = question,
                        answer = resp.answer,
                        topic = topic ?: "General",
                        source = "study",
                        createdAt = System.currentTimeMillis(),
                        nextReview = tomorrow
                    )
                )
            }

            Result.Success(resp)
        } catch (e: Exception) {
            Result.Error(e.toUserMessage())
        }
    }

    suspend fun generateFlashcards(notes: String, topic: String): Result<List<FlashcardEntity>> {
        return try {
            val resp = api.generateCards(GenerateCardsRequest(notes = notes, topic = topic))
            val tomorrow = System.currentTimeMillis() + 86_400_000L
            val entities = resp.flashcards.map {
                FlashcardEntity(
                    id = UUID.randomUUID().toString(),
                    question = it.question,
                    answer = it.answer,
                    topic = topic,
                    source = "generated",
                    createdAt = System.currentTimeMillis(),
                    nextReview = tomorrow
                )
            }
            Result.Success(entities)
        } catch (e: Exception) {
            Result.Error(e.toUserMessage())
        }
    }

    // ── Daily FAQs ───────────────────────────────────────────────────────────

    fun getAllDailyFaqs(): Flow<List<DailyFaqEntity>> = db.dailyFaqDao().getAll()

    suspend fun todayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    suspend fun alreadyFetchedToday(): Boolean =
        db.dailyFaqDao().countSince(todayStartMillis()) > 0

    suspend fun fetchAndStoreDailyFaqs(topic: String, role: String, level: String): Result<Int> {
        return try {
            val resp = api.fetchDailyFaq(DailyFaqRequest(topic = topic, role = role, level = level))
            val now = System.currentTimeMillis()
            val entities = resp.faqs.map { dto ->
                DailyFaqEntity(
                    id = UUID.randomUUID().toString(),
                    topic = resp.topic,
                    question = dto.question,
                    answer = dto.answer,
                    source = dto.source,
                    fetchedAt = now
                )
            }
            db.dailyFaqDao().insertAll(entities)
            Result.Success(entities.size)
        } catch (e: Exception) {
            Result.Error(e.toUserMessage())
        }
    }

    suspend fun saveFaqAsFlashcard(faq: DailyFaqEntity) {
        val tomorrow = System.currentTimeMillis() + 86_400_000L
        db.flashcardDao().insert(
            FlashcardEntity(
                id = UUID.randomUUID().toString(),
                question = faq.question,
                answer = faq.answer,
                topic = faq.topic,
                source = "study",
                createdAt = System.currentTimeMillis(),
                nextReview = tomorrow
            )
        )
        db.dailyFaqDao().update(faq.copy(savedAsFlashcard = true))
    }

    // ── Demo Seed ─────────────────────────────────────────────────────────────

    suspend fun seedDemoData(role: String, level: String) {
        DemoDataSeeder(db).seed(role, level)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    fun parseWeakSpots(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson(json, Array<String>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun Exception.toUserMessage(): String = when {
        message?.contains("Unable to resolve host") == true ||
        message?.contains("timeout") == true ||
        message?.contains("connect") == true ->
            "Cannot reach server. Make sure the backend is running."
        message?.contains("500") == true ->
            "AI model temporarily overloaded. Please try again."
        message?.contains("503") == true ->
            "Service unavailable. Please try again shortly."
        else -> message ?: "Something went wrong. Please try again."
    }
}
