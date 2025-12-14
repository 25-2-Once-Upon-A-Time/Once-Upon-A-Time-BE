package pproject.once_upon_a_time.domain.story.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.script.domain.Script;
import pproject.once_upon_a_time.domain.script.repository.ScriptRepository;
import pproject.once_upon_a_time.domain.story.domain.GenerationStatus;
import pproject.once_upon_a_time.domain.story.domain.Story;
import pproject.once_upon_a_time.domain.story.dto.AiGenerationResponse; // [중요] 새 DTO 임포트
import pproject.once_upon_a_time.domain.story.dto.UserRequestDto;
import pproject.once_upon_a_time.domain.story.repository.StoryRepository;

import java.time.LocalDateTime;
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
        Story story = Story.builder()
            .member(member)
            .title(request.getTitle())
            .theme(request.getTheme())
            .vibe(request.getVibe())
            .originalPrompt(request.getOriginalPrompt())
            .generationStatus(GenerationStatus.PROCESSING)
            .build();
        return storyRepository.save(story);
    }

    /**
     * [트랜잭션 2] AI 응답을 바탕으로 동화와 스크립트를 DB에 최종 업데이트
     */
    @Transactional
    public void finalizeStory(Long storyId, AiGenerationResponse aiResponse) { // [변경] 파라미터 타입 변경
        Story story = storyRepository.findById(storyId)
            .orElseThrow(() -> new IllegalArgumentException("Story not found"));

        // 1. AI 응답으로 Story 정보 업데이트
        // [변경] DTO 구조 변경에 따른 데이터 추출 방식 수정
        // keywords와 summary는 이제 'storyInfo' 안에 있습니다.
        AiGenerationResponse.Metadata meta = aiResponse.metadata();
        AiGenerationResponse.StoryInfo info = aiResponse.storyInfo();

        story.updateWithAiResponse(
            aiResponse.content(),
            info.summary(),      // [수정] info에서 가져옴
            info.keywords(),     // [수정] info에서 가져옴
            meta.projectName(),
            meta.version(),
            meta.modelType(),
            meta.totalSegments()
        );

        // [참고] 현재 Python 로직에는 썸네일 생성 기능이 없으므로 null 처리하거나
        // 추후 이미지 생성 로직 추가 시 주석 해제하세요.
        // story.updateThumbnailUrl(null);

        story.updateCompletedAt(LocalDateTime.now());

        // 2. AI 응답으로 Script 엔티티 리스트 생성 및 저장
        final Story finalStory = story;

        // [변경] aiResponse.scripts() -> aiResponse.script() (단수형으로 변경됨)
        List<Script> scripts = aiResponse.script().stream()
            .map(item -> Script.builder() // item은 ScriptItem Record
                .story(finalStory)
                .seq(item.seq())
                .role(item.role())
                .text(item.text())
                // [중요] 현재 단계(텍스트 생성)에서는 오디오 파일이 아직 없습니다.
                // 오디오는 추후 TTS 단계에서 생성되므로 여기선 null로 둡니다.
                .audioFilePath(null)
                .build())
            .collect(Collectors.toList());

        scriptRepository.saveAll(scripts);

        // 3. Story 상태 'COMPLETED'로 변경
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
