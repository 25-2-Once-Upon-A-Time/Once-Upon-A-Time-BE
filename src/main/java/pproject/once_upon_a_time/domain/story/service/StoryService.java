package pproject.once_upon_a_time.domain.story.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.story.domain.Story;
import pproject.once_upon_a_time.domain.story.dto.StoryDetailResponseDto;
import pproject.once_upon_a_time.domain.story.dto.StoryListResponseDto;
import pproject.once_upon_a_time.domain.story.repository.StoryRepository;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryRepository storyRepository;

    @Transactional(readOnly = true)
    public List<StoryListResponseDto> getStoryList(String keyword, Long memberId) {
        List<Story> stories;
        if (keyword == null || keyword.isBlank()) {
            stories = storyRepository.findByMemberId(memberId);
        } else {
            stories = storyRepository.findByMemberIdAndTitleContainingIgnoreCase(memberId, keyword);
        }
        return stories.stream()
            .map(StoryListResponseDto::new)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StoryDetailResponseDto getStoryDetail(Long storyId) {
        Story story = storyRepository.findById(storyId)
            .orElseThrow(() -> new CustomException(ErrorCode.STORY_NOT_FOUND));
        return new StoryDetailResponseDto(story);
    }
}
