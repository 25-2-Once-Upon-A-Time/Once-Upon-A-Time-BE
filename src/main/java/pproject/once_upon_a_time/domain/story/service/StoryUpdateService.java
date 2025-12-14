package pproject.once_upon_a_time.domain.story.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.script.domain.Script;
import pproject.once_upon_a_time.domain.script.repository.ScriptRepository;
import pproject.once_upon_a_time.domain.story.domain.GenerationStatus;
import pproject.once_upon_a_time.domain.story.domain.Story;
import pproject.once_upon_a_time.domain.story.dto.AiGenerationResponse;
import pproject.once_upon_a_time.domain.story.dto.UserRequestDto;
import pproject.once_upon_a_time.domain.story.repository.StoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryUpdateService {

    private final StoryRepository storyRepository;
    private final ScriptRepository scriptRepository;

    /**
     * [트랜잭션 1] 동화 생성을 시작하고, 초기 상태로 DB에 저장
     */
    @Transactional
    public Story initiateStory(UserRequestDto request, Member member) {
        // 빌더에서도 projectName 제외됨 (엔티티에서 삭제했으므로)
        Story story = Story.builder()
            .member(member)
            .title(request.getTitle())
            .theme(request.getTheme())
            .vibe(request.getVibe())
            .originalPrompt(request.getPrompt())
            .generationStatus(GenerationStatus.PROCESSING)
            .build();
        return storyRepository.save(story);
    }

    /**
     * [트랜잭션 2] AI 응답을 바탕으로 동화와 스크립트를 DB에 최종 업데이트
     */
    @Transactional
    public void finalizeStory(Long storyId, AiGenerationResponse aiResponse) {
        Story story = storyRepository.findById(storyId)
            .orElseThrow(() -> new IllegalArgumentException("Story not found"));

        // 1. AI 응답으로 Story 정보 업데이트
        AiGenerationResponse.Metadata meta = aiResponse.metadata();
        AiGenerationResponse.StoryInfo info = aiResponse.storyInfo();

        // [수정 1] meta.projectName() 제거 (총 6개 인자)
        story.updateWithAiResponse(
            aiResponse.content(),
            info.summary(),
            info.keywords(),
            meta.version(),
            meta.modelType(),
            meta.totalSegments()
        );

        // [수정 2] updateCompletedAt() 제거
        // -> 아래 completeGeneration() 내부에서 this.completedAt = LocalDateTime.now()가 실행됨

        // 2. AI 응답으로 Script 엔티티 리스트 생성 및 저장
        final Story finalStory = story;

        List<Script> scripts = aiResponse.script().stream()
            .map(item -> Script.builder()
                .story(finalStory)
                .seq(item.seq())
                .role(item.role())
                .text(item.text())
                .emotion(item.emotion()) // [추가] DTO에서 감정 꺼내서 저장
                .audioFilePath(null)
                .build())
            .collect(Collectors.toList());

        scriptRepository.saveAll(scripts);

        // 3. Story 상태 'COMPLETED'로 변경 (시간 자동 기록)
        story.completeGeneration();
    }

    /**
     * [트랜잭션 N] 동화 생성 실패 시 상태 업데이트
     */
    @Transactional
    public void finalizeStoryOnError(Long storyId) {
        Story story = storyRepository.findById(storyId)
            .orElseThrow(() -> new IllegalArgumentException("Story not found"));
        story.failGeneration();
    }
}
