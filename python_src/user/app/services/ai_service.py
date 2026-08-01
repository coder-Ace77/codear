import httpx
import os
import datetime
from sqlalchemy.orm import Session
from app.models.user import ChatMessage, User

class AiService:
    def __init__(self, db: Session):
        self.db = db
        self.api_key = os.getenv("OPEN_AI_KEY")
        self.api_url = "https://api.openai.com/v1/chat/completions"


    async def get_chat_history(self, user_id: int, problem_id: str):
        history = self.db.query(ChatMessage).filter(
            ChatMessage.user_id == user_id,
            ChatMessage.problem_id == problem_id
        ).order_by(ChatMessage.timestamp.asc()).all()
        
        return [{"role": msg.role, "content": msg.content} for msg in history]

    async def get_ai_response(self, user: User, problem_statement: str, code: str, user_message: str, problem_id: str):
        from app.services.rate_limiter_service import RateLimiterService
        
        max_chats = int(os.getenv("MAX_CHATS_PER_WEEK", 20))
        seconds_in_week = 7 * 24 * 3600
        
        is_allowed, next_time, update_vals = RateLimiterService.check_rate_limit(
            current_count=user.chat_count_week or 0,
            last_reset=user.last_chat_reset,
            max_requests=max_chats,
            period_seconds=seconds_in_week
        )
        
        if not is_allowed:
            formatted_time = next_time.strftime("%Y-%m-%d %H:%M:%S UTC")
            return f"⚠️ Rate limit reached. Standard rate is {max_chats} chats/week. Next chat available at {formatted_time}."
            
        # Update user state if allowed
        user.chat_count_week = update_vals[0]
        user.last_chat_reset = update_vals[1]
        # Commit will happen after successful response in save_message
        self.db.commit()

        system_prompt = f"You are an expert coding assistant. Context:\nProblem: {problem_statement}\nUser's Current Code: {code}\nKeep answers concise."
        
        history = self.db.query(ChatMessage).filter(
            ChatMessage.user_id == user.id,
            ChatMessage.problem_id == problem_id
        ).order_by(ChatMessage.timestamp.asc()).all()

        messages = [{"role": "system", "content": system_prompt}]
        
        for msg in history:
            role = "assistant" if msg.role == "assistant" else "user"
            messages.append({"role": role, "content": msg.content})

        messages.append({"role": "user", "content": user_message})
        
        payload = {
            "model": os.getenv("AI_MODEL", "gpt-3.5-turbo"),
            "messages": messages
        }
        headers = {"Authorization": f"Bearer {self.api_key}"}

        async with httpx.AsyncClient() as client:
            try:
                response = await client.post(self.api_url, json=payload, headers=headers)
                response.raise_for_status()
                data = response.json()
                ai_reply = data["choices"][0]["message"]["content"]
            except Exception as e:
                return f"⚠️ AI Service Error: {str(e)}"

            self.save_message(user.id, user_message, "user", problem_id)
            self.save_message(user.id, ai_reply, "assistant", problem_id)
            
            # user.chat_count_week updated at start of function
            # self.db.commit() # Already committed in save_message
            
            return ai_reply

    def save_message(self, user_id, content, role, problem_id):
        msg = ChatMessage(user_id=user_id, content=content, role=role, problem_id=problem_id)
        self.db.add(msg)
        self.db.commit()