package pproject.once_upon_a_time.global.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.micrometer.common.lang.Nullable;
import org.springframework.http.HttpStatus;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ExceptionDto;

public record ApiResult<T>(
        @JsonIgnore HttpStatus httpStatus,
        boolean success,
        @Nullable T data,
        @Nullable ExceptionDto error
) {
    public static <T> ApiResult<T> ok(@Nullable final T data) {
        return new ApiResult<>(HttpStatus.OK, true, data, null);
    }

    public static <T> ApiResult<T> created(@Nullable final T data) {
        return new ApiResult<>(HttpStatus.CREATED, true, data, null);
    }

    public static <T> ApiResult<T> fail(final CustomException e, final String path) {
        return new ApiResult<>(
                e.getErrorCode().getHttpStatus(),
                false,
                null,
                ExceptionDto.of(e.getErrorCode(), path, e.getMessage())
        );
    }
}
