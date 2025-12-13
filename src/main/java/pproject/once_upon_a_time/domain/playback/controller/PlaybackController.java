package pproject.once_upon_a_time.domain.playback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pproject.once_upon_a_time.auth.annotation.LoginUser;
import pproject.once_upon_a_time.domain.audiobook.dto.PlaybackFinishRequest;
import pproject.once_upon_a_time.domain.audiobook.dto.PlaybackInfoResponse;
import pproject.once_upon_a_time.domain.audiobook.dto.PlaybackProgressRequest;
import pproject.once_upon_a_time.domain.audiobook.dto.PlaybackStartResponse;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.playback.service.PlaybackService;
import pproject.once_upon_a_time.global.response.ApiResult;

@RestController
@RequestMapping("/api/v1/audiobooks/{audiobookId}/playback")
@RequiredArgsConstructor
@Tag(name = "Playback", description = "오디오북 재생 API")
public class PlaybackController {

    private final PlaybackService playbackService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "재생 정보 조회",
            description = "DB 재생 정보를 조회하고 Redis 값이 있으면 우선 적용해 반환합니다. 동화/캐릭터 메타정보(제목, 테마, 분위기, 썸네일, 주인공 이름) 포함.",
            security = @SecurityRequirement(name = "JWT TOKEN")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PlaybackInfoResponse.class))),
            @ApiResponse(responseCode = "404", description = "재생 정보 없음 / 오디오북 또는 관련 동화/캐릭터 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResult<PlaybackInfoResponse>> getPlayback(
            @PathVariable Long audiobookId,
            @Parameter(hidden = true) @LoginUser Member member) {
        PlaybackInfoResponse response = playbackService.getInfo(audiobookId, member);
        ApiResult<PlaybackInfoResponse> body = ApiResult.ok(response);
        return ResponseEntity.status(body.httpStatus()).body(body);
    }

    @PostMapping(value = "/start", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "재생 시작",
            description = "재생 정보를 조회/생성 후 Redis에 초기화하고 재생 정보를 반환합니다.",
            security = @SecurityRequirement(name = "JWT TOKEN")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PlaybackStartResponse.class))),
            @ApiResponse(responseCode = "404", description = "오디오북 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResult<PlaybackStartResponse>> start(
            @PathVariable Long audiobookId,
            @Parameter(hidden = true) @LoginUser Member member) {
        PlaybackStartResponse response = playbackService.start(audiobookId, member);
        ApiResult<PlaybackStartResponse> body = ApiResult.ok(response);
        return ResponseEntity.status(body.httpStatus()).body(body);
    }

    @PatchMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "재생 진행도 업데이트",
            description = "현재 재생 위치/상태를 Redis에 저장합니다.",
            security = @SecurityRequirement(name = "JWT TOKEN")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 진행도"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResult<Void>> updateProgress(
            @PathVariable Long audiobookId,
            @RequestBody PlaybackProgressRequest request,
            @Parameter(hidden = true) @LoginUser Member member) {
        playbackService.updateProgress(audiobookId, member, request);
        ApiResult<Void> body = ApiResult.ok(null);
        return ResponseEntity.status(body.httpStatus()).body(body);
    }

    @PostMapping(value = "/finish", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "재생 완료",
            description = "Redis에 저장된 최신 진행도를 반영해 MySQL에 완료 상태로 저장합니다.",
            security = @SecurityRequirement(name = "JWT TOKEN")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "재생 정보 없음/잘못된 진행도"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResult<Void>> finish(
            @PathVariable Long audiobookId,
            @RequestBody(required = false) PlaybackFinishRequest request,
            @Parameter(hidden = true) @LoginUser Member member) {
        playbackService.finish(audiobookId, member, request);
        ApiResult<Void> body = ApiResult.ok(null);
        return ResponseEntity.status(body.httpStatus()).body(body);
    }
}
