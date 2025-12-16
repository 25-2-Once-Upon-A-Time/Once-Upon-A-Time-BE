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
        // [수정 완료] Map -> List<Map> 으로 변경!
        // 에러 원인: AI가 배열([])을 주는데 Map({})으로 받으려 했음.
        private List<Map<String, Object>> characters;
        private List<String> tags;
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