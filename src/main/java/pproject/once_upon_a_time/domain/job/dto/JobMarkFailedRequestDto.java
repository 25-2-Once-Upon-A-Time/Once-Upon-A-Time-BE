package pproject.once_upon_a_time.domain.job.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class JobMarkFailedRequestDto {

    @NotBlank(message = "errorMessage는 필수입니다.")
    private String errorMessage;
}
