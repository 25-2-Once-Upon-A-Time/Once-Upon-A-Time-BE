import json
import os
from typing import Optional

import boto3

from .models import JobSpec, QueueMessage


def _sqs_client():
    return boto3.client(
        "sqs",
        region_name=os.getenv("AWS_REGION"),
    )


def fetch_job() -> Optional[QueueMessage]:
    """
    Pull a job message from SQS and return the parsed payload.
    """
    queue_url = os.getenv("AWS_SQS_QUEUE_URL")
    if not queue_url:
        raise RuntimeError("AWS_SQS_QUEUE_URL is not set")

    response = _sqs_client().receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1,
        WaitTimeSeconds=10,
        AttributeNames=["ApproximateReceiveCount"],
    )
    messages = response.get("Messages", [])
    if not messages:
        return None

    message = messages[0]
    body = json.loads(message["Body"])
    job = JobSpec(
        job_id=body["jobId"],
        job_type=body["jobType"],
        input_key=body["inputKey"],
    )
    receive_count = int(message.get("Attributes", {}).get("ApproximateReceiveCount", "1"))
    return QueueMessage(
        job=job,
        receipt_handle=message["ReceiptHandle"],
        receive_count=receive_count,
    )


def delete_message(receipt_handle: str) -> None:
    queue_url = os.getenv("AWS_SQS_QUEUE_URL")
    if not queue_url:
        raise RuntimeError("AWS_SQS_QUEUE_URL is not set")
    _sqs_client().delete_message(QueueUrl=queue_url, ReceiptHandle=receipt_handle)
