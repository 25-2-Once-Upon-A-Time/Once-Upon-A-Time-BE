import json
import os
import urllib.error
import urllib.request
from typing import Any, Dict, Optional, Tuple


def _request(method: str, path: str, payload: Optional[Dict[str, Any]] = None) -> Tuple[int, str]:
    base_url = os.getenv("JOB_API_BASE_URL")
    if not base_url:
        raise RuntimeError("JOB_API_BASE_URL is not set")

    worker_token = os.getenv("WORKER_TOKEN")
    if not worker_token:
        raise RuntimeError("WORKER_TOKEN is not set")

    url = f"{base_url.rstrip('/')}{path}"
    headers = {
        "X-Worker-Token": worker_token,
    }

    data = None
    if payload is not None:
        data = json.dumps(payload).encode("utf-8")
        headers["Content-Type"] = "application/json"

    request = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            return response.status, response.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8") if exc.fp else ""
        return exc.code, body


def mark_running(job_id: str) -> bool:
    status, _ = _request("POST", f"/api/v1/internal/jobs/{job_id}/running")
    if status == 200:
        return True
    if status in (404, 409):
        return False
    raise RuntimeError(f"Failed to mark running: status={status}")


def mark_succeeded(job_id: str, output_key: str) -> bool:
    status, _ = _request(
        "POST",
        f"/api/v1/internal/jobs/{job_id}/succeeded",
        {"outputKey": output_key},
    )
    if status == 200:
        return True
    if status in (404, 409):
        return False
    raise RuntimeError(f"Failed to mark succeeded: status={status}")


def mark_failed(job_id: str, error_message: str) -> bool:
    status, _ = _request(
        "POST",
        f"/api/v1/internal/jobs/{job_id}/failed",
        {"errorMessage": error_message},
    )
    if status == 200:
        return True
    if status in (404, 409):
        return False
    raise RuntimeError(f"Failed to mark failed: status={status}")
