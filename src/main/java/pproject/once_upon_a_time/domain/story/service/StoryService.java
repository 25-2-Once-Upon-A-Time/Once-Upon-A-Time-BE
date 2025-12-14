package pproject.once_upon_a_time.domain.story.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.member.repository.MemberRepository;
import pproject.once_upon_a_time.domain.story.domain.Story;
import pproject.once_upon_a_time.domain.story.dto.*; // AiGenerationResponse 포함
import pproject.once_upon_a_time.domain.story.repository.StoryRepository;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryUpdateService storyUpdateService;
    // [변경] 기존 AiClient 제거 -> AiProcessService 주입
    private final AiProcessService aiProcessService;
    private final MemberRepository memberRepository;
    private final StoryRepository storyRepository;

    public StoryCreateResponseDto createStory(Long memberId, UserRequestDto request) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 1. 초기 저장 (트랜잭션 O)
        Story story = storyUpdateService.initiateStory(request, member);

        // [변경] DTO 타입 변경 (AiResponse -> AiGenerationResponse)
        AiGenerationResponse aiResponse;
        try {
            // 2. AI 프로세스 실행 (트랜잭션 X)
            log.info("Requesting story generation to AI Process for storyId: {}", story.getId());

            // [변경] generateStory 호출 대상 변경
            aiResponse = aiProcessService.generateStory(request);

        } catch (Exception e) {
            log.error("AI story generation failed for storyId: {}", story.getId(), e);
            // 실패 상태로 업데이트
            storyUpdateService.finalizeStoryOnError(story.getId());
            throw new RuntimeException("AI 서버로부터 동화를 생성하는 데 실패했습니다.", e);
        }

        // 3. 최종 업데이트 (트랜잭션 O)
        log.info("Finalizing story for storyId: {}", story.getId());
        storyUpdateService.finalizeStory(story.getId(), aiResponse);

        return new StoryCreateResponseDto(story.getId());
    }

    @Transactional(readOnly = true)
    public List<StoryListResponseDto> getStoryList(String keyword) {
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

    @Transactional(readOnly = true)
    public StoryDetailResponseDto getStoryDetail(Long storyId) {
        Story story = storyRepository.findById(storyId)
            .orElseThrow(() -> new CustomException(ErrorCode.STORY_NOT_FOUND));
        return new StoryDetailResponseDto(story);
    }
}
