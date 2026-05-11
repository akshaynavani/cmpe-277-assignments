from typing import Any, Dict, List, Optional
from pydantic import BaseModel


# ── Context Builder ───────────────────────────────────────────────────────────

class TopicPerformance(BaseModel):
    topic: str
    scores: List[float]


class StudyActivity(BaseModel):
    topic: str
    times_asked: int
    avg_self_grade: float


class BuildContextRequest(BaseModel):
    user_profile: Dict[str, str]
    topic_scores: List[TopicPerformance]
    episodic_memories: List[str]
    faq_activity: List[StudyActivity]


class BuildContextResponse(BaseModel):
    context_summary: str


# ── Session ───────────────────────────────────────────────────────────────────

class BeginSessionRequest(BaseModel):
    role: str
    level: str
    domain: str
    context: str


class BeginSessionResponse(BaseModel):
    question: str


class ConversationMessage(BaseModel):
    role: str
    content: str


class RubricScores(BaseModel):
    clarity: float
    correctness: float
    communication: float
    edge_cases: float


class EvaluateAnswerRequest(BaseModel):
    role: str
    level: str
    domain: str
    context: str
    session_delta: str
    history: List[ConversationMessage]
    answer: str


class EvaluateAnswerResponse(BaseModel):
    scores: RubricScores
    feedback: str
    topic: str
    next_question: str


class ScoreRecord(BaseModel):
    topic: str
    clarity: float
    correctness: float
    communication: float
    edge_cases: float


class WrapSessionRequest(BaseModel):
    role: str
    level: str
    context: str
    session_delta: str
    scores: List[Dict[str, Any]]


class WrapSessionResponse(BaseModel):
    overall_score: float
    strong_areas: List[str]
    weak_spots: List[str]
    summary: str
    next_focus: str


# ── Daily FAQ ─────────────────────────────────────────────────────────────────

class DailyFaqRequest(BaseModel):
    topic: str
    role: str
    level: str


class FaqItem(BaseModel):
    question: str
    answer: str
    source: str  # e.g. "glassdoor", "leetcode", "internet"


class DailyFaqResponse(BaseModel):
    faqs: List[FaqItem]
    topic: str


# ── Study ─────────────────────────────────────────────────────────────────────

class FlashcardRef(BaseModel):
    question: str
    answer: str
    topic: str


class AskStudyRequest(BaseModel):
    question: str
    topic: Optional[str] = None
    context: str
    relevant_flashcards: List[FlashcardRef] = []


class AskStudyResponse(BaseModel):
    answer: str
    related_topics: List[str]
    save_as_flashcard: bool


class GenerateCardsRequest(BaseModel):
    notes: str
    topic: str


class GeneratedCard(BaseModel):
    question: str
    answer: str


class GenerateCardsResponse(BaseModel):
    flashcards: List[GeneratedCard]
    topic: str
