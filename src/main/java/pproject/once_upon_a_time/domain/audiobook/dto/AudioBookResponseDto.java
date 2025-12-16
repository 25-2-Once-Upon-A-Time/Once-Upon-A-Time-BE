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

        @Schema(description = "동화 제목", example = "숲속의 용감한 토끼")
        private String storyTitle;

        @Schema(description = "동화 테마", example = "용기")
        private String theme;

        @Schema(description = "동화 분위기", example = "따뜻하고 서정적")
        private String vibe;

        @Schema(description = "썸네일 URL", example = "https://example.com/thumbnail.jpg")
        private String thumbnailUrl;

        @Schema(description = "캐릭터 이름", example = "루비")
        private String characterName;

        @Schema(description = "오디오북 길이(초, 소수점 허용)", example = "600.5")
        private Double duration;
    }
}
