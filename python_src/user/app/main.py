from fastapi import FastAPI
from app.api import user_router
from app.database import engine, Base
from fastapi.middleware.cors import CORSMiddleware  # Import this

# Create database tables (equivalent to spring.jpa.hibernate.ddl-auto=update)
Base.metadata.create_all(bind=engine)

app = FastAPI(title="User Microservice")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  
    allow_credentials=True,
    allow_methods=["*"],  
    allow_headers=["*"],  
)

app.include_router(user_router.router)

@app.get("/api/v1/user/health")
def health_check():
    return "User service is up and running"


if __name__ == "__main__":
    import uvicorn
    import os
    port = int(os.getenv("PORT", 8080))
    uvicorn.run(app, host="0.0.0.0", port=port)