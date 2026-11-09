import logging
import os
import uvicorn
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware  # Import this

from app.api import problem_router, editorial_router, admin_router, assessment_router, run_router
from app.api.submission_router import router as sub_router
from app.core.schema_upgrade import ensure_columns
from app.database import engine, Base

def setup_schema():
    Base.metadata.create_all(bind=engine)
    ensure_columns(engine)

# Runs table schema 
if os.getenv("RUN_SCHEMA_SETUP", "true").lower() == "true":
    setup_schema()

app = FastAPI(
    title="Problem Microservice",
    description="Microservice for problem management and code submission",
    version="1.0.0"
)

_origins = [o.strip() for o in os.getenv("CORS_ORIGINS", "*").split(",") if o.strip()]

app.add_middleware(
    CORSMiddleware,
    allow_origins=_origins,
    allow_credentials=False,
    allow_methods=["GET", "POST", "PATCH", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "X-API-Key"],
    expose_headers=["Retry-After"],
)

@app.exception_handler(Exception)
async def runtime_exception_handler(request: Request, exc: Exception):
    # Log the real error; never send exception text (SQL, hostnames, paths) to the client.
    logging.getLogger(__name__).exception("Unhandled error on %s %s", request.method, request.url.path)
    return JSONResponse(
        status_code=500,
        content={"message": "Internal server error"},
    )

app.include_router(problem_router.router)
app.include_router(run_router.router)
app.include_router(editorial_router.router)
app.include_router(admin_router.router)
app.include_router(assessment_router.admin_router)
app.include_router(assessment_router.router)
app.include_router(sub_router)

@app.get("/api/v1/problem/health-check")
async def health_check():
    return "health is running"