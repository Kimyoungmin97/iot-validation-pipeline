package com.iot.pipeline.iot.validator;

/**
 * IoT 데이터 검증기 인터페이스 (전략 패턴)
 *
 * 설계 의도:
 * - 전략 패턴(Strategy Pattern) 적용: 각 검증 단계를 독립적인 전략으로 구현
 * - 신규 검증 로직 추가 시 이 인터페이스를 구현하는 클래스만 추가하면 됨
 * - IotValidationPipeline이 List<IotValidator>를 순서대로 실행하는 구조
 * - OCP(개방-폐쇄 원칙): 기존 코드 수정 없이 새 검증기 추가 가능
 *
 * 면접 포인트:
 * "검증 로직을 왜 인터페이스로 추상화했나요?"
 * → 파이프라인의 각 단계를 독립 모듈로 분리하면 단위 테스트가 쉽고,
 *   장비 유형이 늘어날 때 해당 장비 전용 검증기를 추가하기만 하면 됩니다.
 *
 * @param <T> 검증 대상 타입 (topic 검증: String, payload 검증: Map, 범위 검증: ParsedPayload 등)
 */
public interface IotValidator<T> {

    /**
     * 검증 수행
     *
     * @param target 검증 대상
     * @return 검증 결과 (성공/실패 + 실패 메시지)
     */
    ValidationResult validate(T target);
}
