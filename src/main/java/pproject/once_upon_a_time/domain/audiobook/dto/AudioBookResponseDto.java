package pproject.once_upon_a_time.domain.audiobook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 오디오북 리스트 응답 DTO.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "오디오북 리스트 응답 DTO")
public class AudioBookResponseDto {

    @Schema(description = "오디오북 리스트")
    private List<Item> items;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "오디오북 항목 DTO")
    public static class Item {
        @Schema(description = "오디오북 ID", example = "1")
        private Long audiobookId;

        @Schema(description = "오디오북 이름(스토리 제목)", example = "헨젤과 그레텔")
        private String audiobookName;
    }
}
