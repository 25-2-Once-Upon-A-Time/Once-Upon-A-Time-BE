package pproject.once_upon_a_time.domain.audiobook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pproject.once_upon_a_time.domain.playback.domain.PlaybackStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "플레이백 진행도 업데이트 요청 DTO")
public class PlaybackProgressRequest {

    @Schema(description = "현재 재생 위치(초)", example = "120")
    private Integer currentTime;

    @Schema(description = "재생 상태", example = "PLAYING")
    private PlaybackStatus status;
}
