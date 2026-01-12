package pproject.once_upon_a_time.infrastructure;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pproject.once_upon_a_time.global.response.ApiResult;
import pproject.once_upon_a_time.global.response.ExceptionDto;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/upload")
@Tag(name = "Upload", description = "S3 스토리지 업로드 API")
public class UploadController {

    private final StorageService storageService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "이미지 업로드", description = "이미지 파일을 S3에 업로드합니다.")
    @ApiResponse(responseCode = "200", description = "업로드 성공",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UploadUrlApiResult.class)))
    public ResponseEntity<ApiResult<UploadUrlResponse>> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        String url = storageService.uploadFile(file, StorageService.FileType.IMAGE);
        var body = ApiResult.ok(new UploadUrlResponse(url));
        return ResponseEntity.status(body.httpStatus()).body(body);
    }

    @PostMapping(value = "/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "오디오 업로드", description = "오디오 파일을 S3에 업로드합니다.")
    @ApiResponse(responseCode = "200", description = "업로드 성공",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UploadUrlApiResult.class)))
    public ResponseEntity<ApiResult<UploadUrlResponse>> uploadAudio(@RequestParam("file") MultipartFile file) throws IOException {
        String url = storageService.uploadFile(file, StorageService.FileType.AUDIO);
        var body = ApiResult.ok(new UploadUrlResponse(url));
        return ResponseEntity.status(body.httpStatus()).body(body);
    }

    @PostMapping(value = "/character", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "캐릭터 오디오 업로드", description = "캐릭터용 오디오 파일을 S3에 업로드합니다.")
    @ApiResponse(responseCode = "200", description = "업로드 성공",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UploadUrlApiResult.class)))
    public ResponseEntity<ApiResult<UploadUrlResponse>> uploadCharacter(@RequestParam("file") MultipartFile file) throws IOException {
        String url = storageService.uploadFile(file, StorageService.FileType.CHARACTER);
        var body = ApiResult.ok(new UploadUrlResponse(url));
        return ResponseEntity.status(body.httpStatus()).body(body);
    }

    /** 업로드된 파일의 공개 URL을 담는 DTO. */
    @Schema(name = "UploadUrlResponse", description = "업로드된 파일의 퍼블릭 URL")
    public record UploadUrlResponse(String url) {}

    /** Swagger에서 ApiResult<UploadUrlResponse> 구조를 보여주기 위한 샘플 스키마. */
    @Schema(name = "UploadUrlApiResult")
    public static class UploadUrlApiResult {
        @Schema(description = "요청 성공 여부", example = "true")
        public boolean success = true;

        @Schema(description = "업로드된 파일 정보")
        public UploadUrlResponse data = new UploadUrlResponse("https://.../path/to/file");

        @Schema(description = "에러 정보(성공 시 null)")
        public ExceptionDto error;
    }
}
