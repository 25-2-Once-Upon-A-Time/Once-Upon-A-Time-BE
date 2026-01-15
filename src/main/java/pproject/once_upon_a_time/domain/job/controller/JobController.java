package pproject.once_upon_a_time.domain.job.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pproject.once_upon_a_time.domain.job.domain.Job;
import pproject.once_upon_a_time.domain.job.dto.JobCreateRequestDto;
import pproject.once_upon_a_time.domain.job.dto.JobResultResponseDto;
import pproject.once_upon_a_time.domain.job.dto.JobResponseDto;
import pproject.once_upon_a_time.domain.job.service.JobService;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;
import pproject.once_upon_a_time.global.response.ApiResult;
import pproject.once_upon_a_time.global.storage.S3StorageService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final S3StorageService s3StorageService;

    @PostMapping
    public ResponseEntity<ApiResult<JobResponseDto>> createJob(
        @RequestBody @Valid JobCreateRequestDto request
    ) {
        Job job = jobService.createJobAndPublish(
            request.getType(),
            request.getTargetType(),
            request.getTargetId(),
            request.getInputKey()
        );
        ApiResult<JobResponseDto> body = ApiResult.created(JobResponseDto.from(job));
        return ResponseEntity.status(body.httpStatus()).body(body);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResult<JobResponseDto>> getJob(@PathVariable UUID jobId) {
        Job job = jobService.getJob(jobId);
        return ResponseEntity.ok(ApiResult.ok(JobResponseDto.from(job)));
    }

    @GetMapping("/{jobId}/result")
    public ResponseEntity<ApiResult<JobResultResponseDto>> getJobResult(@PathVariable UUID jobId) {
        Job job = jobService.getJob(jobId);
        String outputKey = job.getOutputKey();
        if (outputKey == null || outputKey.isBlank()) {
            throw new CustomException(ErrorCode.JOB_RESULT_NOT_READY);
        }
        String presignedUrl = s3StorageService.createPresignedGetUrl(outputKey);
        JobResultResponseDto responseDto = JobResultResponseDto.builder()
            .outputKey(outputKey)
            .presignedUrl(presignedUrl)
            .build();
        return ResponseEntity.ok(ApiResult.ok(responseDto));
    }
}
