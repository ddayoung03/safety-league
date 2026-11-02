# 세이프티 리그 앱 개발 일지

이 문서는 `safety-league/docs/app-dev-handoff.md`의 작업 방식에 따라 매 작업/일자별로 작성하는 개발 기록이다.

---

## 2026-11-03 (1일차: SDK/환경 분석 및 코어 로직 단위 테스트 구현)

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

---

### 2. 기획서와 차이점 및 직접 정한 것 (이유 포함)

1. **차량 속도(Speed) 수집 경로**:
   - 기획서에는 `Vehicle State`의 하나로 묶여 기술되어 있었으나, 실제 Pleos Connect 플랫폼에서는 클러스터 계기판 속도와의 정합성을 보장하기 위해 구글 AAOS 표준 `CarPropertyManager.PERF_VEHICLE_SPEED` (`android.car.permission.CAR_SPEED`)를 직접 읽는 것이 공식 권장 규격임. 이를 아키텍처에 반영함.
2. **위치 좌표 수집**:
   - 기획서에 "NaviHelper 또는 AAOS 위치"로 열려 있었으나, 현대차 공식 가이드에 따라 `ai.pleos.playground:FusedLocation`을 1순위로 채택하고, 지원되지 않는 일반 에뮬레이터 환경에서는 표준 `LocationManager`로 부드럽게 fallback 되도록 데이터 소스 인터페이스를 추상화하기로 결정함.
3. **SDK 미설치/일반 안드로이드 환경 격리**:
   - 로컬 단위 테스트(JUnit)와 Mock 환경에서도 의존성 충돌 없이 비즈니스 로직을 온전히 테스트할 수 있도록, 점수 계산·연속 기록·리그 산정 로직을 안드로이드/SDK 프레임워크와 완전히 분리된 순수 Kotlin 함수(`Pure Kotlin`)로 구성하기로 함.

---

### 3. 막힌 점
- 현재 개발 머신에 Android SDK는 존재하나 Pleos AVD 이미지는 아직 설치되지 않은 상태임.
- 실제 AVD 다운로드는 대용량(수 GB)이므로, 우선 앱 프로젝트 구조 구축, 순수 함수 및 단위 테스트(TDD), 모의(Mock) 신호 수집 엔진을 완벽하게 구축한 뒤 에뮬레이터 검증을 진행하도록 단계를 분리함.

---

### 4. 확인 못 한 것
- 오프라인 대회장(12/3~4)에서 제공되는 실제 Virtual Console과 에뮬레이터 간의 네트워크 포트/IPC 연동 세부 파라미터. (단, 에뮬레이터 내부 VHAL 표준 프로퍼티를 따르므로 코드 레벨에서는 표준 리스너를 유지하면 정상 수신됨).
