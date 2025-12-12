package pproject.once_upon_a_time.domain.story.dto;

import lombok.Data;

@Data
public class UserRequestDto {
    private String title;
    private String theme;
    private String vibe;
    private String originalPrompt;
    // memberId는 SecurityContext에서 가져옴
    // targetAge는 AI 응답 필드에 없으므로, 요청에 포함될 경우 여기에 추가
}
