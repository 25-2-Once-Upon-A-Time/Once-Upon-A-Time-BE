package pproject.once_upon_a_time.domain.audiobook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pproject.once_upon_a_time.domain.playback.domain.PlaybackStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "플레이백 조회 응답 DTO")
public class PlaybackInfoResponse {

    @Schema(description = "플레이백 ID", example = "1")
    private Long playbackId;

    @Schema(description = "오디오북 ID", example = "10")
    private Long audiobookId;

    @Schema(description = "마지막 재생 위치(초)", example = "120")
    private Integer lastPosition;

    @Schema(description = "진행률(0.0 ~ 1.0)", example = "0.5")
    private Float progressRate;

    @Schema(description = "재생 상태", example = "PLAYING")
    private PlaybackStatus status;
}
