import os
import uvicorn
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware  # Import this

from app.api import problem_router, editorial_router
from app.api.submission_router import router as sub_router
from app.database import engine, Base

# Create database tables
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="Problem Microservice",
    description="Microservice for problem management and code submission",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.exception_handler(Exception)
async def runtime_exception_handler(request: Request, exc: Exception):
    return JSONResponse(
        status_code=404,
        content={"message": str(exc)},
    )

app.include_router(problem_router.router)
app.include_router(editorial_router.router)
app.include_router(sub_router)

@app.get("/api/v1/problem/health-check")
async def health_check():
    return "health is running"