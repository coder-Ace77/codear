import boto3
import json
import logging
import os
import time
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger(__name__)

sqs_client = boto3.client(
    'sqs',
    region_name='ap-south-1',
    aws_access_key_id=os.getenv('SQS_ACCESS_KEY'),
    aws_secret_access_key=os.getenv('SQS_SECRET_KEY')
)

# Reconcile the names with your .env
DEFAULT_QUEUE_URL = os.getenv('SQS_QUEUE_URL') 
TEST_QUEUE_URL = os.getenv('SQS_TEST_QUEUE')

def send_to_queue(message: dict, queue_url: str = None):
    # Use the provided URL, or fallback to the default from .env
    target_url = queue_url or DEFAULT_QUEUE_URL
    
    if not target_url:
        raise ValueError("SQS Queue URL is not defined. Check your .env file.")

    sqs_client.send_message(
        QueueUrl=target_url,
        MessageBody=json.dumps(message)
    )
    wake_engine()


# The engine's EC2 instance is stopped while idle. When ENGINE_INSTANCE_ID is set, every enqueue makes sure it is
# starting; starting a running instance is a no-op, so the call is only throttled to keep submits fast.
ENGINE_INSTANCE_ID = os.getenv('ENGINE_INSTANCE_ID')
WAKE_EVERY_SECONDS = 60
_ec2_client = None
_last_wake = 0.0


def wake_engine():
    global _ec2_client, _last_wake
    if not ENGINE_INSTANCE_ID or time.monotonic() - _last_wake < WAKE_EVERY_SECONDS:
        return
    _last_wake = time.monotonic()
    try:
        if _ec2_client is None:
            _ec2_client = boto3.client('ec2', region_name='ap-south-1')
        _ec2_client.start_instances(InstanceIds=[ENGINE_INSTANCE_ID])
    except Exception:
        # e.g. the instance is still stopping; the engine-waker Lambda starts it once it has stopped.
        logger.warning("could not start the engine instance", exc_info=True)