package com.iot.pipeline.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.pipeline.iot.dto.IotDataRequest;
import com.iot.pipeline.iot.repository.IotDataRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * IoT 파이프라인 통합 테스트
 *
 * 전략:
 * - @SpringBootTest: 실제 스프링 컨텍스트 로드 (H2 DB + 임베디드 Redis)
 * - MockMvc: HTTP 계층까지 포함한 엔드-투-엔드 검증
 * - 각 테스트 후 @AfterEach로 DB 초기화해 테스트 간 독립성 보장
 *
 * 면접 포인트:
 * "통합 테스트와 단위 테스트를 왜 분리했나요?"
 * → 단위 테스트는 빠르게(수십 ms) 비즈니스 로직을 검증하고,
 *   통합 테스트는 Spring Context 전체를 올려 HTTP 요청부터 DB 저장까지
 *   실제 흐름이 올바르게 동작하는지 검증합니다.
 *   CI에서 단위 테스트를 먼저, 통합 테스트를 나중에 실행해 피드백 루프를 최적화할 수 있습니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(EmbeddedRedisConfig.class)
@DisplayName("IoT 파이프라인 통합 테스트")
class IotPipelineIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private IotDataRepository iotDataRepository;

    @AfterEach
    void cleanup() {
        iotDataRepository.deleteAll();
    }

    @Test
    @DisplayName("정상 IoT 데이터 POST → 201 Created + DB 저장 확인")
    void valid_iot_data_is_saved() throws Exception {
        IotDataRequest request = IotDataRequest.builder()
                .topic("iot/devices/device-001/temperature")
                .payload("{\"value\": 23.5, \"unit\": \"celsius\", \"timestamp\": 1711234567890}")
                .build();

        mockMvc.perform(post("/api/iot/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deviceId").value("device-001"))
                .andExpect(jsonPath("$.data.sensorType").value("temperature"))
                .andExpect(jsonPath("$.data.sensorValue").value(23.5));

        assertThat(iotDataRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("잘못된 topic → 400 Bad Request (TOPIC_FORMAT 실패)")
    void invalid_topic_returns_400() throws Exception {
        IotDataRequest request = IotDataRequest.builder()
                .topic("wrong/topic/format")
                .payload("{\"value\": 23.5, \"unit\": \"celsius\"}")
                .build();

        mockMvc.perform(post("/api/iot/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(iotDataRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("범위 초과 센서 값 → 400 Bad Request (RANGE_CHECK 실패)")
    void out_of_range_value_returns_400() throws Exception {
        IotDataRequest request = IotDataRequest.builder()
                .topic("iot/devices/device-001/temperature")
                .payload("{\"value\": 9999.0, \"unit\": \"celsius\"}")
                .build();

        mockMvc.perform(post("/api/iot/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("유효 범위")));

        assertThat(iotDataRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("검색어 기록 후 랭킹 조회")
    void search_and_get_ranking() throws Exception {
        // 검색어 기록
        mockMvc.perform(post("/api/ranking/search?keyword=temperature"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/ranking/search?keyword=temperature"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/ranking/search?keyword=humidity"))
                .andExpect(status().isOk());

        // 랭킹 조회
        mockMvc.perform(get("/api/ranking/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rankings[0].keyword").value("temperature"))
                .andExpect(jsonPath("$.data.rankings[0].score").value(2.0));
    }
}
