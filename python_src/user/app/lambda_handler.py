"""AWS Lambda entry point: adapts API Gateway / Function URL events to the FastAPI app."""
from mangum import Mangum

from app.main import app, setup_schema

_asgi = Mangum(app, lifespan="off")


def handler(event, context):
    # Run once after each deploy: aws lambda invoke --payload '{"setup_schema": true}' ...
    if event.get("setup_schema"):
        setup_schema()
        return {"schema": "ok"}
    return _asgi(event, context)
