package pproject.once_upon_a_time.domain.job.dto;

import lombok.Builder;
import lombok.Getter;
import pproject.once_upon_a_time.domain.job.domain.Job;
import pproject.once_upon_a_time.domain.job.domain.JobStatus;
import pproject.once_upon_a_time.domain.job.domain.JobType;

import java.util.UUID;

@Getter
@Builder
public class JobResponseDto {
    private UUID id;
    private JobType type;
    private JobStatus status;
    private String inputKey;
    private String outputKey;
    private String errorMessage;

    public static JobResponseDto from(Job job) {
        return JobResponseDto.builder()
            .id(job.getId())
            .type(job.getType())
            .status(job.getStatus())
            .inputKey(job.getInputKey())
            .outputKey(job.getOutputKey())
            .errorMessage(job.getErrorMessage())
            .build();
    }
}
