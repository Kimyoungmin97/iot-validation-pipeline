package com.iot.pipeline.iot.controller;

import com.iot.pipeline.common.response.ApiResponse;
import com.iot.pipeline.iot.dto.IotDataRequest;
import com.iot.pipeline.iot.dto.IotDataResponse;
import com.iot.pipeline.iot.service.IotValidationPipeline;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * IoT 데이터 수신 컨트롤러
 *
 * 설계 의도:
 * - 컨트롤러는 HTTP 요청/응답 처리에만 집중, 비즈니스 로직은 IotValidationPipeline에 위임
 * - @Valid: DTO 수준의 기본 검증(NotBlank 등)을 컨트롤러 진입 전에 처리
 */
@RestController
@RequestMapping("/api/iot")
@RequiredArgsConstructor
public class IotDataController {

    private final IotValidationPipeline pipeline;

    /**
     * IoT 디바이스에서 전송된 센서 데이터를 수신해 파이프라인으로 처리
     * POST /api/iot/data
     */
    @PostMapping("/data")
    public ResponseEntity<ApiResponse<IotDataResponse>> receiveData(
            @Valid @RequestBody IotDataRequest request) {
        IotDataResponse response = pipeline.process(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("데이터가 성공적으로 저장되었습니다.", response));
    }
}
