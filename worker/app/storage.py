import os
from pathlib import Path

import boto3


def _s3_client():
    return boto3.client(
        "s3",
        region_name=os.getenv("AWS_REGION"),
    )


def download_text(key: str) -> str:
    bucket = os.getenv("AWS_S3_BUCKET")
    if not bucket:
        raise RuntimeError("AWS_S3_BUCKET is not set")
    obj = _s3_client().get_object(Bucket=bucket, Key=key)
    return obj["Body"].read().decode("utf-8")


def upload_bytes(key: str, data: bytes, content_type: str) -> None:
    bucket = os.getenv("AWS_S3_BUCKET")
    if not bucket:
        raise RuntimeError("AWS_S3_BUCKET is not set")
    _s3_client().put_object(Bucket=bucket, Key=key, Body=data, ContentType=content_type)


def upload_file(key: str, file_path: Path, content_type: str) -> None:
    upload_bytes(key, file_path.read_bytes(), content_type)
