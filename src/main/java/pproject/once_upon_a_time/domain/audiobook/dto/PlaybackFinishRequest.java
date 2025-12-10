package pproject.once_upon_a_time.domain.audiobook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pproject.once_upon_a_time.domain.playback.domain.PlaybackStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "플레이백 완료 요청 DTO")
public class PlaybackFinishRequest {

    @Schema(description = "최종 재생 위치(초)", example = "320")
    private Integer finalPosition;

    @Schema(description = "재생 상태", example = "COMPLETED")
    private PlaybackStatus status;
}
