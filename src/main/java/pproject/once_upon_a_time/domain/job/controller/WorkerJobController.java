package pproject.once_upon_a_time.domain.job.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pproject.once_upon_a_time.domain.job.domain.Job;
import pproject.once_upon_a_time.domain.job.dto.JobMarkFailedRequestDto;
import pproject.once_upon_a_time.domain.job.dto.JobMarkSucceededRequestDto;
import pproject.once_upon_a_time.domain.job.dto.JobResponseDto;
import pproject.once_upon_a_time.domain.job.service.JobService;
import pproject.once_upon_a_time.domain.job.service.WorkerAuthService;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;
import pproject.once_upon_a_time.global.response.ApiResult;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/jobs")
@RequiredArgsConstructor
public class WorkerJobController {

    private final JobService jobService;
    private final WorkerAuthService workerAuthService;

    @PostMapping("/{jobId}/running")
    public ResponseEntity<ApiResult<JobResponseDto>> markRunning(
        @PathVariable UUID jobId,
        @RequestHeader("X-Worker-Token") String workerToken
    ) {
        workerAuthService.validate(workerToken);
        jobService.getJob(jobId);

        if (!jobService.markRunning(jobId)) {
            throw new CustomException(ErrorCode.JOB_STATUS_NOT_ALLOWED);
        }

        Job job = jobService.getJob(jobId);
        return ResponseEntity.ok(ApiResult.ok(JobResponseDto.from(job)));
    }

    @PostMapping("/{jobId}/succeeded")
    public ResponseEntity<ApiResult<JobResponseDto>> markSucceeded(
        @PathVariable UUID jobId,
        @RequestHeader("X-Worker-Token") String workerToken,
        @RequestBody @Valid JobMarkSucceededRequestDto request
    ) {
        workerAuthService.validate(workerToken);
        jobService.getJob(jobId);

        if (!jobService.markSucceeded(jobId, request.getOutputKey())) {
            throw new CustomException(ErrorCode.JOB_STATUS_NOT_ALLOWED);
        }

        Job job = jobService.getJob(jobId);
        return ResponseEntity.ok(ApiResult.ok(JobResponseDto.from(job)));
    }

    @PostMapping("/{jobId}/failed")
    public ResponseEntity<ApiResult<JobResponseDto>> markFailed(
        @PathVariable UUID jobId,
        @RequestHeader("X-Worker-Token") String workerToken,
        @RequestBody @Valid JobMarkFailedRequestDto request
    ) {
        workerAuthService.validate(workerToken);
        jobService.getJob(jobId);

        if (!jobService.markFailed(jobId, request.getErrorMessage())) {
            throw new CustomException(ErrorCode.JOB_STATUS_NOT_ALLOWED);
        }

        Job job = jobService.getJob(jobId);
        return ResponseEntity.ok(ApiResult.ok(JobResponseDto.from(job)));
    }
}
