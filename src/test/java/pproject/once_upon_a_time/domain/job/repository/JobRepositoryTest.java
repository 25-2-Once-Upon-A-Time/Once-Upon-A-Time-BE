package pproject.once_upon_a_time.domain.job.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.job.domain.Job;
import pproject.once_upon_a_time.domain.job.domain.JobStatus;
import pproject.once_upon_a_time.domain.job.domain.JobTargetType;
import pproject.once_upon_a_time.domain.job.domain.JobType;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JobRepositoryTest {

    @Autowired
    private JobRepository jobRepository;

    @Test
    @Transactional
    void updateStatusIfMatches_isIdempotent() {
        Job job = jobRepository.save(Job.builder()
            .type(JobType.STORY)
            .status(JobStatus.PENDING)
            .targetType(JobTargetType.STORY)
            .targetId(1L)
            .build());

        int first = jobRepository.updateStatusIfMatches(job.getId(), JobStatus.PENDING, JobStatus.RUNNING);
        int second = jobRepository.updateStatusIfMatches(job.getId(), JobStatus.PENDING, JobStatus.RUNNING);

        Job updated = jobRepository.findById(job.getId()).orElseThrow();

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(0);
        assertThat(updated.getStatus()).isEqualTo(JobStatus.RUNNING);
    }

    @Test
    @Transactional
    void markFailed_requiresRunningStatus() {
        Job job = jobRepository.save(Job.builder()
            .type(JobType.AUDIOBOOK)
            .status(JobStatus.PENDING)
            .targetType(JobTargetType.STORY)
            .targetId(1L)
            .build());

        int notRunning = jobRepository.markFailed(job.getId(), JobStatus.RUNNING, JobStatus.FAILED, "error");
        jobRepository.updateStatusIfMatches(job.getId(), JobStatus.PENDING, JobStatus.RUNNING);
        int running = jobRepository.markFailed(job.getId(), JobStatus.RUNNING, JobStatus.FAILED, "error");

        Job updated = jobRepository.findById(job.getId()).orElseThrow();

        assertThat(notRunning).isEqualTo(0);
        assertThat(running).isEqualTo(1);
        assertThat(updated.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(updated.getErrorMessage()).isEqualTo("error");
    }

    @Test
    @Transactional
    void markSucceeded_setsOutputKey() {
        Job job = jobRepository.save(Job.builder()
            .type(JobType.ILLUSTRATION)
            .status(JobStatus.PENDING)
            .targetType(JobTargetType.STORY)
            .targetId(1L)
            .errorMessage("old")
            .build());

        jobRepository.updateStatusIfMatches(job.getId(), JobStatus.PENDING, JobStatus.RUNNING);
        int updatedCount = jobRepository.markSucceeded(
            job.getId(),
            JobStatus.RUNNING,
            JobStatus.SUCCEEDED,
            "jobs/1/result.json"
        );

        Job updated = jobRepository.findById(job.getId()).orElseThrow();

        assertThat(updatedCount).isEqualTo(1);
        assertThat(updated.getStatus()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(updated.getOutputKey()).isEqualTo("jobs/1/result.json");
        assertThat(updated.getErrorMessage()).isNull();
    }
}
