package pproject.once_upon_a_time.domain.audiobook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Schema(description = "오디오북 생성 요청 DTO")
public class AudioBookMakeRequest {

    @NotNull
    @Schema(description = "스토리 ID", example = "1")
    private Long storyId;

    @NotNull
    @Schema(description = "캐릭터 ID", example = "2")
    private Long characterId;
}
