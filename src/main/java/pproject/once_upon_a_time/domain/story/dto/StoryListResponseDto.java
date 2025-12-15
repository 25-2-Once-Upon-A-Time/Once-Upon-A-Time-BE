package pproject.once_upon_a_time.domain.story.dto;

import lombok.Getter;
import pproject.once_upon_a_time.domain.story.domain.Story;

import java.time.LocalDateTime;

@Getter

public class StoryListResponseDto {
    private final Long id;
    private final String title;
    private final String thumbnailUrl;
    private final LocalDateTime createdDate;

    public StoryListResponseDto(Story story) {
        this.id = story.getId();
        this.title = story.getTitle();
        this.thumbnailUrl = story.getThumbnailUrl();
        this.createdDate = story.getCreatedDate();
    }
}
