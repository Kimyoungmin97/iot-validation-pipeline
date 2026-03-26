package com.iot.pipeline.iot.repository;

import com.iot.pipeline.iot.entity.IotData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * IoT 데이터 저장소
 *
 * 설계 의도:
 * - Spring Data JPA로 CRUD 자동 구현
 * - 자주 사용되는 조회 패턴(장비별, 기간별)을 메서드 쿼리로 표현
 */
public interface IotDataRepository extends JpaRepository<IotData, Long> {

    List<IotData> findByDeviceIdOrderByReceivedAtDesc(String deviceId);

    List<IotData> findBySensorTypeAndReceivedAtBetween(
            String sensorType, LocalDateTime from, LocalDateTime to);

    @Query("SELECT d FROM IotData d WHERE d.deviceId = :deviceId AND d.sensorType = :sensorType " +
           "ORDER BY d.receivedAt DESC")
    List<IotData> findLatestByDeviceAndSensor(
            @Param("deviceId") String deviceId,
            @Param("sensorType") String sensorType);
}
