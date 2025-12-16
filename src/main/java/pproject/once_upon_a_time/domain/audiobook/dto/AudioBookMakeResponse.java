package pproject.once_upon_a_time.domain.audiobook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Schema(description = "오디오북 생성 응답 DTO")
public class AudioBookMakeResponse {

    @Schema(description = "오디오북 ID", example = "10")
    private Long audioBookId;

    @Schema(description = "오디오북 URL", example = "https://mock.storage.local/audiobooks/title_voice.wav")
    private String audioUrl;

    @Schema(description = "총 재생 시간(초, 소수점 허용)", example = "12.34")
    private Double duration;

    @Schema(description = "스토리 ID", example = "1")
    private Long storyId;

    @Schema(description = "캐릭터 ID", example = "2")
    private Long characterId;
}
