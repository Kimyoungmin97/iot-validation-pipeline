package com.iot.pipeline.iot.service;

import com.iot.pipeline.iot.dto.IotDataRequest;
import com.iot.pipeline.iot.dto.IotDataResponse;
import com.iot.pipeline.iot.dto.ParsedPayload;
import com.iot.pipeline.iot.entity.IotData;
import com.iot.pipeline.iot.exception.IotValidationException;
import com.iot.pipeline.iot.exception.IotValidationException.ValidationStage;
import com.iot.pipeline.iot.repository.IotDataRepository;
import com.iot.pipeline.iot.validator.PayloadParseValidator;
import com.iot.pipeline.iot.validator.SensorRangeValidator;
import com.iot.pipeline.iot.validator.TopicFormatValidator;
import com.iot.pipeline.iot.validator.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IoT 데이터 검증 파이프라인 (3단계)
 *
 * 설계 의도:
 * - 파이프라인 패턴: 각 단계를 독립 검증기에 위임하고, 이 클래스는 단계 조율(orchestration)만 담당
 * - 단계별 실패 시 즉시 예외를 던져 불필요한 후속 처리를 방지 (fail-fast)
 * - 검증 통과 데이터만 DB에 저장해 데이터 품질 보장
 *
 * 파이프라인 흐름:
 * [1단계 Topic 검증] → [2단계 Payload 파싱] → [3단계 범위 검사] → [DB 저장]
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IotValidationPipeline {

    private final TopicFormatValidator topicFormatValidator;
    private final PayloadParseValidator payloadParseValidator;
    private final SensorRangeValidator sensorRangeValidator;
    private final IotDataRepository iotDataRepository;

    /**
     * IoT 데이터를 3단계 파이프라인으로 검증 후 저장
     *
     * @param request 수신된 IoT 데이터 (topic + payload)
     * @return 저장된 데이터 응답 DTO
     * @throws IotValidationException 검증 실패 시 발생 (stage 정보 포함)
     */
    @Transactional
    public IotDataResponse process(IotDataRequest request) {
        log.debug("[Pipeline] 처리 시작 — topic={}", request.getTopic());

        // ─── 1단계: Topic 규격 검증 ────────────────────────────────
        ValidationResult topicResult = topicFormatValidator.validate(request.getTopic());
        if (topicResult.isInvalid()) {
            log.warn("[Pipeline][1단계] Topic 검증 실패: {}", topicResult.getMessage());
            throw new IotValidationException(ValidationStage.TOPIC_FORMAT, topicResult.getMessage());
        }

        String deviceId = topicFormatValidator.extractDeviceId(request.getTopic());
        String sensorType = topicFormatValidator.extractSensorType(request.getTopic());
        log.debug("[Pipeline][1단계] 통과 — deviceId={}, sensorType={}", deviceId, sensorType);

        // ─── 2단계: Payload 파싱 및 필드 검증 ─────────────────────
        ValidationResult payloadResult = payloadParseValidator.validate(request.getPayload());
        if (payloadResult.isInvalid()) {
            log.warn("[Pipeline][2단계] Payload 검증 실패: {}", payloadResult.getMessage());
            throw new IotValidationException(ValidationStage.PAYLOAD_PARSE, payloadResult.getMessage());
        }

        ParsedPayload parsed = payloadParseValidator.parse(request.getPayload());
        log.debug("[Pipeline][2단계] 통과 — value={}, unit={}", parsed.getValue(), parsed.getUnit());

        // ─── 3단계: 센서 값 범위 검사 ──────────────────────────────
        ValidationResult rangeResult = sensorRangeValidator.validate(sensorType, parsed.getValue());
        if (rangeResult.isInvalid()) {
            log.warn("[Pipeline][3단계] 범위 검사 실패: {}", rangeResult.getMessage());
            throw new IotValidationException(ValidationStage.RANGE_CHECK, rangeResult.getMessage());
        }
        log.debug("[Pipeline][3단계] 통과 — sensorType={}, value={}", sensorType, parsed.getValue());

        // ─── DB 저장 ────────────────────────────────────────────────
        IotData saved = iotDataRepository.save(
                IotData.builder()
                        .deviceId(deviceId)
                        .sensorType(sensorType)
                        .sensorValue(parsed.getValue())
                        .unit(parsed.getUnit())
                        .rawPayload(request.getPayload())
                        .build()
        );

        log.info("[Pipeline] 처리 완료 — id={}, deviceId={}, sensorType={}, value={}",
                saved.getId(), deviceId, sensorType, parsed.getValue());

        return toResponse(saved);
    }

    private IotDataResponse toResponse(IotData data) {
        return IotDataResponse.builder()
                .id(data.getId())
                .deviceId(data.getDeviceId())
                .sensorType(data.getSensorType())
                .sensorValue(data.getSensorValue())
                .unit(data.getUnit())
                .receivedAt(data.getReceivedAt())
                .build();
    }
}
