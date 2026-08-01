import boto3
import json
import os
from dotenv import load_dotenv

load_dotenv()

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