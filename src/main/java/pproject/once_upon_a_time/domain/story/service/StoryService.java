package pproject.once_upon_a_time.domain.story.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.story.domain.Story;
import pproject.once_upon_a_time.domain.story.dto.StoryCreateRequestDto;
import pproject.once_upon_a_time.domain.story.dto.StoryCreateResponseDto;
import pproject.once_upon_a_time.domain.story.dto.StoryDetailResponseDto;
import pproject.once_upon_a_time.domain.story.dto.StoryListResponseDto;
import pproject.once_upon_a_time.domain.story.repository.StoryRepository;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StoryService {

    private final StoryRepository storyRepository;

    public List<StoryListResponseDto> findAllStories(String keyword) {
        List<Story> stories;
        if (keyword == null || keyword.isBlank()) {
            stories = storyRepository.findAll();
        } else {
            stories = storyRepository.findByTitleContainingIgnoreCase(keyword);
        }
        return stories.stream()
                .map(StoryListResponseDto::new)
                .collect(Collectors.toList());
    }

    public StoryDetailResponseDto findStoryById(Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORY_NOT_FOUND));
        return new StoryDetailResponseDto(story);
    }

    @Transactional
    public StoryCreateResponseDto createStory(StoryCreateRequestDto requestDto) {
        Story story = requestDto.toEntity();
        Story savedStory = storyRepository.save(story);
        return new StoryCreateResponseDto(savedStory.getId());
    }
}
