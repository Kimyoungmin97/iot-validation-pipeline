package com.iot.pipeline.iot.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * IoT 데이터 처리 결과 응답 DTO
 */
@Getter
@Builder
public class IotDataResponse {

    private Long id;
    private String deviceId;
    private String sensorType;
    private Double sensorValue;
    private String unit;
    private LocalDateTime receivedAt;
}
