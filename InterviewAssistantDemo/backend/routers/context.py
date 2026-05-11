from fastapi import APIRouter
from agents.context_agent import ContextBuilderAgent
from models.schemas import BuildContextRequest, BuildContextResponse

router = APIRouter(prefix="/context", tags=["Context"])
agent = ContextBuilderAgent()


@router.post("/build", response_model=BuildContextResponse)
def build_context(req: BuildContextRequest):
    """
    Called once per session by the Android app.
    Receives aggregated local data, returns a compact coaching brief
    cached on the device for the duration of the session.
    """
    summary = agent.build(
        user_profile=req.user_profile,
        topic_scores=[t.model_dump() for t in req.topic_scores],
        episodic_memories=req.episodic_memories,
        faq_activity=[f.model_dump() for f in req.faq_activity],
    )
    return BuildContextResponse(context_summary=summary)
