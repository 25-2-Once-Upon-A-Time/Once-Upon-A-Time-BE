package pproject.once_upon_a_time.domain.story.dto;

import lombok.Getter;
import pproject.once_upon_a_time.domain.story.domain.Story;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class StoryDetailResponseDto {

    private final Long storyId; // 엔티티의 id를 storyId로 변경
    private final String title;
    private final String summary;
    private final String content;
    private final String thumbnailUrl;
    private final List<String> keywords;
    private final LocalDateTime createdDate;
    private final String theme; // 예: "용기"
    private final String vibe;  // 예: "따뜻한"

    public StoryDetailResponseDto(Story story) {
        this.storyId = story.getId(); // 엔티티의 getId() 사용
        this.title = story.getTitle();
        this.summary = story.getSummary();
        this.content = story.getContent();
        this.thumbnailUrl = story.getThumbnailUrl();
        this.keywords = story.getKeywords();
        this.createdDate = story.getCreatedDate();
        this.theme = story.getTheme();
        this.vibe = story.getVibe();
    }
}
