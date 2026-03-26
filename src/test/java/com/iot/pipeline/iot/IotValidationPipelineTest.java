package com.iot.pipeline.iot;

import com.iot.pipeline.iot.dto.IotDataRequest;
import com.iot.pipeline.iot.dto.IotDataResponse;
import com.iot.pipeline.iot.entity.IotData;
import com.iot.pipeline.iot.exception.IotValidationException;
import com.iot.pipeline.iot.exception.IotValidationException.ValidationStage;
import com.iot.pipeline.iot.repository.IotDataRepository;
import com.iot.pipeline.iot.service.IotValidationPipeline;
import com.iot.pipeline.iot.validator.PayloadParseValidator;
import com.iot.pipeline.iot.validator.SensorRangeValidator;
import com.iot.pipeline.iot.validator.TopicFormatValidator;
import com.iot.pipeline.iot.validator.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * IotValidationPipeline 단위 테스트 (Mockito)
 *
 * 전략:
 * - 각 검증기를 Mockito로 모킹해 파이프라인 조율 로직만 테스트
 * - "어느 단계에서 실패했을 때 어느 예외가 던져지는가"를 중점적으로 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IotValidationPipeline 단위 테스트")
class IotValidationPipelineTest {

    @Mock private TopicFormatValidator topicFormatValidator;
    @Mock private PayloadParseValidator payloadParseValidator;
    @Mock private SensorRangeValidator sensorRangeValidator;
    @Mock private IotDataRepository iotDataRepository;

    @InjectMocks
    private IotValidationPipeline pipeline;

    private static final String VALID_TOPIC = "iot/devices/device-001/temperature";
    private static final String VALID_PAYLOAD = """
            {"value": 23.5, "unit": "celsius", "timestamp": 1711234567890}
            """;

    @Test
    @DisplayName("정상 데이터는 저장 후 응답 반환")
    void valid_data_is_saved_and_returned() {
        // given
        given(topicFormatValidator.validate(VALID_TOPIC)).willReturn(ValidationResult.success());
        given(topicFormatValidator.extractDeviceId(VALID_TOPIC)).willReturn("device-001");
        given(topicFormatValidator.extractSensorType(VALID_TOPIC)).willReturn("temperature");
        given(payloadParseValidator.validate(VALID_PAYLOAD)).willReturn(ValidationResult.success());
        given(payloadParseValidator.parse(VALID_PAYLOAD)).willReturn(
                com.iot.pipeline.iot.dto.ParsedPayload.builder()
                        .value(23.5).unit("celsius").timestamp(1711234567890L).build());
        given(sensorRangeValidator.validate("temperature", 23.5)).willReturn(ValidationResult.success());

        IotData saved = IotData.builder()
                .deviceId("device-001").sensorType("temperature")
                .sensorValue(23.5).unit("celsius").rawPayload(VALID_PAYLOAD).build();
        // receivedAt을 리플렉션으로 설정하지 않고 null 허용 (테스트 목적)
        given(iotDataRepository.save(any())).willReturn(saved);

        // when
        IotDataResponse response = pipeline.process(
                IotDataRequest.builder().topic(VALID_TOPIC).payload(VALID_PAYLOAD).build());

        // then
        assertThat(response.getDeviceId()).isEqualTo("device-001");
        assertThat(response.getSensorValue()).isEqualTo(23.5);
        verify(iotDataRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("1단계(Topic) 검증 실패 시 TOPIC_FORMAT 예외 발생 + 이후 단계 실행 안 됨")
    void topic_validation_failure_throws_correct_stage_exception() {
        // given
        given(topicFormatValidator.validate(anyString()))
                .willReturn(ValidationResult.failure("잘못된 topic"));

        // when & then
        assertThatThrownBy(() -> pipeline.process(
                IotDataRequest.builder().topic("bad-topic").payload(VALID_PAYLOAD).build()))
                .isInstanceOf(IotValidationException.class)
                .satisfies(ex -> {
                    IotValidationException e = (IotValidationException) ex;
                    assertThat(e.getStage()).isEqualTo(ValidationStage.TOPIC_FORMAT);
                });

        // payload, range 검증은 호출되지 않아야 함 (fail-fast)
        verify(payloadParseValidator, never()).validate(any());
        verify(sensorRangeValidator, never()).validate(any(), anyDouble());
    }

    @Test
    @DisplayName("2단계(Payload) 검증 실패 시 PAYLOAD_PARSE 예외 발생")
    void payload_validation_failure_throws_correct_stage_exception() {
        // given
        given(topicFormatValidator.validate(VALID_TOPIC)).willReturn(ValidationResult.success());
        given(topicFormatValidator.extractDeviceId(VALID_TOPIC)).willReturn("device-001");
        given(topicFormatValidator.extractSensorType(VALID_TOPIC)).willReturn("temperature");
        given(payloadParseValidator.validate(anyString()))
                .willReturn(ValidationResult.failure("value 없음"));

        // when & then
        assertThatThrownBy(() -> pipeline.process(
                IotDataRequest.builder().topic(VALID_TOPIC).payload("{}").build()))
                .isInstanceOf(IotValidationException.class)
                .satisfies(ex -> {
                    IotValidationException e = (IotValidationException) ex;
                    assertThat(e.getStage()).isEqualTo(ValidationStage.PAYLOAD_PARSE);
                });

        verify(sensorRangeValidator, never()).validate(any(), anyDouble());
    }

    @Test
    @DisplayName("3단계(범위) 검증 실패 시 RANGE_CHECK 예외 발생")
    void range_validation_failure_throws_correct_stage_exception() {
        // given
        given(topicFormatValidator.validate(VALID_TOPIC)).willReturn(ValidationResult.success());
        given(topicFormatValidator.extractDeviceId(VALID_TOPIC)).willReturn("device-001");
        given(topicFormatValidator.extractSensorType(VALID_TOPIC)).willReturn("temperature");
        given(payloadParseValidator.validate(VALID_PAYLOAD)).willReturn(ValidationResult.success());
        given(payloadParseValidator.parse(VALID_PAYLOAD)).willReturn(
                com.iot.pipeline.iot.dto.ParsedPayload.builder()
                        .value(9999.0).unit("celsius").timestamp(0L).build());
        given(sensorRangeValidator.validate("temperature", 9999.0))
                .willReturn(ValidationResult.failure("범위 초과"));

        // when & then
        assertThatThrownBy(() -> pipeline.process(
                IotDataRequest.builder().topic(VALID_TOPIC).payload(VALID_PAYLOAD).build()))
                .isInstanceOf(IotValidationException.class)
                .satisfies(ex -> {
                    IotValidationException e = (IotValidationException) ex;
                    assertThat(e.getStage()).isEqualTo(ValidationStage.RANGE_CHECK);
                });

        verify(iotDataRepository, never()).save(any());
    }
}
