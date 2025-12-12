package pproject.once_upon_a_time.domain.story.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pproject.once_upon_a_time.domain.story.dto.UserRequestDto;

import java.util.List;

@Profile("dev") // 'dev' 프로필에서만 활성화되는 Mock 클라이언트
@Service
public class MockAiClient implements AiClient {

    /**
     * 실제 AI 서버를 호출하는 대신, 테스트용 가짜 데이터를 생성하여 반환합니다.
     */
    @Override
    public AiResponse generateStory(UserRequestDto request) {
        // AI가 생성하는 것처럼 보이도록 약간의 지연 추가 (선택 사항)
        try {
            System.out.println("가짜 AiClient 호출됨. 2초간 동화 생성 시뮬레이션...");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 메타데이터 DTO 생성
        MetadataDto metadata = new MetadataDto(
                "MockAI-Project",
                "1.1.0",
                "mock-model-v2",
                2 // 생성할 스크립트 개수
        );

        // 스크립트 DTO 리스트 생성
        List<ScriptDto> scripts = List.of(
            new ScriptDto(1, "해설", "옛날 옛적, 작은 마을에 '"+ request.getTheme() + "'을(를) 주제로 한 이야기가 시작되었어요.", "dummy_path_01.wav"),
            new ScriptDto(2, "주인공", "정말 '" + request.getVibe() + "' 분위기인걸!", "dummy_path_02.wav")
        );

        // 최종 AiResponse 객체 생성 및 반환
        return new AiResponse(
            "'" + request.getTitle() + "'의 전체 내용입니다. " + request.getOriginalPrompt(),
            "'" + request.getTitle() + "'의 요약입니다.",
            List.of("테스트", "생성", "AI"),
            scripts,
            metadata,
            "https://picsum.photos/seed/a-new-story/400/600" // 이미지 다운로드 테스트용 URL
        );
    }
}
