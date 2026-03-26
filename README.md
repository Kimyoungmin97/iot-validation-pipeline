# IoT 데이터 검증 파이프라인

IoT 센서 데이터를 3단계 파이프라인으로 검증하고, Redis Sorted Set을 활용한 인기 검색어 랭킹 기능을 포함한 Spring Boot 백엔드 프로젝트입니다.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Build | Gradle |
| DB | MySQL 8.0 |
| Cache | Redis 7.2 |
| Test | JUnit5 + Mockito |
| Container | Docker / Docker Compose |

---

## 프로젝트 구조

```
src/main/java/com/iot/pipeline/
├── IotValidationPipelineApplication.java
├── iot/                          # IoT 데이터 검증 도메인
│   ├── controller/               # HTTP 수신
│   ├── service/                  # 파이프라인 조율
│   ├── validator/                # 3단계 검증기 (전략 패턴)
│   │   ├── IotValidator.java     # 검증기 인터페이스
│   │   ├── TopicFormatValidator  # 1단계: Topic 규격 검증
│   │   ├── PayloadParseValidator # 2단계: Payload 파싱/검증
│   │   └── SensorRangeValidator  # 3단계: 센서 범위 검사
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   └── exception/
├── ranking/                      # 인기 검색어 랭킹 도메인
│   ├── controller/
│   ├── service/                  # Redis Sorted Set 랭킹
│   ├── repository/
│   └── entity/
└── common/                       # 공통 모듈
    ├── config/                   # Redis 설정, IoT 프로퍼티
    ├── exception/                # 전역 예외 핸들러
    └── response/                 # 공통 API 응답 형식
```

---

## 핵심 설계 포인트

### 1. IoT 데이터 검증 파이프라인 (전략 + 파이프라인 패턴)

```
[POST /api/iot/data]
       │
       ▼
[1단계] TopicFormatValidator — iot/devices/{deviceId}/{sensorType} 형식 검증
       │ 실패 → IotValidationException(TOPIC_FORMAT)
       ▼
[2단계] PayloadParseValidator — JSON 파싱 + 필수 필드(value, unit) 검증
       │ 실패 → IotValidationException(PAYLOAD_PARSE)
       ▼
[3단계] SensorRangeValidator — 물리적 유효 범위 검사 (application.yml 기반)
       │ 실패 → IotValidationException(RANGE_CHECK)
       ▼
[DB 저장] — 검증 통과 데이터만 저장
```

**확장 방법:** 신규 장비 타입 추가 시 `application.yml`의 `iot.validation.sensors`에 범위만 추가하면 됩니다.

### 2. Redis 캐싱 전략

| 패턴 | 적용 위치 | TTL |
|------|-----------|-----|
| Redis Sorted Set (ZINCRBY) | 인기 검색어 랭킹 | - |
| Cache-Aside (@Cacheable) | 키워드 검색 횟수 | 5분 |
| 캐시 TTL 차등 적용 | ranking: 1시간, device: 10분 | - |
| allkeys-lru 정책 | docker-compose.yml Redis 설정 | - |

---

## 실행 방법

### 1. 사전 요구사항

- Java 17+
- Docker & Docker Compose

### 2. 인프라 실행 (MySQL + Redis)

```bash
docker-compose up -d
```

컨테이너 상태 확인:

```bash
docker-compose ps
```

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

또는 JAR 빌드 후 실행:

```bash
./gradlew build
java -jar build/libs/iot-validation-pipeline-0.0.1-SNAPSHOT.jar
```

서버 기본 포트: `http://localhost:8080`

---

## API 사용 예시

### IoT 데이터 전송

```bash
# 정상 데이터
curl -X POST http://localhost:8080/api/iot/data \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "iot/devices/device-001/temperature",
    "payload": "{\"value\": 23.5, \"unit\": \"celsius\", \"timestamp\": 1711234567890}"
  }'

# 비정상 데이터 (범위 초과) → 400 Bad Request
curl -X POST http://localhost:8080/api/iot/data \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "iot/devices/device-001/temperature",
    "payload": "{\"value\": 9999.0, \"unit\": \"celsius\"}"
  }'
```

### 인기 검색어 랭킹

```bash
# 검색어 기록
curl -X POST "http://localhost:8080/api/ranking/search?keyword=temperature"

# Top 10 조회
curl http://localhost:8080/api/ranking/top

# 특정 키워드 검색 횟수 (Cache-Aside)
curl "http://localhost:8080/api/ranking/count?keyword=temperature"
```

---

## 테스트 실행 방법

### 전체 테스트 실행

```bash
./gradlew test
```

### 테스트 결과 리포트

```bash
open build/reports/tests/test/index.html
```

### 테스트 구성

| 테스트 클래스 | 유형 | 설명 |
|---------------|------|------|
| `TopicFormatValidatorTest` | 단위 | Topic 형식 검증기 (정상/비정상 케이스) |
| `PayloadParseValidatorTest` | 단위 | Payload 파싱 검증기 |
| `SensorRangeValidatorTest` | 단위 | 센서 범위 검사 (파라미터 테스트) |
| `IotValidationPipelineTest` | 단위 | 파이프라인 조율 로직 (Mockito) |
| `RankingServiceTest` | 단위 | Redis 랭킹 서비스 (Redis Mockito) |
| `IotPipelineIntegrationTest` | 통합 | HTTP → DB까지 전체 흐름 (H2 + 임베디드 Redis) |

---

## 면접 예상 질문 & 답변

**Q. 검증 로직을 왜 인터페이스(IotValidator)로 추상화했나요?**
A. 전략 패턴을 적용해 각 검증 단계를 독립 모듈로 분리했습니다. 단위 테스트가 쉬워지고, 신규 장비 추가 시 새 검증기 클래스만 추가하면 파이프라인 코드를 수정하지 않아도 됩니다(OCP).

**Q. Redis Sorted Set을 왜 사용했나요?**
A. Hash로 저장 후 앱에서 정렬하면 모든 키워드를 메모리에 올려야 합니다. Sorted Set은 Redis 내부에서 정렬 상태를 유지하므로 ZREVRANGE로 상위 N개만 O(log N + K)로 조회 가능합니다.

**Q. allkeys-lru 정책을 선택한 이유는?**
A. 랭킹/캐시 용도이므로 메모리가 가득 찼을 때 가장 오래 사용되지 않은 항목을 자동 제거하는 LRU가 적합합니다. `volatile-lru`는 TTL이 설정된 키만 대상으로 하는데, Sorted Set 키는 TTL이 없어서 `allkeys-lru`를 선택했습니다.

**Q. 통합 테스트에서 임베디드 Redis를 쓴 이유는?**
A. 실제 Redis에 의존하면 CI 환경마다 Redis 설치가 필요하고 테스트 격리가 어렵습니다. 임베디드 Redis를 사용하면 테스트 시작/종료와 함께 자동으로 실행/종료되어 독립적인 테스트 환경을 보장합니다.
