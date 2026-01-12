package pproject.once_upon_a_time.domain.audiobook.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pproject.once_upon_a_time.auth.annotation.LoginUser;
import pproject.once_upon_a_time.domain.audiobook.dto.AudioBookMakeRequest;
import pproject.once_upon_a_time.domain.audiobook.dto.AudioBookResponseDto;
import pproject.once_upon_a_time.domain.audiobook.service.AudioBookService;
import pproject.once_upon_a_time.domain.job.domain.Job;
import pproject.once_upon_a_time.domain.job.dto.JobResponseDto;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.global.response.ApiResult;
import pproject.once_upon_a_time.global.response.ExceptionDto;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Audiobooks", description = "오디오북 조회 API")
public class AudioBookController {

    private final AudioBookService audioBookService;

    @GetMapping(value = "/audiobooks", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "오디오북 리스트 조회",
            description = "로그인한 사용자의 오디오북 리스트를 반환합니다.",
            security = @SecurityRequirement(name = "JWT TOKEN")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AudioBookResponseApiResult.class))),
            @ApiResponse(responseCode = "404", description = "오디오북을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResult<AudioBookResponseDto>> getAudioBooks(
            @Parameter(hidden = true) @LoginUser Member member) {
        AudioBookResponseDto response = audioBookService.getAudioBooks(member);
        var body = ApiResult.ok(response);
        return ResponseEntity.status(body.httpStatus()).body(body);
    }

    @PostMapping(
            value = "/audiobook/make",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "오디오북 생성",
            description = "스토리와 캐릭터를 선택해 오디오북을 생성합니다.",
            security = @SecurityRequirement(name = "JWT TOKEN")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 입력"),
            @ApiResponse(responseCode = "404", description = "스토리 또는 캐릭터 없음")
    })
    public ResponseEntity<ApiResult<JobResponseDto>> makeAudioBook(
            @Valid @RequestBody AudioBookMakeRequest request,
            @Parameter(hidden = true) @LoginUser Member member
    ) {
        Job job = audioBookService.createAudioBookJob(request.getStoryId(), request.getCharacterId(), member);
        var body = ApiResult.accepted(JobResponseDto.from(job));
        return ResponseEntity.status(body.httpStatus()).body(body);
    }

    /** Swagger에서 ApiResult<AudioBookResponseDto> 구조를 보여주기 위한 헬퍼 클래스 */
    @Schema(name = "AudioBookResponseApiResult")
    public static class AudioBookResponseApiResult {
        @Schema(description = "요청 성공 여부", example = "true")
        public boolean success = true;

        @Schema(description = "응답 데이터")
        public AudioBookResponseDto data = new AudioBookResponseDto(
                java.util.List.of(new AudioBookResponseDto.Item(
                        1L,
                        "동화 제목",
                        "동화 제목",
                        "테마",
                        "분위기",
                        "https://example.com/thumbnail.jpg",
                        "캐릭터 이름",
                        60.4
                ))
        );

        @Schema(description = "에러 정보(성공 시 null)")
        public ExceptionDto error;
    }
}
