package com.iot.pipeline.iot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * IoT 데이터 수신 요청 DTO
 *
 * 설계 의도:
 * - topic: MQTT 표준 형식(iot/devices/{deviceId}/{sensorType})을 따름
 * - payload: 원본 JSON 문자열 그대로 수신 — 파싱은 파이프라인 내부에서 처리
 *   (역할 분리: 컨트롤러는 수신만, 파이프라인은 검증/파싱)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotDataRequest {

    @NotBlank(message = "topic은 필수입니다.")
    private String topic;

    @NotBlank(message = "payload는 필수입니다.")
    private String payload;
}
