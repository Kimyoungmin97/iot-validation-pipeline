package com.iot.pipeline.iot.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * IoT 센서 데이터 엔티티
 *
 * 설계 의도:
 * - 검증을 통과한 데이터만 DB에 저장 — 파이프라인 통과 = 신뢰할 수 있는 데이터
 * - receivedAt을 @PrePersist로 자동 설정해 클라이언트 조작 불가
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "iot_data", indexes = {
        @Index(name = "idx_device_id", columnList = "device_id"),
        @Index(name = "idx_sensor_type", columnList = "sensor_type"),
        @Index(name = "idx_received_at", columnList = "received_at")
})
public class IotData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "sensor_type", nullable = false, length = 50)
    private String sensorType;

    @Column(name = "sensor_value", nullable = false)
    private Double sensorValue;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Builder
    public IotData(String deviceId, String sensorType, Double sensorValue,
                   String unit, String rawPayload) {
        this.deviceId = deviceId;
        this.sensorType = sensorType;
        this.sensorValue = sensorValue;
        this.unit = unit;
        this.rawPayload = rawPayload;
    }

    @PrePersist
    protected void onCreate() {
        this.receivedAt = LocalDateTime.now();
    }
}
