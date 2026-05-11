from fastapi import APIRouter
from agents.study_agent import StudyAgent
from models.schemas import (
    AskStudyRequest, AskStudyResponse,
    GenerateCardsRequest, GenerateCardsResponse, GeneratedCard,
    DailyFaqRequest, DailyFaqResponse, FaqItem,
)

router = APIRouter(prefix="/study", tags=["Study"])
agent = StudyAgent()


@router.post("/ask", response_model=AskStudyResponse)
def ask_study(req: AskStudyRequest):
    """
    Answers a technical question using candidate context and related flashcards
    passed from the Android app's local database.
    Fully stateless -- no DB reads or writes.
    """
    result = agent.ask(
        question=req.question,
        context=req.context,
        relevant_flashcards=[f.model_dump() for f in req.relevant_flashcards],
        topic=req.topic,
    )
    return AskStudyResponse(
        answer=result["answer"],
        related_topics=result.get("related_topics", []),
        save_as_flashcard=result.get("save_as_flashcard", False),
    )


@router.post("/generate", response_model=GenerateCardsResponse)
def generate_cards(req: GenerateCardsRequest):
    """
    Converts raw study notes into flashcards.
    Android app saves the returned cards to its local Room database.
    Fully stateless -- no DB reads or writes.
    """
    cards = agent.generate_cards(req.notes, req.topic)
    return GenerateCardsResponse(
        flashcards=[GeneratedCard(**c) for c in cards],
        topic=req.topic,
    )


@router.post("/daily-faq", response_model=DailyFaqResponse)
def daily_faq(req: DailyFaqRequest):
    """
    Searches the internet (DuckDuckGo) for real interview FAQs about the
    candidate's topic/role/level, then synthesises them via LLM.
    Called once daily by Android WorkManager; results are persisted in Room.
    """
    faqs = agent.fetch_daily_faqs(
        topic=req.topic,
        role=req.role,
        level=req.level,
    )
    return DailyFaqResponse(
        faqs=[FaqItem(**f) for f in faqs],
        topic=req.topic,
    )
