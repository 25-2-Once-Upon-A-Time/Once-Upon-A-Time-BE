package pproject.once_upon_a_time.domain.job.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JobResultResponseDto {
    private String outputKey;
    private String presignedUrl;
}
