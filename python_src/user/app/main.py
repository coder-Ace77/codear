import os

from fastapi import FastAPI
from app.api import user_router
from app.database import engine, Base
from fastapi.middleware.cors import CORSMiddleware  # Import this

# Create database tables (equivalent to spring.jpa.hibernate.ddl-auto=update)
Base.metadata.create_all(bind=engine)

app = FastAPI(title="User Microservice")

# CORS_ORIGINS is a comma-separated list of allowed frontend origins. Auth is a Bearer header,
# not a cookie, so credentials are never needed (and "*" with credentials is not allowed).
_origins = [o.strip() for o in os.getenv("CORS_ORIGINS", "*").split(",") if o.strip()]

app.add_middleware(
    CORSMiddleware,
    allow_origins=_origins,
    allow_credentials=False,
    allow_methods=["GET", "POST", "PATCH", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type"],
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