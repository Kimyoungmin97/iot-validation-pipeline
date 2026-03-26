package com.iot.pipeline.common.exception;

import com.iot.pipeline.common.response.ApiResponse;
import com.iot.pipeline.iot.exception.IotValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 핸들러
 *
 * 설계 의도:
 * - 비즈니스 예외(IotValidationException)와 시스템 예외를 분리하여 처리
 * - 컨트롤러 계층에서 try-catch를 제거하고 횡단 관심사로 분리
 * - 에러 응답도 ApiResponse로 감싸서 일관성 유지
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * IoT 데이터 검증 실패 예외 처리
     * 400 Bad Request: 클라이언트가 잘못된 데이터를 전송한 경우
     */
    @ExceptionHandler(IotValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleIotValidationException(IotValidationException e) {
        log.warn("[IoT Validation] stage={}, message={}", e.getStage(), e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    /**
     * 예상치 못한 서버 내부 오류 처리
     * 스택 트레이스는 서버 로그에만 남기고, 클라이언트에는 노출 최소화
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[Internal Error] {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("내부 서버 오류가 발생했습니다."));
    }
}
