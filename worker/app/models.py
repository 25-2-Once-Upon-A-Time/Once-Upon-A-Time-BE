from dataclasses import dataclass


@dataclass(frozen=True)
class JobSpec:
    job_id: str
    job_type: str
    input_key: str


@dataclass(frozen=True)
class QueueMessage:
    job: JobSpec
    receipt_handle: str
    receive_count: int
