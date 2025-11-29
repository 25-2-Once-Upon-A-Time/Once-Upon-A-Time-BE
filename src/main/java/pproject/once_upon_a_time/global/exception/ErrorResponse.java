package pproject.once_upon_a_time.global.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Builder
public class ErrorResponse {

    private final boolean success; // 항상 false
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
    private final String path;
    private final String timestamp;
    private final Map<String, String> details; // ✔ Validation 오류 담는 필드

    // 기본 오류
    public static ErrorResponse of(ErrorCode code, String path) {
        return ErrorResponse.builder()
                .success(false)
                .code(code.name())
                .message(code.getMessage())
                .httpStatus(code.getHttpStatus())
                .path(path)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    // Validation 오류 전용
    public static ErrorResponse of(ErrorCode code, String path, MethodArgumentNotValidException e) {

        Map<String, String> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (msg1, msg2) -> msg1
                ));

        return ErrorResponse.builder()
                .success(false)
                .code(code.name())
                .message(code.getMessage())
                .httpStatus(code.getHttpStatus())
                .path(path)
                .timestamp(LocalDateTime.now().toString())
                .details(fieldErrors) // ✔ 필드 에러 담기
                .build();
    }
}
