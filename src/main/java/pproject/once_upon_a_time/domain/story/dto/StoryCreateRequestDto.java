package pproject.once_upon_a_time.domain.story.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import pproject.once_upon_a_time.domain.story.domain.Story;

@Getter
@NoArgsConstructor
public class StoryCreateRequestDto {

    private String theme;
    private String vibe;
    private String prompt;
    private String title;

    public Story toEntity() {
        return Story.builder()
                .theme(theme)
                .vibe(vibe)
                .originalPrompt(prompt)
                .title(title)
                .build();
    }
}
