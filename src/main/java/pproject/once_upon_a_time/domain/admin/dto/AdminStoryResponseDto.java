package pproject.once_upon_a_time.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;
import pproject.once_upon_a_time.domain.story.domain.Story;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminStoryResponseDto {
    private Long storyId;
    private String title;
    private String writerName;
    private String theme;
    private LocalDateTime createdDate;

    public static AdminStoryResponseDto from(Story story) {
        return AdminStoryResponseDto.builder()
            .storyId(story.getId())
            .title(story.getTitle())
            .writerName(story.getMember().getName())
            .theme(story.getTheme())
            .createdDate(story.getCreatedDate())
            .build();
    }
}
