package pproject.once_upon_a_time.domain.job.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.job.domain.Job;
import pproject.once_upon_a_time.domain.job.domain.JobStatus;
import pproject.once_upon_a_time.domain.job.domain.JobTargetType;
import pproject.once_upon_a_time.domain.job.domain.JobType;
import pproject.once_upon_a_time.domain.job.repository.JobRepository;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final JobQueuePublisher jobQueuePublisher;
    private final SpotInstanceService spotInstanceService;
    private final JobResultProcessor jobResultProcessor;

    @Transactional
    public Job createJob(JobType jobType, JobTargetType targetType, Long targetId, String inputKey) {
        Job job = Job.builder()
            .type(jobType)
            .status(JobStatus.PENDING)
            .targetType(targetType)
            .targetId(targetId)
            .inputKey(inputKey)
            .build();
        return jobRepository.save(job);
    }

    @Transactional
    public Job createJobAndPublish(JobType jobType, JobTargetType targetType, Long targetId, String inputKey) {
        Job job = createJob(jobType, targetType, targetId, inputKey);
        publishJob(job);
        return job;
    }

    @Transactional
    public void publishJob(Job job) {
        spotInstanceService.ensureSpotInstanceRunning();
        jobQueuePublisher.publish(job);
    }

    @Transactional
    public void updateInputKey(Job job, String inputKey) {
        job.updateInputKey(inputKey);
    }

    @Transactional
    public boolean markRunning(UUID jobId) {
        boolean updated = jobRepository.updateStatusIfMatches(jobId, JobStatus.PENDING, JobStatus.RUNNING) == 1;
        if (updated) {
            log.info("Job 상태 업데이트: id={} -> RUNNING", jobId);
        } else {
            log.warn("Job 상태 업데이트 실패: id={} current!=PENDING", jobId);
        }
        return updated;
    }

    @Transactional
    public boolean markSucceeded(UUID jobId, String outputKey) {
        boolean updated = jobRepository.markSucceeded(jobId, JobStatus.RUNNING, JobStatus.SUCCEEDED, outputKey) == 1;
        if (updated) {
            log.info("Job 상태 업데이트: id={} -> SUCCEEDED outputKey={}", jobId, outputKey);
            Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));
            jobResultProcessor.applySuccess(job);
        } else {
            log.warn("Job 상태 업데이트 실패: id={} current!=RUNNING", jobId);
        }
        return updated;
    }

    @Transactional
    public boolean markFailed(UUID jobId, String errorMessage) {
        boolean updated = jobRepository.markFailed(jobId, JobStatus.RUNNING, JobStatus.FAILED, errorMessage) == 1;
        if (updated) {
            log.warn("Job 상태 업데이트: id={} -> FAILED error={}", jobId, errorMessage);
            Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));
            jobResultProcessor.applyFailure(job);
        } else {
            log.warn("Job 상태 업데이트 실패: id={} current!=RUNNING", jobId);
        }
        return updated;
    }

    @Transactional(readOnly = true)
    public Job getJob(UUID jobId) {
        return jobRepository.findById(jobId)
            .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));
    }
}
