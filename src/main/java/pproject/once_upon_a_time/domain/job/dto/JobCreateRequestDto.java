package pproject.once_upon_a_time.domain.job.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pproject.once_upon_a_time.domain.job.domain.JobType;

@Getter
@NoArgsConstructor
public class JobCreateRequestDto {

    @NotNull
    private JobType type;

    private String inputKey;
}
