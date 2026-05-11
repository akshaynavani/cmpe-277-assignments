import logging

from fastapi import APIRouter, HTTPException
from agents.session_agent import SessionAgent

log = logging.getLogger(__name__)
from models.schemas import (
    BeginSessionRequest, BeginSessionResponse,
    EvaluateAnswerRequest, EvaluateAnswerResponse, RubricScores,
    WrapSessionRequest, WrapSessionResponse,
)

router = APIRouter(prefix="/session", tags=["Session"])
agent = SessionAgent()


@router.post("/begin", response_model=BeginSessionResponse)
def begin_session(req: BeginSessionRequest):
    """
    Returns the opening interview question.
    Context summary guides topic selection and difficulty.
    Fully stateless -- no DB reads or writes.
    """
    question = agent.begin(req.role, req.level, req.domain, req.context)
    return BeginSessionResponse(question=question)


@router.post("/evaluate", response_model=EvaluateAnswerResponse)
def evaluate_answer(req: EvaluateAnswerRequest):
    """
    Evaluates the candidate's answer.
    Full conversation history and session delta are sent by the Android app.
    Returns rubric scores and the next question.
    Fully stateless -- no DB reads or writes.
    """
    result = agent.evaluate(
        role=req.role,
        level=req.level,
        domain=req.domain,
        context=req.context,
        session_delta=req.session_delta,
        history=[m.model_dump() for m in req.history],
        answer=req.answer,
    )
    try:
        rubric = RubricScores(**result["scores"])
        feedback = result["feedback"]
        topic = result["topic"]
        next_question = result["next_question"]
    except (KeyError, TypeError) as exc:
        log.error("evaluate_answer missing field %s in: %s", exc, result)
        raise HTTPException(status_code=502, detail=f"Agent response missing field: {exc}") from exc
    return EvaluateAnswerResponse(scores=rubric, feedback=feedback, topic=topic, next_question=next_question)


@router.post("/wrap", response_model=WrapSessionResponse)
def wrap_session(req: WrapSessionRequest):
    """
    Generates end-of-session summary with strong areas, weak spots, and next focus.
    Android app saves the result locally.
    Fully stateless -- no DB reads or writes.
    """
    result = agent.wrap(
        role=req.role,
        level=req.level,
        context=req.context,
        session_delta=req.session_delta,
        scores=req.scores,
    )
    required = ("overall_score", "strong_areas", "weak_spots", "summary", "next_focus")
    missing = [k for k in required if k not in result]
    if missing:
        log.error("wrap_session missing fields %s in: %s", missing, result)
        raise HTTPException(status_code=502, detail=f"Agent response missing fields: {missing}")
    return WrapSessionResponse(**result)
