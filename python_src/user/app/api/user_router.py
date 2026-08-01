from fastapi import APIRouter, Depends, HTTPException, Header
from sqlalchemy.orm import Session
from app.database import get_db
from app.schemas.user_schema import RegisterDTO, LoginDTO, ChatRequest
from app.services.user_service import UserService
from app.services.ai_service import AiService
from app.core import security

router = APIRouter(prefix="/api/v1/user")

@router.post("/register")
def register(data: RegisterDTO, db: Session = Depends(get_db)):
    service = UserService(db)
    return service.register_user(data)

@router.post("/login")
def login(data: LoginDTO, db: Session = Depends(get_db)):
    service = UserService(db)
    token = service.login_user(data)
    if not token:
        raise HTTPException(status_code=401, detail="Invalid credentials")
    return {"token": token}

@router.post("/chat")
async def chat(
    request: ChatRequest, 
    authorization: str = Header(None), 
    db: Session = Depends(get_db)
):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Unauthorized")
    
    token = authorization.split(" ")[1]
    user_id = security.extract_user_id(token)
    
    user_service = UserService(db)
    user = user_service.get_user_by_id(user_id)
    
    ai_service = AiService(db)
    reply = await ai_service.get_ai_response(
        user, 
        request.problemStatement, 
        request.code, 
        request.userMessage,
        request.problemId
    )
    return {"reply": reply}

@router.get("/chat/history/{problemId}")
async def get_chat_history(
    problemId: str,
    authorization: str = Header(None),
    db: Session = Depends(get_db)
):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Unauthorized")
    
    token = authorization.split(" ")[1]
    user_id = security.extract_user_id(token)
    
    ai_service = AiService(db)
    history = await ai_service.get_chat_history(user_id, problemId)
    return history

@router.get("/user")
def get_user(
    authorization: str = Header(None), 
    db: Session = Depends(get_db)
):
    # Check for header
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing or invalid Authorization header")
    
    # Extract token (remove "Bearer " prefix)
    token = authorization.split(" ")[1]
    
    # Decode token to get user_id
    user_id = security.extract_user_id(token)
    if not user_id:
        raise HTTPException(status_code=401, detail="Invalid token")
    
    # Get user from database
    service = UserService(db)
    user = service.get_user_by_id(int(user_id))
    
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    return user