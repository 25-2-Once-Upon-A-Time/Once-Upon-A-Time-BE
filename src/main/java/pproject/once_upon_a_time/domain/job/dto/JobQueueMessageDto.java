package pproject.once_upon_a_time.domain.job.dto;

import lombok.Builder;
import lombok.Getter;
import pproject.once_upon_a_time.domain.job.domain.JobType;

import java.util.UUID;

@Getter
@Builder
public class JobQueueMessageDto {
    private UUID jobId;
    private JobType jobType;
    private String inputKey;
}
