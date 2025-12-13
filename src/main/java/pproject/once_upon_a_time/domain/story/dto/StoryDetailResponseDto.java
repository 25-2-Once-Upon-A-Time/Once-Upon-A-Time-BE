package pproject.once_upon_a_time.domain.story.dto;

import lombok.Getter;
import pproject.once_upon_a_time.domain.story.domain.Story;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class StoryDetailResponseDto {



    private final Long id;

    private final String title;

    private final String summary;

    private final String content;

    private final String thumbnailUrl;

    private final List<String> keywords;

    private final LocalDateTime createdDate;



    public StoryDetailResponseDto(Story story) {

        this.id = story.getId();

        this.title = story.getTitle();

        this.summary = story.getSummary();

        this.content = story.getContent();

        this.thumbnailUrl = story.getThumbnailUrl();

        this.keywords = story.getKeywords();

        this.createdDate = story.getCreatedDate();

    }

}
