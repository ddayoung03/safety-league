# 세이프티 리그 앱 개발 일지

이 문서는 `safety-league/docs/app-dev-handoff.md`의 작업 방식에 따라 매 작업/일자별로 작성하는 개발 기록이다.

---

## 2026-11-03 (1일차: SDK 환경 분석, AAOS 프로젝트 뼈대 구축, 코어 로직 단위 테스트 완료)

### 1. 한 일

#### [작업 1] Pleos Connect SDK 및 에뮬레이터 환경 전수 검증
공식 Pleos 개발자 포털(`document.pleos.ai`) 및 Nexus 리포지토리(`nexus-playground.pleos.ai`) 실물 검증을 완료함.

1. **저장소 및 인증 구조 확인**
   - Maven 저장소: `https://nexus-playground.pleos.ai/repository/maven-releases/`
   - 인증 키/토큰 없이 공개(Public) 접근 가능 확인 완료 (HTTP 200).
   - SDK 주요 라이브러리 및 최신 버전:
     - `ai.pleos.playground:Vehicle:2.3.2` (AAR 실물 및 내부 `classes.jar` API 인터페이스 전수 검증)
     - `ai.pleos.playground:NaviHelper:3.0.5`
     - `ai.pleos.playground:FusedLocation:1.1.3`

2. **차량 신호별 API 매핑 및 기획서 대조 검증**
   기획서의 5대 배점 영역과 결과 카드 트리거에 필요한 모든 신호가 Pleos SDK 및 AAOS 표준 API로 1:1 지원됨을 확인.

   | 항목 | 기획서 표기 | 실제 적용 API 및 인터페이스 | 권한 (Permissions) | 검증 결과 |
   |---|---|---|---|---|
   | **속도** (급가속·급감속) | PERF_VEHICLE_SPEED | AAOS 표준 `CarPropertyManager.getProperty/registerCallback(Float, PERF_VEHICLE_SPEED)` | `android.car.permission.CAR_SPEED` | **일치** (공식 문서에서도 클러스터 속도와 동일성 유지를 위해 AAOS 표준 사용 명시) |
   | **브레이크** (급제동) | Vehicle State Brake | `ai.pleos.playground.vehicle.api.Brake`<br>- `getCurrentBrakePedalStatus()`<br>- `registerBrakePedalStatus(BrakePedalStatusListener)` | `pleos.car.permission.CAR_DRIVE` | **일치** |
   | **조향각** (깜빡이 없는 차선 변경) | Steering | `ai.pleos.playground.vehicle.api.SteeringWheel`<br>- `getCurrentSteeringWheelAngle()`<br>- `registerSteeringWheelAngle(SteeringWheelAngleListener)` | `pleos.car.permission.CAR_DRIVE` | **일치** |
   | **방향지시등** | TurnSignal | `ai.pleos.playground.vehicle.api.TurnSignal`<br>- `checkTurnSignalStateCapabilities()`<br>- `registerTurnSignalState(TurnSignalStateListener)` | `pleos.car.permission.CAR_LIGHT` | **일치** |
   | **안전벨트** | Seat | `ai.pleos.playground.vehicle.api.Safety`<br>- `checkSeatBuckledCapabilities()`<br>- `registerSeatBeltBuckledState(SeatBeltBuckledStateListener)`<br>- `registerSeatBeltNotBuckledWarning(SeatBeltNotBuckledWarningListener)` | `pleos.car.permission.CAR_SAFETY` | **일치** (`Safety` 도메인 산하에 존재) |
   | **경고등** | Safety | `ai.pleos.playground.vehicle.api.Safety`<br>- `getTelltalesInfo()`<br>- `registerTelltales(TelltaleInfoListener)` | `pleos.car.permission.CAR_SAFETY` | **일치** |
   | **도어 개폐** | Door | `ai.pleos.playground.vehicle.api.Door`<br>- `getCurrentDoorPosition()`<br>- `registerDoorPosition(DoorPositionListener)` | `pleos.car.permission.CAR_DOORS` | **일치** |
   | **주행거리** | Odometer | `ai.pleos.playground.vehicle.api.Odometer`<br>- `registerTripDrivingDistance(TripDrivingDistanceListener)` | `pleos.car.permission.CAR_DRIVE` | **일치** |
   | **전비 / 연비** | EvBattery / Fuel | `ai.pleos.playground.vehicle.api.EvBattery` (`TripEvEfficiencyListener`)<br>`ai.pleos.playground.vehicle.api.Fuel` (`TripFuelEconomyListener`) | `pleos.car.permission.CAR_ENERGY` / `CAR_DRIVE` | **일치** (EV/내연기관 분리 대응 가능) |
   | **기어 (P단 감지)** | GEAR_SELECTION = P | `ai.pleos.playground.vehicle.api.Gear`<br>- `registerGearSelection(GearSelectionListener)` (`VehicleGear.PARK`)<br>및 AAOS `VehiclePropertyIds.GEAR_SELECTION` fallback | `pleos.car.permission.CAR_DRIVE` | **일치** (주차 시 결과 카드 자동 팝업 트리거로 사용) |
   | **위치 기록** (리플레이) | NaviHelper 또는 AAOS 위치 | `ai.pleos.playground.FusedLocation`<br>- `registerFusedLocationCallback(FusedLocationListener)`<br>fallback: `android.location.LocationManager` | `pleos.car.permission.FUSED_LOCATION` | **일치** (`FusedLocation SDK` 1순위 사용) |
   | **내비 목적지/남은거리** | NaviHelper | `ai.pleos.playground.NaviHelper`<br>- `registerNaviHelperEventListener` | - | **일치** (주행 중 프로그레스 트랙 거리에 연동) |

3. **에뮬레이터 및 AVD 실행 환경**
   - 시스템 이미지 XML: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml` (Windows x86_64, API 34 기반 Pleos Connect v2.1.0 APIs).
   - 필수 부팅 옵션: **Cold boot** 필수 (Quick boot 시 VHAL 연동 불능).
   - Virtual Console: 차량 속도, 도어, 조향각, 기어 등을 GUI에서 직접 조작하여 테스트 가능.

#### [작업 2] AAOS 앱 프로젝트 뼈대 구축
- **빌드 환경**: Gradle 9.1.0 (Wrapper), Android Gradle Plugin 9.0.1, Kotlin 2.2.0, JDK 17.
- **SDK 설정**: compileSdk = 34, minSdk = 28 (Pleos 보안 기준 충족), targetSdk = 34.
- **저장소 연결**: Google, MavenCentral, Pleos Nexus(`nexus-playground.pleos.ai`) 연동.
- **Pleos 의존성 주입**:
  - `ai.pleos.playground:Vehicle:2.3.2`
  - `ai.pleos.playground:NaviHelper:3.0.5`
  - `ai.pleos.playground:FusedLocation:1.1.3`
- **매니페스트 및 리소스**:
  - `android.hardware.type.automotive` 태그 및 `automotive_app_desc.xml` 선언.
  - 차량 신호 권한 10종(CAR_SPEED, CAR_DRIVE, CAR_LIGHT, CAR_SAFETY, CAR_DOORS, CAR_ENERGY, FUSED_LOCATION 등) 등록.
  - `mockups/tokens.css`의 디자인 토큰 색상(아스팔트 #24272C, 아스팔트짙음 #1E2125, 타워칸 #2E3238, 트랙화이트 #F2F1EC, 회색 #9BA1A8, 차선노랑 #FFC52E, 레이스레드 #C8102E, 골드 #D4A23C)을 `colors.xml` 및 `Theme.SafetyLeague`로 정의.

#### [작업 3] 순수 함수 코어 로직 및 단위 테스트(TDD) 100% 통과
- `mockups/test/demo-data.test.js`의 모든 케이스를 순수 Kotlin 함수 및 JUnit 단위 테스트로 포팅 완료:
  - **시즌 평균**: 11/7 기준 88.5, 11/9 기준 88.8 검증 통과.
  - **주간 평균**: 11.03~11.09 주간 평균 88.8, 이전 주 85.7 (+3.1) 검증 통과.
  - **연속 안전 주행(Streak)**:
    - 70점 이상 운전일 연속 누적 (11/6: 12일, 11/7: 13일, 11/9: 15일).
    - 수요일(11/5) 운전 안 한 날(휴식) 건너뛰기 유지 (11일).
    - 70점 미만 트립 발생 시 즉시 0일 리셋 (10/24: 0일).
    - 같은 날 여러 트립 중 하나라도 70점 미만이면 당일 리셋 (0일).
  - **리그 승급/강등선 및 잔여일**:
    - 상위 30% 승급 라인: 397명 기준 P119.
    - 하위 30% 강등 라인: 397명 기준 P279.
    - 11/9 기준 잔여일 21일.
  - **순위표(타워) 정렬 및 내 점수 반영**:
    - `DemoData.neighbors`에서 '나'의 점수가 해당 시점의 시즌 평균(88.5 / 88.8)으로 정확히 주입되고 정렬됨을 검증.
  - **경로 보간(pointAt)**:
    - 3개 점 polyline 상에서 t=0.0, 0.5, 0.75, 1.0 거리 비례 보간 정확도 검증.
  - **아쉬운 날(Low Day) 리플레이 이벤트 정합성**:
    - 깜빡이 없는 차선 변경 4회, 사건 시간순 정렬 및 시각/t 유효성 검증.
  - **ScoreCalculator (5대 배점 및 공정성 규칙)**:
    - 3km 미만 트립 기록/점수 제외 (0점 처리) 규칙 검증.
    - 부드러움(30) + 매너(25) + 에코(20) + 안전(15) + 꾸준함(10) 만점(100점) 구조 검증.
- `.\gradlew.bat testDebugUnitTest` 실행 결과: **9개 테스트 중 9개 전체 통과 (0 failures, 0 errors)**.

---

### 2. 기획서와 차이점 및 직접 정한 것 (이유 포함)

1. **차량 속도(Speed) 수집 경로**:
   - 기획서에는 `Vehicle State`의 하나로 묶여 기술되어 있었으나, 실제 Pleos Connect 플랫폼에서는 클러스터 계기판 속도와의 정합성을 보장하기 위해 구글 AAOS 표준 `CarPropertyManager.PERF_VEHICLE_SPEED` (`android.car.permission.CAR_SPEED`)를 직접 읽는 것이 공식 권장 규격임. 이를 아키텍처에 반영함.
2. **위치 좌표 수집**:
   - 기획서에 "NaviHelper 또는 AAOS 위치"로 열려 있었으나, 현대차 공식 가이드에 따라 `ai.pleos.playground:FusedLocation`을 1순위로 채택하고, 지원되지 않는 일반 에뮬레이터 환경에서는 표준 `LocationManager`로 부드럽게 fallback 되도록 데이터 소스 인터페이스를 추상화하기로 결정함.
3. **SDK 미설치/일반 안드로이드 환경 격리**:
   - 로컬 단위 테스트(JUnit)와 Mock 환경에서도 의존성 충돌 없이 비즈니스 로직을 온전히 테스트할 수 있도록, 점수 계산·연속 기록·리그 산정 로직을 안드로이드/SDK 프레임워크와 완전히 분리된 순수 Kotlin 함수(`Pure Kotlin`)로 구성함.
4. **한글 경로(`현대차앱개발공모전`) 빌드 설정**:
   - Windows 환경에서 프로젝트 상위 경로에 한글이 포함되어 AGP path validation 에러가 발생함. `gradle.properties`에 `android.overridePathCheck=true`를 추가하여 정상 빌드되도록 조치함.
5. **AGP 9.0 플러그인 설정**:
   - 최신 AGP 9.0은 Kotlin Android 플러그인이 내장되어 있으므로 `org.jetbrains.kotlin.android` 중복 적용 충돌을 방지하기 위해 단일 `com.android.application` 적용으로 최적화함.

---

### 3. 막힌 점
- Gradle 데몬 구동 시 Windows 방화벽 및 포크로 인한 대기 현상이 발생하였으나, `org.gradle.daemon=false` 및 `--no-daemon` 플래그 설정을 통해 안정적인 빌드/테스트 파이프라인을 확보함.

---

### 4. 확인 못 한 것
- 오프라인 대회장(12/3~4)에서 제공되는 실제 Virtual Console과 에뮬레이터 간의 네트워크 포트/IPC 연동 세부 파라미터. (단, 에뮬레이터 내부 VHAL 표준 프로퍼티를 따르므로 코드 레벨에서는 표준 리스너를 유지하면 정상 수신됨).
