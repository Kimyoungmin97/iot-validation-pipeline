package com.iot.pipeline.iot.validator;

import lombok.Getter;

/**
 * 각 검증 단계의 결과를 캡슐화하는 값 객체(Value Object)
 *
 * 설계 의도:
 * - boolean 반환 대신 결과 객체를 반환해 "왜 실패했는지" 메시지를 함께 전달
 * - 파이프라인 체이닝 시 실패 시점의 컨텍스트를 보존
 */
@Getter
public class ValidationResult {

    private final boolean valid;
    private final String message;

    private ValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public static ValidationResult success() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult failure(String message) {
        return new ValidationResult(false, message);
    }

    public boolean isInvalid() {
        return !valid;
    }
}
