"""Starts the stopped engine instance whenever judge work is waiting in its queues.

The problem service already starts the engine on every enqueue. This Lambda is the safety net for the gaps: a
submission that arrived while the instance was still stopping, or a start call that failed. It runs when the
instance reaches "stopped" (EventBridge state-change rule) and every few minutes on a schedule.

Env: ENGINE_INSTANCE_ID, QUEUE_URLS (comma separated).
"""
import os

import boto3

INSTANCE_ID = os.environ["ENGINE_INSTANCE_ID"]
QUEUE_URLS = [u for u in os.environ["QUEUE_URLS"].split(",") if u]
ATTRIBUTES = ["ApproximateNumberOfMessages", "ApproximateNumberOfMessagesNotVisible", "ApproximateNumberOfMessagesDelayed"]

sqs = boto3.client("sqs")
ec2 = boto3.client("ec2")


def pending_messages():
    total = 0
    for url in QUEUE_URLS:
        attrs = sqs.get_queue_attributes(QueueUrl=url, AttributeNames=ATTRIBUTES)["Attributes"]
        total += sum(int(v) for v in attrs.values())
    return total


def handler(event, context):
    pending = pending_messages()
    if pending == 0:
        return {"pending": 0, "action": "none"}
    state = ec2.describe_instances(InstanceIds=[INSTANCE_ID])["Reservations"][0]["Instances"][0]["State"]["Name"]
    if state == "stopped":
        ec2.start_instances(InstanceIds=[INSTANCE_ID])
        action = "started"
    else:
        # pending/running: already up; stopping: the "stopped" event will invoke this again.
        action = f"none ({state})"
    print(f"pending={pending} state={state} action={action}")
    return {"pending": pending, "state": state, "action": action}
