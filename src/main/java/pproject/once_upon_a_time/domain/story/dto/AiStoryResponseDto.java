package pproject.once_upon_a_time.domain.story.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

// AI가 반환하는 최상위 응답 구조 (SUCCESS와 DATA를 포함)
@Getter
@NoArgsConstructor
public class AiStoryResponseDto {
    private boolean success;
    private AiStoryData data;
    private String timestamp;
    private String code; // 오류 발생 시 사용될 수 있음

    // -------------------------------------------------------------------
    // 1. DATA 객체: 실제 동화 정보와 스크립트가 담긴 메인 컨테이너
    // -------------------------------------------------------------------

    @Getter
    @NoArgsConstructor
    public static class AiStoryData {
        private Map<String, Object> metadata;

        @JsonProperty("story_info")
        private StoryInfo storyInfo;

        private List<ScriptSegment> script;

        private String content;
        private String category;
        private String moral;

        // [최종 수정] List<Map> -> Object 로 변경
        // 이유: AI가 리스트([])를 줄 때도 있고, 그냥 설명글("")을 줄 때도 있음
        private Object characters;

        private List<String> tags;

        // [추가] 안전하게 캐릭터 리스트를 반환하는 메서드
        // 서비스(Service) 계층에서는 getCharacters() 대신 이걸 호출하세요!
        public List<Map<String, Object>> getSafeCharacters() {
            if (characters instanceof List) {
                // 리스트로 왔을 경우 그대로 반환
                return (List<Map<String, Object>>) characters;
            }
            // 문자열이나 다른 걸로 왔을 경우 -> 빈 리스트 반환 (에러 방지)
            // 필요하다면 여기서 문자열을 파싱해서 Map으로 만드는 로직을 추가할 수도 있음
            return java.util.Collections.emptyList();
        }

        // [추가] 캐릭터 설명글이 필요할 때 쓰는 메서드
        public String getCharactersDescription() {
            if (characters instanceof String) {
                return (String) characters;
            }
            return "";
        }
    }

    // -------------------------------------------------------------------
    // 2. STORY_INFO 객체: 동화의 제목, 주제, 요약 등 핵심 정보
    // -------------------------------------------------------------------

    @Getter
    @NoArgsConstructor
    public static class StoryInfo {
        private String title;
        private String theme;
        private String vibe;
        @JsonProperty("original_prompt")
        private String originalPrompt;
        @JsonProperty("target_age")
        private String targetAge;
        private String summary;
        // keywords는 tags와 중복되므로 제외하거나 필요시 추가
    }

    // -------------------------------------------------------------------
    // 3. SCRIPT 배열: 대화 및 오디오 파일 정보
    // -------------------------------------------------------------------

    @Getter
    @NoArgsConstructor
    public static class ScriptSegment {
        private int seq;
        private String role;
        private String text;
        private String emotion;
        @JsonProperty("audio_file_name")
        private String audioFileName;
    }
}