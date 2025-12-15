package pproject.once_upon_a_time.domain.story.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.script.domain.Script;
import pproject.once_upon_a_time.domain.script.repository.ScriptRepository;
import pproject.once_upon_a_time.domain.story.domain.GenerationStatus;
import pproject.once_upon_a_time.domain.story.domain.Story;
// import pproject.once_upon_a_time.domain.story.dto.AiGenerationResponse; // 기존 DTO 제거
import pproject.once_upon_a_time.domain.story.dto.AiStoryResponseDto; // [NEW] 새로운 DTO 임포트
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
     * 파라미터: AiGenerationResponse -> AiStoryResponseDto로 변경
     */
    @Transactional
    public void finalizeStory(Long storyId, AiStoryResponseDto aiResponse) { // [수정] DTO 타입 변경
        Story story = storyRepository.findById(storyId)
            .orElseThrow(() -> new IllegalArgumentException("Story not found"));

        // 새로운 DTO 구조에 맞게 데이터 추출
        AiStoryResponseDto.AiStoryData data = aiResponse.getData();
        AiStoryResponseDto.StoryInfo info = data.getStoryInfo();
        AiStoryResponseDto.ScriptSegment firstScript = data.getScript().get(0); // 예시: 첫 번째 스크립트

        // 1. AI 응답으로 Story 정보 업데이트
        // 참고: AiStoryResponseDto는 레코드(Record)가 아닌 일반 클래스이므로 getter를 사용합니다.
        story.updateWithAiResponse(
            data.getContent(), // [수정] content 필드 사용
            info.getSummary(), // [수정] info.getSummary() 사용
            data.getTags(),    // [수정] data.getTags() 사용 (keywords 대신 tags 사용)
            (String) data.getMetadata().get("version"), // [수정] Map에서 version 추출
            (String) data.getMetadata().get("model_type"), // [수정] Map에서 model_type 추출
            (int) data.getMetadata().get("total_segments") // [수정] Map에서 total_segments 추출
        );

        // 2. AI 응답으로 Script 엔티티 리스트 생성 및 저장
        final Story finalStory = story;

        List<Script> scripts = data.getScript().stream() // [수정] data.getScript() 사용
            .map(item -> Script.builder()
                .story(finalStory)
                .seq(item.getSeq())
                .role(item.getRole())
                .text(item.getText())
                .emotion(item.getEmotion())
                .audioFilePath(item.getAudioFileName()) // [수정] audioFileName 사용
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