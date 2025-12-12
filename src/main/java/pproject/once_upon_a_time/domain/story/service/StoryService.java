package pproject.once_upon_a_time.domain.story.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.member.repository.MemberRepository;
import pproject.once_upon_a_time.domain.story.domain.Story;
import pproject.once_upon_a_time.domain.story.dto.StoryCreateResponseDto;
import pproject.once_upon_a_time.domain.story.dto.UserRequestDto;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {



    private final StoryUpdateService storyUpdateService;

    private final AiClient aiClient;

    private final MemberRepository memberRepository;



    public StoryCreateResponseDto createStory(Long memberId, UserRequestDto request) {

        Member member = memberRepository.findById(memberId)

                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));



        // 1. 초기 저장 (트랜잭션 O)

        Story story = storyUpdateService.initiateStory(request, member);

        

        AiResponse aiResponse;

        try {

            // 2. AI 호출 (트랜잭션 X)

            log.info("Requesting story generation to AI server for storyId: {}", story.getId());

            aiResponse = aiClient.generateStory(request);

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

}
