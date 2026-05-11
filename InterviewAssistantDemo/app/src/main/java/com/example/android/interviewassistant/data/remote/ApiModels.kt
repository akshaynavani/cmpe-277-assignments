package com.example.android.interviewassistant.data.remote

import com.google.gson.annotations.SerializedName

// ── Context Builder ──────────────────────────────────────────────────────────

data class TopicPerformanceDto(
    val topic: String,
    val scores: List<Float>
)

data class StudyActivityDto(
    val topic: String,
    val times_asked: Int,
    val avg_self_grade: Float
)

data class BuildContextRequest(
    val user_profile: Map<String, String>,
    val topic_scores: List<TopicPerformanceDto>,
    val episodic_memories: List<String>,
    val faq_activity: List<StudyActivityDto>
)

data class BuildContextResponse(
    val context_summary: String
)

// ── Session ──────────────────────────────────────────────────────────────────

data class BeginSessionRequest(
    val role: String,
    val level: String,
    val domain: String,
    val context: String
)

data class BeginSessionResponse(
    val question: String
)

data class ConversationMessageDto(
    val role: String,
    val content: String
)

data class RubricScoresDto(
    val clarity: Float,
    val correctness: Float,
    val communication: Float,
    val edge_cases: Float
)

data class EvaluateAnswerRequest(
    val role: String,
    val level: String,
    val domain: String,
    val context: String,
    val session_delta: String,
    val history: List<ConversationMessageDto>,
    val answer: String
)

data class EvaluateAnswerResponse(
    val scores: RubricScoresDto,
    val feedback: String,
    val topic: String,
    val next_question: String
)

data class ScorePayload(
    val topic: String,
    val clarity: Float,
    val correctness: Float,
    val communication: Float,
    val edge_cases: Float
)

data class WrapSessionRequest(
    val role: String,
    val level: String,
    val context: String,
    val session_delta: String,
    val scores: List<ScorePayload>
)

data class WrapSessionResponse(
    val overall_score: Float,
    val strong_areas: List<String>,
    val weak_spots: List<String>,
    val summary: String,
    val next_focus: String
)

// ── Study ────────────────────────────────────────────────────────────────────

data class FlashcardRefDto(
    val question: String,
    val answer: String,
    val topic: String
)

data class AskStudyRequest(
    val question: String,
    val topic: String?,
    val context: String,
    val relevant_flashcards: List<FlashcardRefDto>
)

data class AskStudyResponse(
    val answer: String,
    val related_topics: List<String>,
    val save_as_flashcard: Boolean
)

data class GenerateCardsRequest(
    val notes: String,
    val topic: String
)

data class GeneratedCardDto(
    val question: String,
    val answer: String
)

data class GenerateCardsResponse(
    val flashcards: List<GeneratedCardDto>,
    val topic: String
)

// ── Daily FAQ ────────────────────────────────────────────────────────────────

data class DailyFaqRequest(
    val topic: String,
    val role: String,
    val level: String
)

data class FaqItemDto(
    val question: String,
    val answer: String,
    val source: String
)

data class DailyFaqResponse(
    val faqs: List<FaqItemDto>,
    val topic: String
)

// ── Health ───────────────────────────────────────────────────────────────────

data class HealthResponse(
    val status: String
)
