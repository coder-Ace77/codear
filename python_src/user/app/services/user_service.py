from sqlalchemy.orm import Session
from app.models.user import User
from app.schemas.user_schema import RegisterDTO, LoginDTO
from app.core import security
from fastapi import HTTPException

class UserService:
    def __init__(self, db: Session):
        self.db = db

    def register_user(self, data: RegisterDTO):
        # Check if email/username exists
        # Check if email/username exists
        if self.get_user_by_email(data.email):
            raise HTTPException(status_code=400, detail="Email already in use")
        
        if self.db.query(User).filter(User.username == data.username).first():
            raise HTTPException(status_code=400, detail="Username already taken")

        hashed_password = security.get_password_hash(data.password)
        
        new_user = User(
            username=data.username,
            name=data.name,
            email=data.email,
            password=hashed_password,
            role="USER",
            daily_streak=0,
            problem_solved_total=0
        )
        
        self.db.add(new_user)
        self.db.commit()
        self.db.refresh(new_user)
        return new_user

    def login_user(self, data: LoginDTO):
        user = self.get_user_by_email(data.email)
        if not user or not security.verify_password(data.password, user.password):
            return None
        
        # Generate JWT token using user ID as subject
        return security.create_access_token(data={"sub": str(user.id), "username": user.username})

    def get_user_by_email(self, email: str):
        from app.services.cache_service import CacheService
        
        # Check cache for email -> id mapping
        cache_key_email = f"user:email:{email}"
        user_id = CacheService.get_object(cache_key_email)
        
        if user_id:
            user = self.get_user_by_id(int(user_id))
            if user:
                return user
            # If user not found (inconsitency), fall through to DB
        
        user = self.db.query(User).filter(User.email == email).first()
        
        if user:
            # Cache the email -> id mapping
            CacheService.set_object(cache_key_email, str(user.id))
            # Also cache the user object itself by calling get_user_by_id logic or rely on next call
            # Ideally we should populate the ID cache too if we have the object
            # But get_user_by_id handles its own caching. 
            # We can manually seed it here or let the next call do it.
            # For efficiency in login, we might want to seed it.
            # But simpler to just rely on get_user_by_id call later or independent cache.
            pass
            
        return user

    def get_user_by_id(self, user_id: int):
        from app.services.cache_service import CacheService
        from datetime import datetime
        
        cache_key = f"user:{user_id}"
        cached_data = CacheService.get_object(cache_key)
        
        if cached_data:
            # Reconstruct User object from dictionary
            # Convert datetime strings back to datetime objects
            if cached_data.get("last_chat_reset"):
                cached_data["last_chat_reset"] = datetime.fromisoformat(cached_data["last_chat_reset"])
            return User(**cached_data)

        user = self.db.query(User).filter(User.id == user_id).first()
        
        if user:
            # Serialize User object to dictionary
            user_dict = {
                c.name: getattr(user, c.name) for c in user.__table__.columns
            }
            # Convert datetime objects to ISO strings
            if isinstance(user_dict.get("last_chat_reset"), datetime):
                user_dict["last_chat_reset"] = user_dict["last_chat_reset"].isoformat()
                
            CacheService.set_object(cache_key, user_dict)
            
        return user