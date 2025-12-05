package pproject.once_upon_a_time.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import pproject.once_upon_a_time.global.exception.CustomException;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // Null 값인 필드는 JSON 응답에 포함하지 않음
public class ApiResponse<T> {

    private final String code;
    private final String message;
    private T data;

    // 성공 시
    private ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 실패 시
    private ApiResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data);
    }
    
    public static ApiResponse<?> fail(CustomException e) {
        return new ApiResponse<>(e.getErrorCode().name(), e.getMessage());
    }
}
