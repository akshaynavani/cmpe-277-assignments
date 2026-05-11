import json
import logging
import os
import time
from typing import Any, Dict, List, Optional

from fastapi import HTTPException
from groq import Groq, RateLimitError, InternalServerError, AuthenticationError, APIConnectionError
from duckduckgo_search import DDGS

MODEL = "llama-3.3-70b-versatile"
log = logging.getLogger(__name__)


def _complete_with_retry(client: Groq, **kwargs) -> str:
    last_exc: Exception = RuntimeError("unreachable")
    for attempt in range(4):
        try:
            response = client.chat.completions.create(**kwargs)
            return response.choices[0].message.content.strip()
        except AuthenticationError:
            raise  # bad credentials — retrying wastes quota
        except (RateLimitError, InternalServerError, APIConnectionError, Exception) as exc:
            last_exc = exc
            if attempt == 3:
                raise
            time.sleep(2 ** attempt)
    raise last_exc


class StudyAgent:
    def __init__(self):
        self.client = Groq(api_key=os.environ["GROQ_API_KEY"])

    def ask(
        self,
        question: str,
        context: str,
        relevant_flashcards: List[Dict[str, str]],
        topic: Optional[str] = None,
    ) -> Dict[str, Any]:
        flashcard_context = ""
        if relevant_flashcards:
            cards = "\n".join(f"Q: {c['question']}\nA: {c['answer']}" for c in relevant_flashcards)
            flashcard_context = f"\nRELEVANT STUDY CARDS:\n{cards}\n"

        prompt = (
            "You are a personal technical study assistant.\n\n"
            f"CANDIDATE CONTEXT:\n{context}\n"
            f"{flashcard_context}\n"
            f"QUESTION: {question}\n\n"
            "Personalise your answer using the candidate context. Return JSON only:\n"
            '{"answer":"clear concise explanation 2-4 sentences",'
            '"related_topics":["2-3 related topics"],'
            '"save_as_flashcard":true or false}'
        )
        text = _complete_with_retry(
            self.client,
            model=MODEL,
            max_tokens=400,
            response_format={"type": "json_object"},
            messages=[{"role": "user", "content": prompt}],
        )
        try:
            return json.loads(text)
        except ValueError as exc:
            log.error("StudyAgent.ask bad JSON: %s", text)
            raise HTTPException(status_code=502, detail="Agent returned invalid JSON") from exc

    def fetch_daily_faqs(self, topic: str, role: str, level: str) -> List[Dict[str, str]]:
        """
        Searches the internet for real interview FAQs about the given topic,
        then uses Groq to synthesize them into structured Q&A pairs with sources.
        """
        snippets: List[str] = []
        source_labels: List[str] = []

        search_queries = [
            f"{level} {role} {topic} interview questions 2024",
            f"{topic} technical interview FAQ site:glassdoor.com OR site:leetcode.com OR site:interviewbit.com",
            f"{role} {topic} interview experience questions answers",
        ]

        ddgs = DDGS()
        for query in search_queries:
            try:
                results = list(ddgs.text(query, max_results=5))
                for r in results:
                    body = r.get("body", "").strip()
                    href = r.get("href", "")
                    if body and len(body) > 50:
                        snippets.append(f"[{href}]\n{body}")
                        # Derive a short source label from the domain
                        for known in ("glassdoor", "leetcode", "interviewbit", "geeksforgeeks", "stackoverflow"):
                            if known in href:
                                source_labels.append(known)
                                break
                        else:
                            source_labels.append("internet")
            except Exception:
                continue  # skip failed searches, continue with what we have

        if not snippets:
            # Fallback: generate without web context
            combined_context = f"No web results available. Use your training knowledge."
        else:
            combined_context = "\n\n---\n\n".join(snippets[:12])  # cap at 12 snippets

        prompt = (
            f"You are an expert interview coach. A {level} {role} candidate is preparing for {topic} interviews.\n\n"
            f"INTERNET SEARCH RESULTS:\n{combined_context}\n\n"
            f"Based on the above real-world search results and your knowledge, generate exactly 8 diverse, "
            f"high-quality interview FAQ entries for a {level} {role} about '{topic}'. "
            f"Cover different sub-topics, difficulty levels, and question types (conceptual, coding, behavioral).\n\n"
            f"Return JSON only:\n"
            f'{{"faqs":['
            f'{{"question":"...","answer":"clear 2-4 sentence answer","source":"glassdoor|leetcode|interviewbit|internet"}},'
            f'...]}}'
        )

        text = _complete_with_retry(
            self.client,
            model=MODEL,
            max_tokens=3000,
            response_format={"type": "json_object"},
            messages=[{"role": "user", "content": prompt}],
        )
        try:
            data = json.loads(text)
        except ValueError as exc:
            log.error("StudyAgent.fetch_daily_faqs bad JSON: %s", text)
            raise HTTPException(status_code=502, detail="Agent returned invalid JSON") from exc
        return data.get("faqs", []) if isinstance(data, dict) else []

    def generate_cards(self, notes: str, topic: str) -> List[Dict[str, str]]:
        prompt = (
            f"Convert these study notes into flashcards for topic: {topic}\n\n"
            f"NOTES:\n{notes}\n\n"
            "Return a JSON object with a 'flashcards' array of 5-15 cards:\n"
            '{"flashcards":[{"question":"...","answer":"concise 1-3 sentence answer"}]}'
        )
        text = _complete_with_retry(
            self.client,
            model=MODEL,
            max_tokens=2000,
            response_format={"type": "json_object"},
            messages=[{"role": "user", "content": prompt}],
        )
        try:
            data = json.loads(text)
        except ValueError as exc:
            log.error("StudyAgent.generate_cards bad JSON: %s", text)
            raise HTTPException(status_code=502, detail="Agent returned invalid JSON") from exc
        return data.get("flashcards", []) if isinstance(data, dict) else []
