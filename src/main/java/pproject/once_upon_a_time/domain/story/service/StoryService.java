package pproject.once_upon_a_time.domain.story.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.member.repository.MemberRepository;
import pproject.once_upon_a_time.domain.story.domain.Story;
import pproject.once_upon_a_time.domain.story.dto.*;
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
    private final AiProcessService aiProcessService;
    private final MemberRepository memberRepository;
    private final StoryRepository storyRepository;

    // [수정] AiGenerationResponse -> AiStoryResponseDto
    public StoryDetailResponseDto createStory(Long memberId, UserRequestDto request) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 1. 초기 저장 (상태: PROCESSING, 내용은 비어있음)
        Story story = storyUpdateService.initiateStory(request, member);

        // [수정] AiGenerationResponse -> AiStoryResponseDto
        AiStoryResponseDto aiResponse;
        try {
            // 2. AI 프로세스 실행 (Python 호출, 시간 소요됨)
            log.info("Requesting story generation to AI Process for storyId: {}", story.getId());
            aiResponse = aiProcessService.generateStory(request);

        } catch (Exception e) {
            log.error("AI story generation failed for storyId: {}", story.getId(), e);
            storyUpdateService.finalizeStoryOnError(story.getId());
            throw new RuntimeException("AI 서버로부터 동화를 생성하는 데 실패했습니다.", e);
        }

        // 3. 최종 업데이트 (DB에 내용 저장 및 상태 COMPLETED로 변경)
        // [수정] aiResponse를 AiStoryResponseDto로 받으므로, StoryUpdateService의 파라미터도
        //       AiStoryResponseDto를 받도록 수정해야 합니다. (이 로직은 StoryUpdateService 내에 있다고 가정)
        log.info("Finalizing story for storyId: {}", story.getId());
        storyUpdateService.finalizeStory(story.getId(), aiResponse);

        // [중요] DB에는 내용이 저장됐지만, 위 변수 'story'는 아직 빈 껍데기 상태입니다.
        // 클라이언트에게 완성된 내용을 바로 보여주기 위해, DB에서 최신 정보를 다시 가져옵니다.
        Story updatedStory = storyRepository.findById(story.getId())
            .orElseThrow(() -> new CustomException(ErrorCode.STORY_NOT_FOUND));

        // [변경] 상세 DTO 반환
        return new StoryDetailResponseDto(updatedStory);
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
