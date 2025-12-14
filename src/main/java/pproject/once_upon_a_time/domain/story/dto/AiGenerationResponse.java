package pproject.once_upon_a_time.domain.story.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// Python의 JSON 출력 구조와 1:1 매핑되는 DTO
public record AiGenerationResponse(
    Metadata metadata,
    @JsonProperty("story_info") StoryInfo storyInfo, // Python의 snake_case를 매핑
    List<ScriptItem> script,
    String content,
    @JsonProperty("verification_status") String verificationStatus
) {
    public record Metadata(
        @JsonProperty("project_name") String projectName,
        String version,
        @JsonProperty("model_type") String modelType,
        @JsonProperty("total_segments") Integer totalSegments
    ) {}

    public record StoryInfo(
        String title,
        String theme,
        String vibe,
        @JsonProperty("original_prompt") String originalPrompt,
        @JsonProperty("target_age") String targetAge,
        String summary,
        List<String> keywords
    ) {}

    // ScriptItem은 기존에 만드신 클래스를 쓰셔도 되지만, Record로 통일하는 게 깔끔합니다.
    public record ScriptItem(
        Integer seq,
        String role,
        String text,
        String emotion
    ) {}
}