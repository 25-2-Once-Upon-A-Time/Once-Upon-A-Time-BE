package pproject.once_upon_a_time.domain.story.dto;

import lombok.Data;

@Data
public class UserRequestDto {
    private String title;
    private String theme;
    private String vibe;
    private String prompt;
}
