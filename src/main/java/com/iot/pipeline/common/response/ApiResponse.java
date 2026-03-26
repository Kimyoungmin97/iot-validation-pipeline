package com.iot.pipeline.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 공통 API 응답 래퍼
 *
 * 설계 의도:
 * - 모든 API 응답을 일관된 형식(success, message, data)으로 표준화
 * - 클라이언트가 응답 구조를 예측 가능하게 해서 파싱 로직을 단순화
 * - @JsonInclude(NON_NULL): data가 null인 경우(예: 에러 응답) 직렬화에서 제외해 페이로드 간소화
 *
 * @param <T> 응답 데이터 타입
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "success", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
