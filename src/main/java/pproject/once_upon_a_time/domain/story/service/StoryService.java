package pproject.once_upon_a_time.domain.story.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.member.repository.MemberRepository;
import pproject.once_upon_a_time.domain.story.domain.Story;
import pproject.once_upon_a_time.domain.story.dto.UserRequestDto;
import pproject.once_upon_a_time.domain.story.dto.StoryCreateResponseDto;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryUpdateService storyUpdateService;
    private final AiClient aiClient;
    private final MemberRepository memberRepository;

    public StoryCreateResponseDto createStory(Long memberId, UserRequestDto request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Story story = storyUpdateService.initiateStory(request, member);

        AiResponse aiResponse = aiClient.generateStory(request);
        if (aiResponse == null) {
            storyUpdateService.finalizeStoryOnError(story.getId());
            throw new RuntimeException("AI 서버로부터 응답을 받지 못했습니다.");
        }

        storyUpdateService.finalizeStory(story.getId(), aiResponse);

        return new StoryCreateResponseDto(story.getId());
    }
}