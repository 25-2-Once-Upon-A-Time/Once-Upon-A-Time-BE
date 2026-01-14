import json
import os
import tempfile
from pathlib import Path

from .models import JobSpec
from .storage import download_text, upload_bytes, upload_file


def run_job(job: JobSpec) -> str:
    """
    Execute a single AI job and upload output to S3.
    Returns the output key for the job result metadata.
    """
    if job.job_type == "STORY":
        return _run_story(job)
    if job.job_type == "AUDIOBOOK":
        return _run_audiobook(job)
    if job.job_type == "ILLUSTRATION":
        return _run_illustration(job)
    raise ValueError(f"Unsupported job type: {job.job_type}")


def _run_story(job: JobSpec) -> str:
    from .fairy_tale_generator import main_api_handler

    input_json = download_text(job.input_key)
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        raise RuntimeError("OPENAI_API_KEY is not set")

    result_json = main_api_handler(input_json, api_key)
    output_key = f"jobs/{job.job_id}/story/result.json"
    upload_bytes(output_key, result_json.encode("utf-8"), "application/json")
    return output_key


def _run_illustration(job: JobSpec) -> str:
    from .illustration_generator import generate_cover_from_json_file

    input_json = download_text(job.input_key)
    with tempfile.TemporaryDirectory() as tmp_dir:
        input_path = Path(tmp_dir) / "input.json"
        input_path.write_text(input_json, encoding="utf-8")

        output_path = Path(tmp_dir) / "thumbnail.png"
        metadata = generate_cover_from_json_file(str(input_path), output_path=str(output_path))
        image_path = Path(metadata["cover_image_path"])
        image_key = f"jobs/{job.job_id}/illustration/thumbnail.png"
        upload_file(image_key, image_path, "image/png")

    result = {
        "success": True,
        "data": {
            "image_key": image_key,
        },
    }
    output_key = f"jobs/{job.job_id}/illustration/result.json"
    upload_bytes(output_key, json.dumps(result, ensure_ascii=False).encode("utf-8"), "application/json")
    return output_key


def _run_audiobook(job: JobSpec) -> str:
    from .audiobook_generator import generate_audiobook_from_json_file

    input_json = download_text(job.input_key)
    with tempfile.TemporaryDirectory() as tmp_dir:
        input_path = Path(tmp_dir) / "input.json"
        input_path.write_text(input_json, encoding="utf-8")

        metadata = generate_audiobook_from_json_file(str(input_path), output_dir=tmp_dir)
        output_dir = Path(metadata["output_directory"])

        audio_keys = []
        for path in output_dir.glob("*.wav"):
            key = f"jobs/{job.job_id}/audiobook/{path.name}"
            upload_file(key, path, "audio/wav")
            audio_keys.append(key)

    result = {
        "success": True,
        "data": {
            "audio_keys": audio_keys,
        },
    }
    output_key = f"jobs/{job.job_id}/audiobook/result.json"
    upload_bytes(output_key, json.dumps(result, ensure_ascii=False).encode("utf-8"), "application/json")
    return output_key
