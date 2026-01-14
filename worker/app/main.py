import logging
import os
import time

from .job_api import mark_failed, mark_running, mark_succeeded
from .queue import delete_message, fetch_job
from .runner import run_job


def _setup_logging() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s [worker] %(message)s",
    )


def _truncate_message(message: str, limit: int) -> str:
    if len(message) <= limit:
        return message
    return message[: limit - 3] + "..."


def main() -> None:
    _setup_logging()
    logger = logging.getLogger(__name__)
    max_retries = int(os.getenv("WORKER_MAX_RETRIES", "5"))
    error_limit = int(os.getenv("WORKER_ERROR_MESSAGE_LIMIT", "1900"))

    while True:
        message = fetch_job()
        if message is None:
            time.sleep(2)
            continue

        job_id = message.job.job_id
        job_type = message.job.job_type
        receive_count = message.receive_count

        if receive_count > max_retries:
            reason = f"retry limit exceeded (count={receive_count})"
            logger.error("job %s type=%s %s", job_id, job_type, reason)
            try:
                mark_failed(job_id, _truncate_message(reason, error_limit))
            finally:
                delete_message(message.receipt_handle)
            continue

        try:
            if not mark_running(job_id):
                logger.info("skip job=%s type=%s reason=already_processed", job_id, job_type)
                delete_message(message.receipt_handle)
                continue

            output_key = run_job(message.job)
            if not mark_succeeded(job_id, output_key):
                logger.info("skip job=%s type=%s reason=status_conflict", job_id, job_type)
                delete_message(message.receipt_handle)
                continue

            logger.info("completed job=%s type=%s output=%s", job_id, job_type, output_key)
            delete_message(message.receipt_handle)
        except Exception as exc:
            logger.exception("failed job=%s type=%s attempt=%s", job_id, job_type, receive_count)
            try:
                error_message = _truncate_message(str(exc), error_limit)
                if mark_failed(job_id, error_message):
                    delete_message(message.receipt_handle)
            except Exception as mark_exc:
                logger.exception("failed to mark job=%s type=%s", job_id, job_type)
            # Keep message if status update fails to allow retry.


if __name__ == "__main__":
    main()
