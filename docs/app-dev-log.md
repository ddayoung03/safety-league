# 세이프티 리그 앱 개발 일지

이 문서는 `safety-league/docs/app-dev-handoff.md`의 작업 방식에 따라 매 작업/일자별로 작성하는 개발 기록이다.

---

## 2026-11-03 (1일차: SDK 환경 분석, AAOS 뼈대, 순수 함수 TDD, 신호 수집 엔진, 5대 화면 UI 및 특수 모션 구현 완료)

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

#### [작업 4] 차량 신호 수집 엔진 및 백그라운드 트립 추적 서비스 구현
1. **차량 신호 추상화 및 수집 엔진 (`TripCollector`)**:
   - `VehicleSignalListener` 및 `VehicleDataSource` 인터페이스 정의.
   - **기어 상태 머신**: `PARK → DRIVE` 시 트립 시작, 출발 전 안전벨트 스냅샷 검사, `DRIVE → PARK` 시 자동 트립 완료 및 5대 배점 점수 계산.
   - **급가속/급제동 알고리즘**: $dt$ 기반 가속도($dv/dt$) 계산으로 급가속($> +3.0\text{ m/s}^2$) 및 브레이크 연동 급제동($< -3.5\text{ m/s}^2$) 이벤트 실시간 검출.
   - **깜빡이 없는 차선 변경 감지**: $30\text{ km/h}$ 이상 속도에서 조향각($\ge 15^\circ$) 변화 구간 동안 방향지시등 점등 여부를 추적하여 미점등 차선 변경 검출 및 시계열 리플레이 이벤트 생성.
   - **위치 기록**: FusedLocation 경로 점($x, y$) 누적 및 사건 발생 시 상대적 거리 비율 $t(0.0..1.0)$ 매핑.
2. **트립 수집기 단위 테스트 (`TripCollectorTest`)**:
   - 트립 생명주기, 급가속/급제동 검출, 깜빡이 없는 차선 변경, 출발 전 안전벨트 미착용 감점, 3km 미만 트립 제외 판정, 아쉬운 날 시나리오 재현(사건 7건) 등 6개 단위 테스트 구현 및 **100% 통과**. (누적 15개 단위 테스트 전체 성공).
3. **백그라운드 수집 서비스 (`TripTrackingService`)**:
   - 주행 중 화면 주의분산 방지(CarUxRestrictions)를 위해 점수/순위 미노출, 백그라운드 수집 전담.
   - 저우선순위 상시 알림 ("오늘의 레이스 진행 중 - 결과는 주차하면 공개돼요") 유지.
   - P단 감지 시 결과 카드 화면(`MainActivity`) 자동 호출 인텐트 발행.
4. **모의 주행 신호 프로바이더 (`MockVehicleDataSource`)**:
   - 에뮬레이터 또는 오프라인 PT 시연(12/3~4)에서 이긴 날(92점, 18.4km) 및 아쉬운 날(64점, 22.1km) 주행 신호를 실시간 재생/주입할 수 있는 시뮬레이터 구축.

#### [작업 5] 화면 5종 구현 및 특수 모션(불타는 모션 / 비 내리는 모션 / 날씨 반영) 탑재
설계 문서(`2026-11-03-screen-mockups-design.md`) 및 HTML 목업을 바탕으로 5대 화면 UI와 요청된 특수 시각 효과를 완벽하게 구현함.

1. **특수 시각 효과 커스텀 뷰 구현**:
   - **`FlameScoreView` (고득점 불타는 모션)**:
     - 65개의 화염 파티클(주황/노랑/빨강)이 점수 텍스트 주변에서 상방으로 일렁이며 솟아오르고, 은은한 방사형 글로우(RadialGradient) 펄스가 주기적으로 이글거리는 모션 구현.
   - **`RainScoreView` (낮은 점수 비 내리는 모션)**:
     - 50개의 회색 빗줄기 파티클이 점수 위로 비스듬히 차분하게 주룩주룩 흘러내리는 쓸쓸한 비 내림 모션 구현.
   - **`WeatherAmbientView` (주행 중 화면 실시간 날씨 반영 모션)**:
     - 맑음(SUNNY): 우상단 은은한 햇살 광채 펄스.
     - 비(RAINY): 차창에 부드럽게 내리는 빗줄기.
     - 눈(SNOWY): 부드럽게 흩날리는 눈송이 파티클.
     - (CarUxRestrictions 준수: 운전자 시야 방해를 막기 위해 15~20%의 극저자극 알파로 블렌딩).
   - **`CircuitRouteView` (가상 출퇴근 경로 서킷 및 사건 렌더러)**:
     - 100×60 상자 좌표를 화면에 맞춰 스케일링하여 아스팔트 트랙, 중앙 대시선, 출발 녹색원, 도착 체커기 마커 및 리플레이 사건 점들을 렌더링.
   - **`KerbView` (서킷 연석 띠)**:
     - 레이스 레드/트랙 화이트 교차 줄무늬 연석 및 아쉬운 날용 Muted 회색 연석 렌더러.
   - **`PositionChartView` (F1식 포지션 차트)**:
     - 요일별 순위 변동 polyline, P119 승급 라인 점선, 금요일 최고 상승(+3) 강조 렌더러.

2. **화면 5종 구현 및 정합성 검증**:
   - **결과 카드 (이긴 날)**: 92점 + `FlameScoreView` 불타는 모션, 빨간 띠(▲3), 승급 라인까지 6계단, 연속 13일, 18.4km, 순위 타워(P123~P128, P125 나 [노란 줄], P126 해운대드라이버 [추월], "0.3점 차, 다음 주행에서 따라잡을 수 있어요").
   - **결과 카드 (아쉬운 날)**: 64점 + `RainScoreView` 비 내리는 모션, 피트 보드(차선 바꿀 때 깜빡이부터 켜기, 오늘 깜빡이 없이 차선 변경 4회, 매너 +9점 가능), 연속 주행 13일 → 0일 ("다음 주행 70점이면 1일부터 다시"), 순위 타워(P125 해운대드라이버 [목표 점선 테두리], "0.4점 차, 한 번만 잘 달리면 되찾아요", P126 나 ▼1).
   - **레이스 리플레이**: 좌측 대형 서킷 경로(사건 마커 점 포함), 우측 사건 시간순 목록(6:12 급가속, 6:17 차선변경, 6:21 급제동, 6:26 차선변경, 6:31 차선변경, 6:34 급제동, 6:38 차선변경), 하단 피트 보드, 상단 뒤로가기 버튼.
   - **리그 순위 타워**: P125 대형 순위, 승급 라인까지 6계단 · 0.9점, 티어 5칸(골드 강조), 안전운전 효용 2칸(아낀 돈 ₩8,400 / 줄인 급제동 3,120회), 순위 타워(P1, P2 ... P118, P119, 승급 라인 점선, P124, 나 P125, P126 ... 강등 라인 점선).
   - **주간 리포트**: 주간 평균 88.8 (+3.1), 요일별 점수 막대 그래프(일요일 최고점 노랑, 수요일 휴식), 우측 포지션 차트(P131 → P125 ▲6, P119 승급 라인, 금요일 +3 강조), 하단 피트 보드.
   - **주행 중 화면**: 배경 `WeatherAmbientView` + 날씨 전환 버튼(맑음/비/눈), 아이오닉 5 선 실루엣(투명도 18%), "주행 기록 중", "오늘의 레이스 진행 중", 1km 트랙 프로그레스(앞 3칸 기록 인정 + 체커기), 11.2 / 18.4km, "70점 넘기면 연속 13일째".

3. **시연 편의 기능 탑재**:
   - 상단 내비게이션 바로 6개 화면을 원클릭 즉시 전환.
   - [이긴날 시연], [아쉬운날 시연] 버튼 탑재로 가상 주행 $\rightarrow$ P단 주차 $\rightarrow$ 결과 카드 자동 팝업 라이프사이클을 100% 재현 시연 가능.

4. **빌드 및 패키징 완료**:
   - `assembleDebug` 및 `testDebugUnitTest` (15개 전체 통과) $\rightarrow$ `app/build/outputs/apk/debug/app-debug.apk` (18MB) 생성 완료.

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
6. **특수 모션 애니메이션 설계**:
   - 고득점(이긴 날) 시에는 큰 점수 텍스트 주변으로 화염 입자가 솟아오르는 `FlameScoreView`를 배치하여 승리의 성취감을 극대화함.
   - 낮은 점수(아쉬운 날) 시에는 쓸쓸하게 빗줄기가 흘러내리는 `RainScoreView`를 배치하여 아쉬움을 직관적으로 시각화함.
   - 주행 중 화면의 날씨 애니메이션은 CarUxRestrictions 안전 지침을 철저히 준수하여 15~20%의 부드럽고 차분한 투명도로 배경에 융합되도록 튜닝함.

---

### 3. 막힌 점 및 원인 해결
- **급가속 단위 테스트 실패**:
  - 원인: 테스트 케이스 작성 시 감속 테스트 직전 속도(10m/s에서 15m/s로 호출)가 1초 간격으로 주입되어 의도치 않은 급가속 1회가 추가로 감지됨.
  - 해결: 10m/s 상태에서 브레이크 페달을 밟고 5m/s로 감속하도록 테스트 시나리오를 정밀 보정하여 15개 테스트 전수 통과 완료.
- **코틀린 뷰 속성 컴파일 에러**:
  - 증상: `TextView.textStyle` 참조 불가 에러 발생.
  - 원인: `textStyle`은 XML 전용 속성이며, 코드에서는 `setTypeface(null, Typeface.BOLD)`를 사용해야 함.
  - 해결: `setTypeface(...)`로 전면 교체하여 컴파일 성공.

---

### 4. 확인 못 한 것
- 오프라인 대회장(12/3~4)에서 제공되는 실제 Virtual Console과 에뮬레이터 간의 네트워크 포트/IPC 연동 세부 파라미터. (단, 에뮬레이터 내부 VHAL 표준 프로퍼티를 따르므로 코드 레벨에서는 표준 리스너를 유지하면 정상 수신됨).

---

## 2026-11-03 (Claude: 검토 지적 21건 수정)

검토: `docs/app-review-2026-11-03.md`. 단위 테스트 33개 통과(점수 8, 수집 15, 데모 데이터 8, 경로 2), `assembleDebug`·`lintDebug` 통과.
새 테스트는 수정한 코드에 원래 버그(트립 거리 미초기화, 급가속 중복 집계)를 다시 넣으면 실패하는 것을 확인함.

### 항목별 결과
| # | 내용 | 결과 |
|---|---|---|
| 1 | Pleos SDK 미연결 | 고침: Vehicle SDK 리스너 10종(javap로 실제 시그니처 확인), 속도는 AAOS CarPropertyManager, 경로는 FusedLocation. 실패는 `problems`로 모아 시연 모드에 표시 |
| 2 | CarUxRestrictions 미사용 | 고침: 제한 중에는 주행 중 화면만, 다른 화면 진입 차단. 액티비티 `distractionOptimized` |
| 3 | 주행 중 날씨 애니메이션·버튼 | 고침: WeatherAmbientView 삭제, 정지 화면, 1km 단위 갱신 |
| 4 | P단 → 결과 카드 | 고침: 앞에 있으면 콜백, 뒤면 full-screen intent 알림, `onCreate`/`onNewIntent`에서 extra 읽음 |
| 5 | 두 번째 트립 거리 | 고침 + 테스트 |
| 6 | 위치 권한 | 고침: 런타임 요청, 권한 있을 때만 location 타입 FGS(없으면 dataSync) |
| 7 | 시연 컨트롤 상시 노출 | 고침: 왼쪽 위 76dp 모서리 길게 누르면 열림 |
| 8 | 가독성 | 고침: 24sp 미만 글씨 없음, 버튼 76dp, 차트 글씨 sp 단위 |
| 9 | 아쉬운 날 빨강·토큰 외 색 | 고침: 급제동 표시 흰색, 강등 라인은 토큰 빨강, `#F07A80`·`#151719` 제거 |
| 10 | 글꼴 | 고침: Black Han Sans, Saira Condensed(OFL, Google Fonts 공식 저장소) 번들 |
| 11 | 숫자 하드코딩 | 고침: 화면 숫자는 DemoData·LeagueCalculator에서 계산(아쉬운 날 순위표 등은 DemoData로 이동) |
| 12 | 점수식 | 고침: 에코=리그 평균 대비(80%→0, 120%→20), 꾸준함=연속 7 + 거리 3(기본점 없음), 매너=비율, 도어·남은 경고 감점 |
| 13 | 급가속·급제동 중복 | 고침 + 테스트(5Hz·3초 = 1회) |
| 14 | 차선 변경 감지 | 고침 + 테스트(고속 4°/저속 10°, 1~6초 복귀, 45° 초과·6초 초과는 회전, 깜빡이 5초 유예) |
| 15 | 트립 경계 | 고침 + 테스트(움직여야 시작, P→R→P 무시, 상태 초기화, 남은 경고 집계, 도어) |
| 16 | 불꽃·비 모션 | 유지(사용자 요청). 보일 때만 동작, 30fps 상한 |
| 17 | 패키지명 | 고침: `io.github.ddayoung03.safetyleague` |
| 18 | 메모리 저장 | 고침: 앱 내부 `trips.json`에 저장(경로 포함, 외부 전송 없음) |
| 19 | 테스트 부실 | 고침: 센서 경로로만 사건 생성, 카운터 직접 조작(`triggerEvent`) 삭제, 감점·0점 하한·두 번째 트립 테스트 추가 |
| 20 | 이모지·백업·아이콘 | 이모지(날씨 버튼과 함께) 삭제, `allowBackup=false`. **아이콘은 아직 기본 아이콘** |
| 21 | Float 반올림·시각 문자열 비교 | 고침: Double 반올림, 시각은 분 단위 숫자로 비교 |

### 직접 정한 것 (이유)
- 감점 계수: 10km당 급가속 -3, 급제동 -4(급제동이 더 위험), 벨트 미착용 -8, 경고 30초마다 -2, 주행 중 도어 열림 -3. 임계값 급가속 3.0 m/s²(일반 출발 1.5~2.5), 급제동 -3.5 m/s²(일반 정지 약 -2). 실차 데이터로 조정 필요.
- 시연 모드 각본 주행의 결과 카드는 기획서 숫자(92점/64점)를 그대로 보여 준다. 실제 신호로 달린 트립은 계산된 점수를 보여 준다. 리그(다른 운전자)는 실제 서버가 없어 시연용 가상 데이터.
- 내비 목적지 연동(NaviHelper)은 하지 않음: 실제 트립의 주행 중 트랙은 체커기 없이 달린 km만큼 늘어난다.
- `automotive_app_desc`(media 용도)는 이 앱에 맞지 않아 삭제.

### 확인 못 한 것 (에뮬레이터 필요)
- 실제 VHAL 신호 수신, P단 결과 카드, 주행 중 제한, 화면 글씨·버튼 실측. Pleos 에뮬레이터 이미지가 아직 설치되지 않음.
- `pleos.car.permission.PRIVATE_CAR_SAFETY`(경고등)가 서드파티 앱에 허용되는지.
- Pleos 앱 심사: 위치 데이터를 쓰므로 이용약관·개인정보 처리방침 제출과 사용자 동의가 필요(심사 가이드). 앱 안 동의 화면은 아직 없음.

## 2026-11-03 (Claude: Pleos 에뮬레이터 실검증)

환경: AVD `Pleos_Connect`(Pleos Connect v2.1.0, cold boot, 4GB), Virtual Console `adb forward tcp:8088 tcp:8088` → `http://127.0.0.1:8088/`.
Virtual Console에는 문서에 없는 **주행 시뮬레이터**(D + ACCEL 누르기, BRAKE, 조향 슬라이더)가 있어 속도를 실제로 바꿀 수 있다.

### 확인된 것 (실제 신호)
- 권한: Pleos 권한 전부와 `PRIVATE_CAR_SAFETY`(경고등), `CAR_SPEED`, 위치가 부여됨.
- 신호 수신: Pleos 서비스가 앱에 `VENDOR_GEAR_SELECTION`, `PERF_STEERING_ANGLE` 등을 보냄. 전비·연비는 이 에뮬레이터에서 `ERROR_NOT_SUPPORTED_PROPERTY`(에코는 리그 평균 처리).
- 주행 중 잠금: D + 가속으로 움직이자 앱이 스스로 주행 중 화면으로 바뀜(CarUxRestrictions).
- P단 결과 카드: P로 바꾸자 결과 카드가 자동으로 열림. 8.2km, 80점(부드러움 21.4·매너 25·에코 10·안전 15·꾸준함 8.7), 급가속 1·급제동 1.
- 깜빡이 없는 차선 변경: 실제 조향 신호(-6°)로 1회 감지(1차 주행).
- 저장: 에뮬레이터를 완전히 껐다 켠 뒤에도 트립이 남아 있음.

### 실검증에서 찾아 고친 버그 (재현 테스트 먼저)
- 급가속 25회: 실제 속도 신호가 계단식으로 자주 와서 표본 사이 가속도가 기준 위아래로 흔들림 → 가속도를 0.5초 이상 간격으로 재고, 기준의 절반 아래로 떨어져야 사건 종료. 테스트 `jitteryAccelerationFromRealSignalsCountsOnce`(15→1).
- 거리 0km: 에뮬레이터 오도미터가 주행해도 안 움직임 → 오도미터가 안 움직이면 속도×시간 적분값 사용. 테스트 `distanceFallsBackToSpeedWhenOdometerDoesNotMove`(0→4.0km).
- 점수 80.10000000000001 저장 → 소수 첫째 자리로 반올림.
- 실제 트립에 "다른 날의 예시" 표시, 3km 미만 트립이 "0점·연속 기록 0일"로 보임 → 실제 트립은 라벨 숨김, 3km 미만은 "3km 이상 달리면 기록돼요"·연속 기록 그대로.
- 리플레이 피트 보드가 고정 문구 → 트립 사건에서 고름. 위치가 안 바뀐 트립은 "위치 변화가 없어 경로를 그리지 못했어요" 표시(가짜 경로를 그리지 않음).

### 에뮬레이터 한계
- 위치(GPS)가 주행해도 안 바뀌어 실제 경로 리플레이는 에뮬레이터에서 볼 수 없음. 시연은 시연 모드 각본(가상 경로)으로.
- 오도미터 안 움직임(위 대체 처리), 전비·연비 미지원.
- Virtual Console 조향 슬라이더는 부호가 반대(6 → -6). 감지는 절댓값이라 영향 없음.

## 2026-11-03 위치 대체 순서 (FusedLocation → NaviHelper → LocationManager)
Pleos 콘솔에서 FusedLocation이 "준비 중"이라 실제 차량에서 못 쓸 수 있어 대체 순서를 넣음.
- `core/collector/LocationFallback.kt`(순수 로직): 앞에서부터 시작되는 출처 하나만 켬. 켜진 출처가 동작 중 실패하면 끄고 다음으로. 실패는 `problems`(시연 모드 상태 표시)에 남김. 테스트 `LocationFallbackTest` 3개.
- `PleosVehicleDataSource`: FusedLocation(`isSupported()` false면 건너뜀) → NaviHelper(2초마다 `getCurrentLocationInfo()` 요청, 응답은 binder 스레드라 메인으로 넘김) → LocationManager(위치 권한·켜진 제공자 있을 때). 앞의 두 출처는 15초 동안 위치가 한 번도 안 오면 실패로 보고 넘김. (0,0) 좌표는 버림.
- 매니페스트: `pleos.car.permission.NAVI_ROUTE` 추가(lint가 `getCurrentLocationInfo`에 필요하다고 알려줌).
- 에뮬레이터 확인: 기본은 `Location source: FusedLocation`(1초마다 위치). FusedLocation을 잠시 빼고 빌드하면 `Location source: NaviHelper`로 2초마다 `onCurrentLocationInfo` 수신 확인 후 원복.
- 단위 테스트 38개 통과, `assembleDebug`·`lintDebug` 통과.

## 2026-11-03 앱 아이콘 (B안 "승급 방패")
- 시안 3종(체커기 차선 / 승급 방패 / 서킷 연석) 중 B안 확정: 아스팔트 회색 배경, 노란 방패, 승급 화살표 두 개, 체커 띠. 작은 크기에서도 형태가 가장 또렷함. C안은 금지 표지판처럼 보여 제외.
- 적응형 아이콘: `mipmap-anydpi-v26/ic_launcher(_round).xml`, 전경 `drawable/ic_launcher_foreground.xml`(안전 영역 안에 들도록 0.85배), 배경색 `icon_background`. 매니페스트 기본 아이콘 교체(리뷰 #20 남은 항목).
- 제출·스토어용: `docs/app-icon.svg`, `docs/app-icon-512.png`(512×512).
- 에뮬레이터 앱 목록에서 표시 확인.

## 2026-11-03 화면 비율·크기 (impeccable 비평 P1-1, adapt)
비평(`.impeccable/critique/`, 25/40): 시안 1280×720 구성이 고정 dp로 옮겨져 실제 앱 영역(약 1690×1210dp)에 작게 몰림.
- `ui/view/DesignCanvas.kt`: 화면 컨테이너가 1280dp 너비 캔버스를 창 크기에 맞춰 통째로 확대. 창이 시안보다 세로로 길면 캔버스 높이를 늘리고 각 화면의 늘어나는 여백이 채움(잘림·여백 띠 없음).
- 6개 화면 크기를 시안 CSS 값(px = dp)으로 맞춤: 결과 점수 340sp, 리그 P125 200sp, 리포트 평균 150sp, 주행 중 제목 84sp, 순위 줄 높이·글자(화면별 `RowSize`), 타워 폭 등.
- 아쉬운 날 화면을 시안 배치로 다시 짬: 피트 보드 왼쪽, 순위 타워 아래 리플레이 보기 → 통계, 내 순위 줄 흰색(시안), "다른 날의 예시"는 오른쪽 아래.
- 내 순위 줄이 시안처럼 왼쪽으로 튀어나오게 clipChildren/clipToPadding 해제.
- 리플레이: 사건 목록이 남은 높이를 다 쓰게(잘림 해결), 지도 760×456, 뒤로 버튼을 "← 결과로" 76dp 버튼으로.
- 리포트: 막대 칸 높이 부족으로 요일 글자가 잘리던 문제 수정(230dp, 막대 최대 150dp, 쉬는 날은 "휴식" + 얇은 선), 차트 축 글꼴을 앱 글꼴로, 승급 라벨 겹침 수정, "+3" 강조를 데이터에서 계산.
- "리플레이 보기" 아이콘을 시스템 흐린 아이콘에서 진한 삼각형(`drawable/ic_play.xml`)으로.
- 에뮬레이터에서 6개 화면 캡처로 확인. 남은 것: 레이싱 개성 요소(체커 띠, 기울어진 행, 주행 중 실루엣·차 아이콘·결승 체커), 이긴 날 불꽃이 점수를 덮는 문제.
- 같은 날 추가: 기울임 숫자 잘림("88.8"의 마지막 8, "92"의 2, 순위 표 "89.7"의 7). Saira는 똑바른 글꼴이고 기울임은 Android가 비틀어 그리는 가짜 기울임이라 오른쪽 끝이 글자 칸 밖으로 나온다. TextView는 패딩 안쪽으로 잘라 그리므로 여백만으로는 안 됨 → 글자 크기 20%만큼 오른쪽 여백 + 같은 반경의 거의 투명한 그림자(TextView가 그림자 반경만큼 그리는 범위를 넓힘). XML에서 그림자 색을 완전 투명(#00000000)으로 주면 무시되므로 #01000000. 코드의 `text(number = true)`도 같은 처리. 여백 때문에 생긴 줄바꿈(리그 P125, 아쉬운 날 연속 기록 안내)과 "점" 간격도 맞춤.
- 같은 날: 재시작 후 에뮬레이터가 부팅 직후 반복 종료 → Windows 오류 기록상 NVIDIA OpenGL 드라이버 TDR(nvoglv64.dll). `-gpu swiftshader_indirect`(소프트웨어 그리기)로 띄우면 안정적. 해결: NVIDIA 드라이버 577.05(2025-08) → 617.42(2026-09-30) 업데이트 후 `pleos.md`대로 GPU host·cold boot로 부팅 50초, 6개 화면 렌더링·불꽃 화면 3분 띄워도 종료 없음. 소프트웨어 그리기는 더 쓰지 않는다.
## 2026-11-03 Codex polish — 1. 기울어진 배경
- 공용 `SkewDrawable`: -14° 평행사변형 배경, 글자·터치 영역은 수평 유지. 순위 행, 리플레이 버튼, 리그 탭·티어·승급 알약, 리포트·리플레이 작전 띠 적용.
- 목표 행의 노란 점선을 같은 경로에 유지. 기존 색 토큰 사용, core 변경 없음.
- 검증: `testDebugUnitTest assembleDebug` 성공. 화면 비교는 5개 항목 적용 후 함께 진행.

## 2026-11-03 Codex polish — 2. 체커기 띠
- 이긴 날 오른쪽에 폭 150dp, 오른쪽 90dp 넘침, 중심 기준 -14° 기울임, 40dp 체커, alpha 0.85 적용. 화면 가장자리에서 잘리며 다른 화면에는 표시하지 않음.
- 검증: `testDebugUnitTest assembleDebug` 성공. Pleos_Connect cold boot 부팅 확인.

## 2026-11-03 Codex polish — 3. 아스팔트 배경
- 결과 2장·리플레이·리그·리포트에 타원형 방사 조명과 240×240 고정 노이즈 타일 반복 적용. 타일은 최초 한 번 생성하고 재사용하며 프레임마다 생성하지 않음.
- 주행 중은 기존 바탕 위 조명·노이즈 강도를 0.5로 낮춤. 조명 색상은 colors.xml 토큰 추가.
- 검증: `testDebugUnitTest assembleDebug` 성공. 다음 항목의 미구현 테스트가 먼저 포함되어 실패한 실행은 분리 후 재검증.

## 2026-11-03 Codex polish — 4. 주행 중 화면
- 시안 SVG를 VectorDrawable로 옮겨 480×176dp, 오른쪽 70dp·위 318dp, alpha 0.18 실루엣 적용. 자동차도 64×30dp 벡터로 구현.
- 정수 km 상태에 따라 채워지는 -14° 트랙, 목적지가 있을 때만 60dp 결승 칸(22dp 체커 타일), 앞 3칸에 맞춘 밑줄. 자동차는 마지막 채운 칸 끝으로 즉시 이동하며 animator 없음.
- 실제 목적지 미연동 상태는 기존대로 결승 칸 없이 늘어남. UI 상태 계산만 ui/view에 분리, core 변경 없음.
- 검증: DrivingTrackStateTest 3개(1km 경계·목적지 없음·도착 초과), 미구현 실패 확인 후 구현. `testDebugUnitTest assembleDebug` 성공.

## 2026-11-03 Codex polish — 5. 점수 영역 불꽃 제외
- 실제 점수 TextView의 배치 영역을 불꽃 좌표로 변환해 영역에 닿는 입자 전체를 제외. 숫자 뒤의 은은한 조명과 주변 불꽃 모션은 유지.
- 불꽃·비 효과가 Lifecycle onPause에서 멈추고 onResume 및 실제 표시 여부에 따라 재개. 프레임 간격 34ms로 30fps 이내 유지. 불꽃 색상은 colors.xml 토큰으로 이동.
- 검증: `testDebugUnitTest assembleDebug` 성공, APK를 Pleos_Connect에 설치·실행. 최종 화면 비교 진행 중.

### 최종 검증 및 검토
- `testDebugUnitTest assembleDebug lintDebug` 성공. 단위 테스트 42개, 실패 0. `core/` 변경 없음.
- 독립 코드 검토에서 긴 주행(약 185km 이후)의 고정 간격으로 칸 너비가 음수가 되는 경우 발견. 185/500/1000km 회귀 테스트 실패 재현 후 칸 수에 따라 간격을 줄여 해결.
- Pleos_Connect GPU host·cold boot, 최종 APK 설치·실행. 6개 화면 캡처와 시안 나란히 비교, 점수·순위·문구 잘림 없음. 아쉬운 날은 회색 연석·빨강 없음. 주행 실루엣은 글자와 분리됨.
- 캡처·비교 자료: 로컬 `captures/polish/` (원본 6장, `*-compare.png`, `comparison-sheet.png`; git 제외). Pleos의 더 긴 세로 영역을 사용하는 기존 배치 차이는 유지.
- 불꽃은 숫자 영역에 닿는 원 전체를 숨겨 반원으로 잘리는 경계도 방지. 일시정지·재개는 Lifecycle 연결을 코드 검토로 확인; 별도 계측 테스트는 하지 않음.
- main 병합·push 없음. 기존 미추적 `.impeccable/` 보존. 후속 검토: Claude의 브랜치 검토.

### 2026-11-04 design v2 수정 1묶음 · 아침 확인용 요약
- 7번: 미완료 날씨 테스트 구현(d89db22); 맑음 전환 1회 허용·정차 중 맑음 반복 차단.
- 1·2번: 차량 신호 오류는 Logcat만(5a9919f), 불꽃 제외 영역을 글자·칩·말풍선·버튼 전체로 확대(f28f8ab).
- 3·4번: 이긴 날 캐릭터를 시안 상단 346dp로 고정(081e773), 아쉬운 날 그림을 제목 아래로 내리고 보드 글자 정렬(f796f13).
- 검증: 각 항목 testDebugUnitTest assembleDebug -q 통과(54개 테스트 실패 0); captures/review-batch1/ 결과 2장 1회 확인. 남은 5·6·8~11번 미착수, core·mockups 변경 없음.

### 2026-11-04 — design v2 수정 2묶음
- 5번(2a7aa93): 리포트 요일·라벨은 Pretendard, 숫자는 Saira 기울임 적용, 승급 라벨 위치 조정. 6번(14f8926): 리플레이 경로와 목록에 사건 아이콘 VectorDrawable 3종 적용.
- 8번(c4569c4): 정차 판정에 < 0.3m/s 임계값 적용, 경계값 단위 테스트 추가. 추가 수정(53448fd): 이긴 날 경로 시작점을 40dp 옮겨 팀 라디오 패널과 분리.
- 각 항목 testDebugUnitTest assembleDebug -q 통과(당시 기록: 55개 테스트). 결과·리포트·리플레이 PNG 3장은 captures/review-batch2/에 저장.
- 남은 것: 검토 9~11번(주행·리그 캡처, v2 작업 기록, 캐릭터 등장 연출). core·mockups 변경 없음, 기존 untracked 로그와 .impeccable 커밋 제외.
- 복원 근거: 6b1c028의 손상된 추가 부분과 2a7aa93·14f8926·c4569c4·53448fd 커밋 메시지 및 변경 내용. 원문을 확정할 수 없는 문구는 재구성하지 않음.

### 2026-11-04 — design v2 수정 3묶음
- 1번: 손상된 2묶음 기록 UTF-8 복원(e1f18d7). 2번: 데모 탭과 DesignCanvas를 세로 배치하여 탭이 화면을 덮지 않도록 수정(e56b011). 각각 testDebugUnitTest assembleDebug -q 통과.
- 원 지시 1번 484bd8c: Pretendard·Saira 및 색 토큰. 2번 bf39b86: 유리 패널·금속 숫자·캐릭터 공통 렌더링. 3번 56a20e3: 이긴 날 캐릭터와 팀 라디오. 4번 e12565f: 아쉬운 날 피트 크루와 라디오.
- 원 지시 5번 96078d0: 리그 금속 순위·반투명 패널. 6번 84cf149: 리플레이 얼굴·연석. 7번 2a4a15f: 주간 리포트와 리그 제목. 8번 6e78c5c: 주행 위계·숫자 잘림 방지. 10번 4d9b9d3: 지원 여부 확인 후 날씨 배경과 움직임 정책 적용. 이후 검토 수정은 위 1·2묶음 참조.
- 사용 신호: CarPropertyManager의 WINDSHIELD_WIPERS_STATE, ENV_OUTSIDE_TEMPERATURE, HEADLIGHTS_STATE, PERF_VEHICLE_SPEED, GEAR_SELECTION. Pleos 대체 경로는 checkWiperStateCapabilities / checkLowBeamLightStateCapabilities로 읽기 가능 여부 확인 후 구독.
- 에뮬레이터 확인: WINDSHIELD_WIPERS_STATE·HEADLIGHTS_STATE는 unsupported or permission denied(Logcat, captures/review-batch3/signals.log). ENV_OUTSIDE_TEMPERATURE는 기존 캡처의 25.0 수신 확인. 속도·기어는 이전 실검증에서 지원 확인. Pleos 와이퍼·하향등은 capability 콜백 경로는 있으나 실제 값 변경 지원은 미검증(오류가 없다는 이유로 지원으로 간주하지 않음).
- 날씨 신호를 읽지 못하면 기본/시연 값을 사용하고 오류는 Logcat에만 기록. 실제 우천·강설·야간 신호 변화는 실차에서 추가 확인 필요.
- 4번(원 지시 9번): 결과 카드 캐릭터에 12dp → 0dp, 280ms 1회 ObjectAnimator 추가. 아쉬운 날 피트보드 글자도 함께 이동. P단·정차·RESUMED·UX 제한 없음일 때만 시작하며, 화면 전환·onPause·주행 신호 수신 시 취소하고 원위치로 복원. 반복·onResume 재시작 없음.
- 등장 허용 조건 단위 테스트는 구현 전 실패, 구현 후 testDebugUnitTest assembleDebug -q 통과(step2-red.log / step2-build.log). SDK XML 도구 버전 경고는 환경 변경 금지에 따라 유지.
- 5번: Pleos_Connect 기존 GPU host 설정으로 콜드 부팅 후 APK 설치. captures/review-batch3/에 5-driving·5b-driving-rain·5c-driving-snow·5d-driving-night·4-league.png 저장, comparison.png에서 같은 이름의 mockups/png 5장과 비교. 화면 전환 직전 프레임이 찍힌 초회 캡처는 폐기하고 UI 날씨 문구 확인 후 다시 저장.
- 비교 결과: 제목·차량 실루엣·트랙·리그 순위의 큰 겹침/잘림 없음. 실제 앱 영역 1682×1210과 시안 1280×720의 비율 차이로 세로 여백이 더 큼. 야간 달 모양·금속/패널 광택의 작은 차이는 최소 diff 원칙에 따라 유지. 주행 화면의 점수 유도 문구는 기존 안전 안내 문구를 유지(주행 중 점수 금지).
- 날씨 4종은 정차한 에뮬레이터에서 시연 값으로 촬영했으며, 실차 우천·야간 신호 수신 증거는 아님. 움직임은 코드와 WeatherStateTest 14개로 확인: 속도 절댓값 0.3m/s 이상은 정지 그림, 미만은 정차로 판정; 비·눈·밤은 정차 시 34ms 간격, 맑음은 반복 없음. 주행 중 날씨 변경은 1초·분당 1회만 허용. onPause·비표시·detach에서는 프레임 예약 취소. 4종 전체의 주행/정차/전환/비활성 조합 테스트 추가.
- 5번 검증: testDebugUnitTest assembleDebug -q 통과(총 57개, 실패/오류 0; step3-build.log). 시안과 core 변경 없음.

### 2026-11-04 — design v2 마지막 묶음 · 확인용 요약
- 한 일: 미추적 CODEMAP 원본 커밋(8c65e99), 캐릭터 등장 30fps 제한 2줄 별도 커밋(fe48229), 초승달 수정 별도 커밋(dfd4fe3), 브랜치 변경 코드의 lint 수정(d3823c7).
- 달: 1280×720 기준 (1100,100), 반지름 34 원에서 (1116,88), 반지름 30 원을 Path.Op.DIFFERENCE로 제외. 경로는 View 생성 때 한 번 만들고 재사용하며 기존 white 색과 약 .55 불투명도, fade를 적용. 가로 기준 s = width / 1280f를 양 축에 동일 적용해 세로 늘어짐 수정(997a4d6). 맑음의 해는 유지. testDebugUnitTest assembleDebug -q 1회 통과, 테스트 57개 실패/오류 0(build-moon-scale.log).
- 검증: testDebugUnitTest assembleDebug lintDebug -q 최초 실행과 lint 수정 후 실행 모두 종료 코드 0. 안쪽 Canvas 경고 제거 후 바깥쪽 동일 경고가 추가 검출되어 수정 후 같은 명령을 한 번 더 실행함(총 3회). 최종 단위 테스트 57개, 실패/오류/건너뜀 0. 등장 연출 관련 테스트 실패나 추가 수정 없음. 로그: captures/design-v2-final/build.log, build-first.log, build-lint-first.log.
- lint: main 대비 변경 줄의 경고 0건. 기존 경고 200건. 문자열은 리소스로 옮겼으며 표시 문구는 유지. 차량 실루엣은 벡터 기본 크기만 축소하고 화면의 480×176dp 배치 크기는 유지.
- 화면 확인: 진단 screencap에는 권한 창 없이 결과 화면이 표시됨. 재설치 직후 첫 시도에서 UI 트리가 null이라 demo hotspot missing 발생, 이후 정상 트리에서 핫스폿 확인. 기존 좌표도 핫스폿 범위 안이므로 화면 준비 지연이 원인으로 추정됨. capture_night.py만 실제 핫스폿 좌표 사용 및 시작 대기 3초로 수정해 두 번째 시도 성공. captures/design-v2-final/night.png에서 야간 초승달의 세로 늘어짐 없이 원형 곡률 유지 확인. 진단 이미지와 스크립트도 같은 폴더에 보관.
- 남은 것: 실제 차량 날씨 신호 검증, 사용자 확인 후 담당자의 main 병합 검토. 밤 화면 촬영 및 초승달 모양 확인 완료. 이번 작업에서 main 커밋·병합·push, core·시안·시스템 설정 변경, .impeccable 커밋은 하지 않음.
- 사용자가 결정할 것: 주행 화면 목표 문구 — 앱 「안전하게, 다음 한 칸까지」, 시안 「70점 넘기면 연속 13일째」. 문구는 변경하지 않았으며 주행 중 점수 표시 금지 원칙을 함께 검토할 것.

### 2026-11-05 — 실제 점수 리그 연결·차종 배치 · 확인용 요약
- 한 일(Codex): 실제 기록과 시연 분리(3a98688), 시즌 순위 계산(13b6f20), 결과·리그 연결(70da051), 차종 판정·동력 리그(89fdfb4), 차종 선택·배치 화면(fbd0b3b). Codex 한도 소진으로 이후 검토·수정·검증은 Claude가 함.
- 한 일(Claude): 결과 카드에 리그 순위 버튼과 리그의 결과 버튼 추가(실제 모드에서 리그로 갈 길이 없었음), 차종 선택 전 트립을 그 리그에 넣기, 트립 날짜를 서비스 생성일이 아닌 시작일로(727fda9). `CAR_SPEED` 권한 요청과 허용 후 속도 재구독(4ef787b).
- 찾은 큰 버그: 앱이 `CAR_SPEED`를 요청하지 않아 새로 설치하면 속도가 안 들어와 트립이 시작되지 않았음. `registerCallback`은 예외 없이 false만 돌려줌(Logcat: propertyId is not in carPropertyConfig list). 이전 검증은 권한을 직접 부여한 상태였음.
- 에뮬레이터 확인: 첫 실행 권한 창 3개 → 차종 선택 → 배치 화면 → 빈 리그 → 주행 3회(43.3 / 73.3 / 73.3점) → P397 → P351, ▲46 → 재시작 후 유지. 시연 92점·64점·골드 리그 화면 유지, 각본 주행 뒤 trips.json 3개 그대로. 3km 미만 안내 확인. 단위 테스트 70개 통과.
- 차종 값: INFO_MAKE=Hyundai 42dot, INFO_MODEL=Connect, INFO_FUEL_TYPE=[10]. 목록 밖이라 에뮬레이터는 직접 선택으로 감. 자동 인식 화면은 단위 테스트로만 확인.
- 해결 1: 첫 설치 후 주행 중 Pleos 홈 화면이 「주행 중에는 사용할 수 없습니다」로 앱을 가리던 문제. 권한 창을 MainActivity 위에 띄운 작업 창에서만 재현됨(권한을 미리 주면 안 생김, 같은 프로세스에서 작업을 다시 만들거나 앱이 스스로 재시작해도 그대로, 프로세스를 죽이고 새로 열면 사라짐). 권한 요청을 별도 작업 창의 PermissionActivity로 옮겨 새 설치에서 해결. 홈 화면 내부 동작은 시험으로 추정한 것.
- 해결 2: 콜드 부팅 직후 TripTrackingService ANR. 차량 신호 구독을 HandlerThread로 옮김. 콜드 부팅 직후 새 설치 1회에서 ANR 0회.
- Virtual Console을 스크립트로 쓰려면 `POST /browser/session?client=...`를 1초마다 보내야 함(없으면 emulator_inactive 503). 주행 스크립트: captures/league-real-score/drive2.sh(저장소 밖).
- 사용자가 결정할 것: main 병합. 다음 작업 범위(지역·친구 탭, 주간 리포트 실데이터, 시즌 종료 티어 이동, 위치 동의 화면).

### 2026-11-05 — 리그 지역·친구 탭
- 한 일(Claude): 리그 화면의 차종(기본)·지역·친구 탭을 눌러 순위표가 바뀌게 함. 순위는 기존 `SeasonRanking`을 운전자 목록·표시 범위를 받도록 넓혀 재사용(`rank()` 분리, 총원·승급/강등 라인을 목록 크기로 계산). 지역 가상 운전자 148명·친구 5명은 `SeasonDrivers`에 둠. 실제 모드는 내 행만 실제 기록, 시연 모드는 시연 기록(11/9 시즌 평균)만 씀. 지역 이름은 `MainActivity.myRegion`("부산") 한 곳에서 정함(위치 동의 화면이 들어올 자리). 위치 권한 요청 없음.
- 지역·친구 탭에서는 티어 바와 승급 문구를 숨김(승급·강등은 차종 리그에만 해당). 차종 탭이 선택된 상태에서 다시 누르면 전처럼 차종 선택 화면으로 감. 리그 밖으로 나가면 차종 탭으로 돌아감.
- 검증: 클라우드에서 Android Gradle 플러그인을 받지 못해(dl.google.com 프록시 403) 앱 빌드·`testDebugUnitTest`는 못 돌림. 순수 코드(core/calculator·data·model)와 그 테스트만 별도 JVM Gradle 프로젝트로 돌려 35개 통과(새 테스트 5개 포함). `MainActivity`는 컴파일 확인 전.
- 사용자가 확인할 것: PC에서 빌드·단위 테스트, 에뮬레이터에서 세 탭 전환, 시연 값 유지, main 병합.

### 2026-11-05 — 주간 리포트 실제 트립 연결 · 확인용 요약
- 한 일(Claude): 주간 집계 순수 함수 `core/calculator/WeeklyReport.kt`(`WeeklyReportCalculator`)와 테스트 `WeeklyReportTest`(13개). 실제 모드 리포트를 `TripRepository`의 현재 리그 트립(`leagueTrips()`)으로 채움. 시연 모드 화면·값(11.03 ~ 11.09, P131 → P125 ▲6 등)은 그대로.
- 화면 변경: 리포트 머리에 「리그」 돌아가기 버튼, 순위 캡션에 id를 달아 실제 티어 표시. 실제 모드에서 리포트로 갈 길이 없어 리그 머리의 「결과」 옆에 「리포트」 버튼 추가(실제 모드에서만 보임). `MainActivity`는 리포트 부분과 리그의 버튼 연결 2줄만 바꿈. 막대 그래프 그리기는 `scoreBars()`로 빼서 시연·실제가 같이 씀.
- `PositionChartView`: 순위가 없는 날(null)을 건너뛰고, 승급 라인 위 순위와 넓은 순위 폭도 그림. 시연 값(P119 라인, P125~P131)에서는 칸 간격 3과 +3 강조가 전과 같음.
- 가정(스펙이 모호해 단순한 쪽으로 고름):
  - 한 주는 오늘이 든 월~일. 지난주는 그 앞 월~일. 제목 「N월 M주차」는 월요일이 속한 달 기준, 1일이 든 주가 1주차.
  - 평균은 그 주 인정 트립(3km 이상, 현재 리그) 점수의 단순 평균. 요일 막대는 그날 트립 평균을 반올림.
  - 주행 거리는 화면에 칸이 없어 실제 모드의 기간 줄에 붙임(「11.02 ~ 11.08 · 주행 3일 · 42.1km」).
  - 항목별 변화(5개 항목 이번 주 평균과 지난주 대비)는 화면에 칸이 없어 「다음 주 작전」 문구를 고르는 데만 씀: 만점 대비 가장 많이 떨어진 항목, 떨어진 항목이 없으면 만점 대비 가장 낮은 항목. 세부 점수가 없는 트립만 있으면 항목 문구 없이 승급 정보만.
  - 요일별 순위는 그날 끝까지의 트립으로 낸 그 달 시즌 순위(다른 운전자는 가상 데이터). 시즌 첫 트립 전과 오늘 이후는 비움. 주 중간에 달이 바뀌면 새 시즌 순위로 이어짐. 순위 변동은 첫 순위 → 마지막 순위.
  - 지난주 기록이 없으면 변화 배지를 숨기고 「지난주 기록 없음」.
  - 빈 상태(이번 주 트립 없음): 평균 「–」, 막대 모두 「휴식」, 문구 「이번 주 기록이 아직 없어요. 3km 이상 달리면 리포트가 채워져요」. 순위가 하나도 없으면 차트를 숨기고 「순위 대기」.
  - 지난주보다 낮아도 배지는 기존 노랑 그대로(빨강 안 씀).
- 검증:
  - Android 빌드(`testDebugUnitTest assembleDebug`) 실패: 클라우드 네트워크가 Google Maven(dl.google.com)을 막아 `Plugin [id: 'com.android.application', version: '9.0.1'] was not found`. SDK도 받을 수 없음. 그래서 `MainActivity`, `PositionChartView`, 레이아웃은 컴파일하지 못했고 눈으로만 검토함.
  - 순수 함수: 저장소 밖 JVM 전용 Gradle 프로젝트로 `core/calculator`, `core/model`, `core/data`와 그 테스트를 돌림. 43개 통과(DemoData 8, RouteNormalizer 2, ScoreCalculator 8, SeasonRanking 6, VehicleProfile 6, WeeklyReport 13). collector·weather·ui 테스트는 이 방법에 넣지 않음.
- 사용자 PC에서 확인할 것:
  1. `.\gradlew.bat testDebugUnitTest assembleDebug -q` 통과(컴파일 포함).
  2. 시연 모드 리포트 탭: 11월 2주차, 11.03 ~ 11.09, P131 → P125 ▲6, 평균·변화·막대·차트·작전 문구가 main과 같음. 「리그」 버튼은 안 보임.
  3. 시연 모드 리그 화면에 「리포트」 버튼이 안 보임.
  4. 실제 모드 리그 → 「리포트」 → 리포트, 「리그」로 돌아옴.
  5. 이번 주 트립 없음: 「–」, 휴식 7개, 빈 상태 문구, 순위 대기 또는 그 달 순위의 평평한 선.
  6. 주행 1~3회 뒤: 평균·요일 막대(가장 높은 날 노랑)·주행 일수·km·순위 변동·차트가 트립과 맞음. 같은 날 두 번 달리면 막대는 평균.
  7. 차트: P397 근처처럼 순위 폭이 클 때 눈금 글자가 겹치지 않는지, 승급 라인 위 순위일 때 라인이 보이는지.
  8. 3km 미만 트립은 리포트에 안 들어감. 다른 차종 리그의 트립도 안 들어감.
  9. 주행 중에는 리포트로 갈 수 없음(기존 제한 그대로).
- 병합 주의: 다른 세션이 리그 화면을 고치면 `view_league.xml` 머리 버튼과 `showLeague()`의 버튼 연결 2줄이 겹칠 수 있음.

### 2026-11-05 — 시즌 종료 티어 이동 · 확인용 요약
- 한 일(Claude): 티어 이동 순수 함수 `core/calculator/TierMovement.kt`(승급 상위 30%·강등 하위 30%, 기존 `promotionRank`/`demotionRank`와 `SeasonRanking.calculate` 재사용), 정산 저장 `core/repository/SeasonSettlementStore.kt`, 테스트 `TierMovementTest` 10개. `MainActivity.kt`는 4줄만 바꿈(켤 때 정산 호출, 정산 직후 첫 화면을 리그로, 리그 화면에 결과 한 줄).
- 정산 방식: 마지막으로 본 시즌(`season`)을 티어와 같은 `vehicle_profile` 파일에 두고, 새 티어·시즌·안내 문구를 한 번의 `commit()`으로 씀. 그래서 재시작해도 같은 시즌을 두 번 정산하지 않고, 도중에 꺼져도 티어만 바뀌는 일이 없음.
- 가정(스펙이 모호해 가장 단순한 쪽을 고름):
  - 첫 실행(또는 이 기능이 없던 버전에서 업데이트)에는 지금 달만 기록하고 정산하지 않음. 그래서 11월에 처음 깐 사용자는 12월 1일 이후 처음 켤 때 11월이 정산됨.
  - 이번 시즌 유효 트립(3km 이상)이 없으면 순위가 없으므로 유지.
  - 앱을 몇 달 안 켰으면 빠진 달을 차례로 정산하고(빈 달은 유지), 안내는 처음 티어→마지막 티어를 한 줄로 요약.
  - 차종을 고르기 전(프로필 없음)에는 티어가 없으므로 정산하지 않고 시즌 기록만 넘김.
  - 다른 운전자는 모든 티어에서 같은 가상 396명(`SeasonDrivers`)이라 인원은 397명 고정. 승급 P119까지, 강등 P279부터(리그 화면 표시와 같음).
  - 정산은 `onCreate`에서만 함. 앱이 켜진 채로 달이 바뀌면 다음 실행 때 정산.
  - 안내는 새 시즌 리그 화면에서 첫 유효 트립 전까지 맨 위 한 줄(노랑 `note`)로 보여 줌. 주행 중 화면에는 나오지 않음. 강등은 「브론즈 리그에서 새로 출발해요. 차근차근 다시 올라가 봐요」처럼 격려 말투, 빨강 없음.
  - 정산 직후 첫 화면은 지난달 결과 카드 대신 리그. 트립 종료 알림으로 열린 경우는 기존대로 결과 카드.
- 시연 모드: 리그 화면 시연 분기와 `DemoData`는 건드리지 않음(92점·64점·골드 397명 중 P125 그대로).
- 검증(클라우드):
  - `./gradlew testDebugUnitTest assembleDebug -q` 실패: `Plugin com.android.application 9.0.1 was not found` (dl.google.com 접속 거부, Android SDK 없음). 앱 빌드와 `MainActivity.kt` 컴파일은 확인 못 함.
  - 대신 저장소 밖 임시 Kotlin/JVM 프로젝트로 `core/calculator`·`model`·`data`를 컴파일하고 `core/calculator` 테스트를 돌림: RouteNormalizer 2, ScoreCalculator 8, SeasonRanking 6, TierMovement 10, VehicleProfile 6 — 32개 모두 통과.
  - `SeasonSettlementStore`는 Context·SharedPreferences 스텁(저장소에 넣지 않음)으로 한 번 확인: 10월 100점 → 11월 실버 승급, 새 인스턴스로 다시 불러도 재정산 없음, 차종 다시 저장해도 기록 유지. 실제 SharedPreferences 동작은 에뮬레이터에서 확인 필요.
- 사용자 PC에서 확인할 것:
  1. `.\gradlew.bat testDebugUnitTest assembleDebug -q` 통과(기존 70개 + 새 10개).
  2. 시연 모드: 92점·64점 결과, 골드 리그 397명 중 P125 화면 그대로.
  3. 새 설치 후 첫 실행: 정산 안내 없음, 기존 흐름(권한 → 차종 → 배치 → 리그) 그대로.
  4. 달 넘김 승급: 이번 달 높은 점수 트립 1개 → 에뮬레이터 날짜를 다음 달 1일로 → 앱 다시 열기 → 리그 화면, 위 티어 강조, 「N월 시즌 결과 · 실버 리그로 승급했어요. 축하해요!」.
  5. 재시작: 앱 강제 종료 후 다시 열어도 티어가 한 번 더 오르지 않음, 안내는 그대로 한 줄.
  6. 강등: 실버 이상에서 낮은 점수 트립만 → 다음 달 → 격려 문구, 빨강 없음. 브론즈에서는 내려가지 않음.
  7. 기록 없는 달: 트립 없이 달 넘김 → 「기록이 없어 … 유지해요」.
  8. 새 시즌 첫 유효 트립 뒤 리그 화면에서 안내가 사라지고 순위가 나옴.
  9. 정산 직후 주행을 시작하면 주행 중 화면만 보이고 안내·순위가 나오지 않음.
- 사용자가 결정할 것: main 병합. 안내를 첫 트립 전까지가 아니라 한 번만 보여 줄지.

### 위치 동의 화면 · 확인용 요약 (작업 환경 시계 2026-11-04, 바로 위 항목 다음에 한 작업)
- 한 일(Claude): 차종 선택·배치 뒤 「시작하기」에서 위치 동의 화면을 연다. 안내 문구: 경로는 리플레이에서 내 화면에만 보이고 리그에는 점수와 순위만 올라가며 경로는 공유하지 않음, 기록하지 않아도 점수·리그는 그대로. 동의는 SharedPreferences(`location_consent`)에 저장해 재시작 후 유지. 차종 화면(리그의 차종 탭)에 「경로 기록 설정」 버튼을 두어 나중에 바꿀 수 있음.
- 새 파일: `core/collector/LocationConsent.kt`(순수 함수 `LocationPolicy`), `core/repository/LocationConsentStore.kt`, `ui/LocationConsentScreen.kt`, `res/layout/view_location_consent.xml`, `res/values/strings_location_consent.xml`(strings.xml 충돌을 피하려고 분리), 테스트 `LocationPolicyTest`.
- 고친 파일: `MainActivity.kt`(화면 enum 1개, show 분기 1줄, 배치 「시작하기」·onCreate에 동의 확인 각 1줄, 차종 화면 버튼 1줄, 동의 처리 함수), `PermissionActivity.kt`(위치 권한은 동의한 뒤에만 요청, 별도 작업 창으로 여는 `askInOwnTask`), `PleosVehicleDataSource.kt`(동의 안 하면 위치 출처를 켜지 않음, `syncLocation`), `TripTrackingService.kt`(onStartCommand마다 `syncLocation`, 동의 안 하면 location 서비스 타입 안 씀).
- 위치 순서: 기존 `LocationFallback`(FusedLocation → NaviHelper → LocationManager, 실패 시 다음)을 그대로 재사용. 순서와 동의 판단은 `LocationPolicy.sourceOrder`로 순수 함수화.
- 권한 창: 동의하면 위치 권한이 없을 때 기존과 같이 MainActivity를 닫고 별도 작업 창의 PermissionActivity로 묻고, 끝나면 MainActivity를 새로 연다(「해결 1」 방식 그대로). 거부하면 권한 창 없이 리그(또는 마지막 결과)로.
- 가정(스펙이 모호해 가장 단순한 쪽을 고름):
  - 첫 실행 권한 창에서 위치 권한(FINE/COARSE/Pleos FUSED_LOCATION)을 뺐다. 동의 전에 위치 권한을 묻지 않기 위해서. CAR_SPEED·알림은 그대로 첫 실행에 묻는다.
  - 이미 설치되어 동의 기록이 없는 사용자는 다음 실행 때 동의 화면을 한 번 본다. 그 전까지 경로는 기록하지 않는다(위치 권한이 이미 있어도).
  - 동의했지만 OS 위치 권한을 거부하면 동의는 GRANTED로 남고 경로는 비어 있음. 기존처럼 다음 실행 때 권한 창을 다시 띄움.
  - 「경로 기록 설정」은 동의 여부를 정한 뒤에만 차종 화면 격자의 빈 칸(11개 다음 12번째)에 보인다. 리그 화면은 지역·친구 탭 작업과 겹치지 않게 손대지 않음.
  - 동의를 끄면 다음 트립부터 경로를 받지 않는다. 이미 저장된 트립의 경로는 지우지 않는다.
  - 시연 모드에서는 동의 화면을 띄우지 않으며, 각본 주행·92점/64점·골드 리그 397명 중 P125 값은 손대지 않음.
- 검증:
  - 전체 빌드: 실패. `./gradlew testDebugUnitTest` → `Plugin [id: 'com.android.application', version: '9.0.1'] was not found` (dl.google.com, nexus-playground.pleos.ai 연결 불가, Maven Central 429). 로그: captures/location-consent/build-first.log. 그래서 assembleDebug·전체 단위 테스트·lint는 이 환경에서 못 돌림.
  - 순수 함수 테스트: kotlinc 2.2.0 + JUnit 4.13.2로 `LocationConsent.kt`, `LocationFallback.kt`, `Models.kt`와 `LocationPolicyTest`, `LocationFallbackTest`만 따로 컴파일해 실행 → `OK (8 tests)`(새 5개, 기존 3개). 로그: captures/location-consent/pure-tests.log.
  - 바뀐 Android 파일은 SDK 없이 kotlinc로 돌려 문법 오류 0건, 새 기호(LocationPolicy·LocationConsentStore·syncLocation·askInOwnTask·inflateLocationConsent)는 모두 해석됨을 확인. Android·SDK·R·뷰 바인딩 미해석 오류는 이 환경 한계라 실제 컴파일 확인은 사용자 PC에서 필요.
- 사용자 PC에서 확인할 것:
  1. `.\gradlew.bat testDebugUnitTest assembleDebug lintDebug -q` 통과(테스트 수 70 → 75 예상).
  2. 새로 설치: 첫 권한 창에 위치 권한이 없고(속도·알림만) → 차종 선택 → 배치 → 「시작하기」 → 위치 동의 화면이 나오는지, 안내 문구 확인.
  3. 「경로 기록하기」: 별도 작업 창 권한 창에서 위치 권한 요청 → 허용 후 리그로. 주행 1회 뒤 리플레이에 실제 경로가 그려지는지, Logcat `Location source: FusedLocation`(또는 대체 출처).
  4. 3번 직후 주행 중 Pleos 홈 화면이 「주행 중에는 사용할 수 없습니다」로 앱을 가리지 않는지(해결 1 재발 여부).
  5. 「기록하지 않기」: 권한 창 없이 리그로. 주행 1회 → 점수·순위 정상, 리플레이 경로 비어 있음, Logcat `Location: 경로 기록 동의 안 함`.
  6. 앱을 종료·재시작해도 동의 화면이 다시 안 나오는지(동의 유지).
  7. 리그 → 차종 탭 → 「경로 기록 설정」 → 현재 상태 문구 표시 → 바꾸기가 다음 트립부터 반영되는지.
  8. 주행 중(기어 D·속도)에는 동의 화면이 안 뜨고, 동의 화면을 보던 중 주행이 시작되면 주행 중 화면으로 바뀌는지.
  9. 시연 모드: 92점·64점·골드 리그 397명 중 P125 화면과 각본 주행이 그대로인지.

### 2026-11-05 — 첫 클라우드 작업 다섯 개 병합 · 점검 보고서의 시연·화면 문제 수정
- 병합(Claude): main에서 지역·친구 탭(`cool-mayer-7magfk`) → 주간 리포트(`jolly-cerf-oyw9e0`) → 시즌 티어 이동(`quirky-fermi-ipjac7`) → 위치 동의(`ecstatic-einstein-2bi94b`) → 점검 보고서(`nifty-heisenberg-ec1z4y`) 순서로 병합. 충돌은 이 파일에서만 났고 양쪽 기록을 모두 남김.
- 병합 결과를 코드로 따라가 본 것
  - 첫 실행: 권한 창(별도 작업 창, 위치는 아직 안 물음) → 차종 선택(자동 인식이면 바로 배치) → 배치 → 「시작」에서 위치 동의 → 동의하면 위치 권한을 다시 별도 작업 창으로 → MainActivity. 시연 모드에서는 동의 화면을 건너뜀. 순서대로 이어짐.
  - 정산 직후 첫 화면: 달이 바뀌어 정산하면 지난 트립 카드 대신 리그를 열고, 새 시즌 첫 주행 전까지 결과 안내 한 줄을 보여 줌. 남은 점: 위치 동의를 아직 안 했으면 동의 화면이 먼저 뜨고, 동의 뒤에는 지난 시즌의 마지막 트립 카드가 열림(드묾, 이번에는 안 고침).
  - 실제/시연 분기: `!demoMode && demoResultWin == null`이면 실제. 리그·지역·친구·리포트 모두 같은 조건. 시연 각본 주행 결과는 `lastResultWasDemo`로 92/64 카드.
- 고친 것
  - C1: 시연 92점 카드에서 연 리플레이는 그날(11-09) 리플레이(`DemoData.winDay*`: 8:02 ~ 8:31 · 18.4km, 감점 사건 없음)를 보여 줌. 「다른 날의 예시」 표시는 64점 리플레이에만. 사건이 없으면 목록에 「감점 사건 없이 달렸어요」.
  - C2: 시연 기준일을 `DemoData.today = 2026-11-09` 하나로 통일. 11-07(92점)과 11-09(93점)을 맞바꿔 92점 트립이 11-09가 됨(18.4km도 옮김). 11-09 시즌 평균 88.8·P125·승급 6계단·0.9점, 리포트 88.8·지난주 85.7·P131 → P125 ▲6은 그대로.
    - 92점 카드: P125, 11-08 순위를 126 → 128로 바꿔 ▲3 유지. 추월한 세 명(P126 해운대드라이버 88.7 · P127 주말여행러 88.6 · P128 퇴근길여유 88.5)은 11-08 내 평균 88.3보다 높고 오늘 88.8보다 낮음. 연속 기록은 11-09 기준이라 13일 → 15일. 11-07 순위는 125 → 126(11-07 평균이 88.5 → 88.7로 바뀌어 자연스럽게).
    - 순위표는 `DemoData.board` 하나(11-09)로 합치고 날짜별 `rawBoard`를 지움(부드러운출근길 P123 89.1 → P2 95.8 문제도 사라짐). 리그 화면은 전처럼 1·2위, 승급 라인 앞뒤, 내 앞뒤만 보이고 92점 카드는 P123~P128을 보여 줌. 같은 별명은 어느 화면에서나 같은 평균.
    - 64점 날(P126 ▼1 유지): 보고서 제안(11-10에 64점)으로는 평균이 86.0이 되어 ▼1이 수학적으로 불가능. 그래서 「시즌 후반의 예시」로 둠: 이 트립 전까지 49트립 평균 88.8로 P125였다가 64점을 받아 (88.8×49+64)/50 = 88.3, 해운대드라이버(88.7)에게만 밀려 P126 ▼1. 내 평균은 `lowDayAvgAfter`로 계산함.
    - 64점 내역을 `DemoData.lowDayDetail`(25/0/14/15/10)로 두고, 피트 보드 하드코딩(+9/+8/+3)을 없앰. 시연 64점 카드는 「매너 +25점 가능」. 실제 트립에 세부 점수가 없으면 숫자 없이 둠(전에는 시연 숫자 9가 나왔음).
  - C4: `SeasonDrivers.drivers`(실제 모드 가상 운전자 396명)가 시연 순위표 줄을 그대로 지나고 사이는 직선, 맨 아래 62점. 실제 모드에서 평균 88.8이면 시연과 같이 P125·6계단·0.9점. 92점 하나 → P77, 80점 → P214, 64점 → P377(전에는 92·88.5점이 P1). 승급선 P119 89.7, 강등선 P279 73.6. 지역·친구 목록은 그대로.
  - H2: 운전 제한이 풀렸을 때 `latestCompletedTrip`(마지막으로 저장된 트립)을 열지 않음. 제한 중에 끝난 이번 트립(`pendingResult`)이 있으면 그 결과, 트립이 아직 진행 중이면 주행 중 화면 유지, 아니면 리그. 제한 중에 결과 알림으로 앱이 열려도 같은 방식으로 미뤘다가 엶.
  - M4: 시연 리포트 범위(11.03 ~ 11.09)는 그대로 두고 막대 요일을 날짜에서 계산(화 수 목 금 토 일 월, 휴식일 11-05는 「목」). 실제 모드는 월~일 그대로.
  - M5: CarUxRestrictions 연결이 실패하거나 매니저를 못 얻으면 주행 중으로 보고 주행 중 화면을 띄움. 그 뒤로는 기어·속도 신호(`isDrivingByMotion`): P단이고 0.3 m/s 미만으로 확인될 때만 풀고, 기어를 모르면 주행 중.
  - L2: 작은 리그 순위선. 승급선 최소 P1은 이미 지역·친구 탭 작업에 있었고, 강등선이 승급선 이하가 되지 않게 막음. 인원 1~6명 경계 테스트 추가.
- 가정(모호해서 단순한 쪽으로 고름)
  - 시연 리포트는 기준일까지 7일(11.03 화 ~ 11.09 월). 제목 「11월 2주차 리포트」는 그대로 둠. 범위를 11.02 ~ 11.08로 옮기면 머리 숫자(P131 → P125 ▲6, 88.8)가 바뀌고 리그 화면과 날짜가 어긋나서.
  - 92점 내역은 30/25/12/15/10(전비 리그 평균의 104%, 깜빡이 켠 차선 변경 1회)이라고 보고 테스트로만 고정함. 화면에는 내역 칸이 없음.
  - 실제 모드 기본 티어는 브론즈 그대로. 분포는 모든 티어가 같음(골드 시연 분포).
  - M5 대체 판단 신호는 날씨용으로 이미 받는 기어·속도(`VehicleWeatherSource`)를 씀. 그 신호도 못 받으면 주행 중 화면에 머묾(안전한 쪽).
  - `mockups/demo-data.js`는 고치지 않음. `DemoData` 주석에 다른 점을 적음.
- 손대지 않은 것: `core/collector/`, `ScoreCalculator.kt`(다른 세션). C3(각본 신호로 계산하면 92/64가 안 나옴), H1·H3·H4·M1~M3·M6·M7, L1·L3~L7. 주행 중 화면의 「11.2 / 18.4km · 해운대까지」는 64점 각본에서도 같음(C3과 함께 볼 것).
- 검증
  - Android 빌드는 못 함: 클라우드에서 Google Maven(dl.google.com)이 403이라 Android Gradle 플러그인을 받지 못함. **`MainActivity.kt`는 컴파일하지 못했음.** 레이아웃 id(바인딩 이름 → `@+id`)와 import는 스크립트와 눈으로 대조함: 모르는 id 없음, 새로 쓰는 `isDrivingByMotion`은 기존 `weather.*` import로 들어옴.
  - 순수 코드: 저장소 밖 JVM Gradle 프로젝트(커밋 안 함)로 `core/calculator`, `core/model`, `core/data`, `weather/WeatherState.kt`, `core/collector`의 순수 파일 4개, `ui/view`의 순수 파일 2개와 저장소의 단위 테스트 13개 클래스를 모두 돌림. 113개 통과(DemoData 16, SeasonRanking 12, WeatherState 15, TierMovement 10, WeeklyReport 13, TripCollector 18 등).
  - 새 테스트: 92점 카드·리그·리포트 기준일과 숫자 일치, 별명별 평균 하나, 64점 날 평균·순위(▼1) 계산, 64점 내역 = ScoreCalculator 결과, 92점 내역, 92점 리플레이 데이터, 실제 분포가 시연 순위표를 지남, 시연 리포트 요일, 작은 리그 순위선, 연결 실패 시 주행 판단.
- 사용자 PC·에뮬레이터에서 확인할 것
  1. `.\gradlew.bat testDebugUnitTest assembleDebug -q` 통과(특히 `MainActivity` 컴파일).
  2. 시연 「이긴 날」: 92점, 18.4km, 연속 15일, P125 ▲3, P126~P128에 「추월」 태그, 「0.3점 차」 문구, 승급 라인까지 6계단.
  3. 시연 「이긴 날」 → 리플레이: 8:02 ~ 8:31 · 18.4km, 「다른 날의 예시」 없음, 「감점 사건 없이 달렸어요」, 돌아가기 → 92점 카드.
  4. 시연 「아쉬운 날」: 64점, 22.1km, 13일 → 0일, P126 ▼1, 해운대드라이버 「목표」·「0.4점 차」, 피트 보드 「깜빡이!」·「매너 +25점 가능」. 리플레이는 6:08 ~ 6:42 · 22.1km와 「다른 날의 예시」.
  5. 시연 리그: P125, 내 평균 88.8, 승급 라인까지 6계단 · 0.9점, 줄은 P1·P2·P118·P119·P124·P125·P126(전과 같음), 21일 남음.
  6. 시연 리포트: 11.03 ~ 11.09 · 주행 6일, 막대 요일이 「화 수 목 금 토 일 월」이고 「목」이 휴식, 가장 높은 막대(93, 토)만 노랑, P131 → P125 ▲6, 88.8, +3.1.
  7. 실제 모드 리그: 트립 하나 평균에 따라 순위가 시연과 비슷한 자리(88.8 → P125, 92 → P77 근처). 리포트 차트의 넓은 순위 폭 눈금이 겹치지 않는지.
  8. H2: 주행 중 D단으로 멈춰 제한이 풀리는 설정이면(에뮬레이터에서 속도 0·기어 D), 이전 트립 카드가 아니라 주행 중 화면이 남는지. P로 바꾸면 이번 트립 카드가 뜨는지. 3km 미만이면 리그로.
  9. M5: 정상 에뮬레이터에서는 동작 변화 없음(연결 성공). 가능하면 CarUxRestrictions 서비스가 없는 이미지에서 처음엔 주행 중 화면, 기어 P·속도 0이면 풀리는지.
  10. 친구 탭(6명)·지역 탭에서 순위가 정상이고 앱이 죽지 않는지.
  11. main 병합 여부 결정.

### 2026-11-05 — 점수 계산 수정 (점검 보고서 H1·H3·H4·M1·M2·M3·M7·C3)
- 기준: `docs/spec-audit-2026-11-05.md`(브랜치 `claude/nifty-heisenberg-ec1z4y`). 항목마다 재현 값으로 단위 테스트를 먼저 쓰고, 지금 동작을 옮긴 임시 구현으로 실패를 확인한 다음 고쳤다(새 테스트 18개 중 15개 실패 → 수정 후 0개).
- H1: 트립 점수를 정수로 반올림해 저장한다(`ScoreCalculator.tripScore`). 결과 카드의 표시(`roundToInt`), 이긴 날 판정(`>= 70`), 연속 기록(`streakAt`)이 같은 값을 쓴다. 69.8 → 70점 → 연속 유지, 69.4 → 69점 → 끊김. 항목별 내역은 소수 첫째 자리 그대로다.
- H3: 에코는 트립 평균 전비로 매긴다. 트립 평균 = 총 거리 ÷ 총 소모량이다. 순간값은 다음 값이 올 때까지 이어진다고 보고, 속도 적분 거리로 소모량(거리 ÷ 순간값)을 쌓는다. 0 이하·NaN은 버린다. 7.0으로 10km, 주차 직전 3.0 → 에코 20(이전 0).
- H4: 트립을 시작한 속도 표본을 가속도 기준점으로 쓴다. 0.6 → 2.0 m/s(0.5초)는 2.8 m/s²로 계산되어 급가속이 아니다(이전 4.0).
- M1: 경고등 상태를 트립 밖에서도 기억한다. 출발 전부터 켜져 있으면 출발 시각부터 잰다. 출발 전에 꺼진 경고는 감점하지 않는다.
- M2: `VehicleCatalog.averageEfficiency`에 차종별 가상 기준표를 두었다. 실제 리그 평균 데이터가 아니다. 표에 없는 차종이나 동력 방식이면 EV 5.8 / 내연 12.0을 쓴다. 아이오닉 5 EV는 시연 기준과 같은 5.8이다.
- M3: 급제동(급감속)은 페달 신호 없이 속도 변화(−3.5 m/s² 미만)로 판정한다. 회생제동 감속도 포함된다. `onBrakePedalChanged`는 남겨 두었지만 쓰지 않는다. 실차 원페달 최대 감속도가 기준을 넘는지(오탐 여부)는 확인하지 못했다.
- M7: 피트 보드 선택을 순수 함수 `core/calculator/PitBoard.kt`로 옮겼다. 만점 대비 잃은 점수가 가장 큰 항목을 고른다. 부드러움은 급가속·급제동 몫으로 나눈다. 안전 기본기·에코도 후보다. `MainActivity`는 실제 트립에서만 이 함수를 쓴다(3줄 변경). 시연(`DemoData`) 피트 보드는 다른 세션 범위라 그대로 두었다.
- C3: `MockVehicleDataSource` 각본을 조정했다. 가감속은 초당 2~2.5 m/s, 조향은 1.5초 유지, 전비 신호는 6.032 / 6.264다. `MockVehicleDataSourceTest` 결과: 이긴 날 30/25/12/15/10 = **92**, 아쉬운 날 25/0/14/15/10 = **64**(급가속 1·급제동 2·깜빡이 없는 차선 변경 4). 주석 "11/7 금"은 "토"로 고쳤다(L5).
- 가정(사용자 확인 불가, 가장 단순한 쪽을 고름)
  - H1은 버림 표시 대신 정수 저장을 골랐다. UI를 건드리지 않고, 시연 데이터의 정수 점수와도 맞는다. 69.5는 70점(연속 유지)이다. 이 변경 전에 저장된 소수 점수 트립(에뮬레이터의 43.3·73.3 등)은 그대로라 69.5~69.9였다면 표시·판정 불일치가 남는다.
  - H3은 거리 가중 산술 평균이 아니라 에너지 기준 평균(조화 평균)이다. 이것이 트립 컴퓨터의 평균 전비와 같은 정의다. 순간값 뒤로 달린 거리가 없으면 마지막 순간값을 쓴다.
  - M7에서 꾸준함(연속 기록·거리)은 이번 트립의 습관으로 고칠 수 없어 후보에서 뺐다. 안전 기본기는 벨트·경고·도어를 점수로 구분할 수 없어 문구를 "출발 전 벨트, 주행 중 경고등 바로 확인하기"로 묶었다. 잃은 점수가 0.5점 미만이면 "좋아요!"를 보여 주고 감점 문구는 없다. 새 글자(기본기!·에코!·좋아요!)가 보드 폭에 맞는지는 에뮬레이터에서 확인이 필요하다.
  - C3의 92/64는 아이오닉 5 기준 5.8과 각본 전 연속 기록 10일 이상(테스트는 12일·13일)일 때 값이다. 앱의 각본 주행은 저장소의 실제 연속 기록과 차종 프로필 기준을 쓴다. 그래서 새 설치(연속 0일)에서는 계산값이 7점 낮다. 결과 카드는 지금처럼 `DemoData`의 92/64를 보여 준다.
- 바꾸지 않음: 배점 30/25/20/15/10, 10km당 환산, 3km 미만 제외, 연속 70점 기준, `DemoData.kt`, 서비스.
- 검증: Android Gradle 플러그인을 받을 수 없어(dl.google.com 403) 앱 빌드와 `testDebugUnitTest`는 실행하지 못했다. 저장소 밖의 JVM Gradle 프로젝트(Kotlin 2.1.0, JUnit 4.13.2, kotlinx-coroutines-core 1.8.1, 커밋 안 함)로 `core/`(Android 의존 4개 파일 제외)와 `app/src/test/.../core/` 테스트를 돌렸다. 결과: 69개, 실패 0, 오류 0, 건너뜀 0(수정 전 기존 51개 통과 확인). `ui/view`·`weather` 테스트는 이 프로젝트에 넣지 않았다.
- 컴파일하지 못한 코드: `ui/MainActivity.kt`의 3줄 변경(피트 보드 연결)은 Android 의존이라 컴파일하지 못했다. 에뮬레이터 빌드에서 확인이 필요하다.
- 사용자가 결정할 것: main 병합(에뮬레이터에서 결과 카드·피트 보드·각본 주행 확인 후).

### 2026-11-05 — 2차 클라우드 결과 병합(통합·시연 + 점수 계산) · 남은 문제 마무리
- 병합(Claude): `claude/dazzling-goodall-e83ju7`(통합·시연·화면 수정)에서 이 브랜치를 만들고 `claude/vigilant-cori-5dzlwm`(점수 계산 수정)을 병합. 충돌은 `ui/MainActivity.kt` 2곳(피트 보드)과 이 파일. 이 파일은 양쪽 기록을 모두 남김(위 두 항목).
- 피트 보드 충돌 정리: `pitBoard(events, detail, demo)` 하나로 합침.
  - 실제 트립이고 세부 점수가 있으면 `core/calculator/PitBoard.pick` 결과(글자·문구·이유·「+N점 가능」).
  - 시연 64점 카드(실제 트립 없음)는 `DemoData.lowDayDetail`(25/0/14/15/10)로 「깜빡이!」·「매너 +25점 가능」.
  - 세부 점수가 없는 옛 실제 트립은 사건 횟수로 문구만 고르고 숫자는 비움. 실제 트립 경로에서 `DemoData` 값을 쓰는 곳은 없음(코드로 확인).
  - 리플레이 하단 문구도 같은 함수(`demo = t == null`)를 씀. 리플레이에는 사건별 감점 숫자가 없음(문구만).
- 두 브랜치의 전제 대조
  - 같음: 각본 주행 92점(30/25/12/15/10, 18.4km, 사건 0)과 64점(25/0/14/15/10, 22.1km, 급가속 1·급제동 2·깜빡이 없는 차선 변경 4). 리그 평균 전비 5.8.
  - 테스트로 고정: `DemoData.winDayDetail`(30/25/12/15/10)을 새로 두고, `MockVehicleDataSourceTest.scriptsMatchDemoCards`가 각본 결과를 `DemoData`의 내역·점수·거리·사건 종류와 비교. 연속 기록 입력도 `DemoData`에서 가져옴(이긴 날 = 기준일 전날까지 12일, 아쉬운 날 = `lowDayStreakBefore` 13일). `DemoDataTest`의 92점 계산도 `winDayDetail`과 비교.
  - 남은 차이(이전 기록과 같음): 앱의 각본 주행은 저장소의 실제 연속 기록과 차종 프로필 기준을 쓰므로 새 설치(연속 0일)에서는 계산값이 7점 낮다. 결과 카드는 `DemoData`의 92/64를 보여 준다.
- 92점 카드 연속 기록: 시안 `mockups/result-win.html`은 `streakAt('2026-11-07')` = **13일**. 통합 브랜치에서 92점 날을 11-09로 옮기며 15일이 됐음. **13일로 되돌림.** `DemoData.trips`의 10-26 점수를 81 → 68로 바꿔 11-09 기준 연속이 10-27부터 13일이 되게 함.
  - 10-26은 10월(지난 시즌)이고 지난주(10-27 ~ 11-02, 85.7) 밖이라 92점(P125 ▲3), 64점(P126 ▼1), 골드 397명 중 P125, 시즌 평균 88.8, 리포트 88.8·+3.1·P131 → P125 ▲6은 그대로. 테스트로 확인.
  - 바뀌는 값: 화면에 안 나오는 날짜별 연속(11-05 9일, 11-06 10일, 11-07 11일, 11-08 12일). 92점 계산의 연속 점수는 10일 이상이면 만점이라 그대로.
  - 시안 파일은 고치지 않음. 시안 `demo-data.js`의 10-26은 81 그대로라 이 줄은 시안과 다름(`DemoData` 주석에 적음). 64점 카드 「13일 → 0일」은 시안과 같음.
- M5 대체 판단 따라가기(코드만, 에뮬레이터 실행 못 함)
  - 정상 에뮬레이터: `Car.createCar`와 `CarUxRestrictionsManager`를 얻으면 `uxFallback = false`라 기어·속도 판단은 쓰지 않음. P단 정지면 UX 제한 서비스가 「최적화 불필요」를 주므로 주행 중으로 고정되지 않음.
  - 연결 실패(대체 판단) 직후: `onStart`에서 `connectUxRestrictions()`가 `weatherSource.start()`보다 먼저 돌아 기어가 UNKNOWN → 주행 중 화면. 이어서 `start()`가 `getProperty`로 현재 값을 바로 읽음(속도 → 기어 순). 기어 P를 받으면 `isDrivingByMotion` = false라 풀림. 속도 권한(`CAR_SPEED`)이 아직 없어 속도가 null이어도 P단이면 풀림. 기어는 `CAR_POWERTRAIN`(일반 권한)이라 첫 설치에도 들어옴.
  - 풀린 뒤 화면: 진행 중 트립이 없으면 리그(차종을 안 골랐으면 차종 선택). 그래서 대체 판단 환경에서는 앱을 열 때 `onCreate`가 연 화면(위치 동의, 결과 카드 등) 대신 리그가 뜬다. 정상 에뮬레이터에서는 생기지 않음. 고치지 않음(H2의 「풀리면 리그」 결정을 건드리게 되어서).
  - 고정될 수 있는 경우 1: 대체 판단 환경에서 `GEAR_SELECTION`도 못 받으면 계속 주행 중 화면(의도한 안전한 쪽).
  - 고정될 수 있는 경우 2: 속도 속성이 오류 이벤트를 내면 `VehicleWeatherSource.clear`가 기어도 UNKNOWN으로 지움. 기어는 바뀔 때만 이벤트가 오므로 P단에 서 있는 동안 다시 풀리지 않음(앱을 다시 열면 풀림). 대체 판단 환경에서만 생기고 날씨 모듈 동작이라 고치지 않음.
- 낮음 항목(점검 보고서 L1~L7)
  - L1 고침: 3km 미만 트립 거리를 버림으로 저장(2.96km → 「2.9km」). 3km 이상은 전처럼 반올림. `TripCollector.finishTrip`.
  - L2: 이미 고쳐져 있음(통합 브랜치). 경계 테스트 있음.
  - L3 고침: 아쉬운 날 카드의 「전 → 후」 연속 기록을 둘 다 모든 리그 트립으로 셈(전에는 「전」만 현재 리그). 연속 기록은 사람 기준.
  - L4 고치지 않음: 주행 중 도어 열림 −3은 안전 기본기(15점) 안의 감점이라 배점은 그대로다. 빼면 점수 동작이 바뀌고, 기획서는 저장소 밖이라 여기서 고칠 수 없음. 사용자가 기획서에 「도어 열림 −3」을 추가할지 정하면 됨.
  - L5: 이미 고쳐져 있음(점수 계산 브랜치).
  - L6 고침: 결과 카드 연속 기록을 오늘이 아니라 트립 날짜 기준으로 셈(이긴 날·아쉬운 날 모두). 쓰이지 않게 된 `MainActivity.today`를 지움.
  - L7 고침: 승급까지 계단이 남았는데 점수 차가 0.0으로 반올림되면 0.1점으로 보여 줌(`SeasonRanking.rank`). 승급권 안이면 0점 그대로.
- 「테스트가 없는 점수·순위 로직」 채운 것
  - 이미 다른 작업이 채운 것: H1·H3·M1·H4·회생 감속·효율 신호 여러 번(TripCollectorTest), C3(MockVehicleDataSourceTest), DemoData 일관성(DemoDataTest), 피트 보드 선택(PitBoardTest), 작은 리그 순위선(SeasonRankingTest).
  - 새로: `LeagueCalculatorTest`(1~6명·397명 순위선, 706.8/8 → 88.4와 353/4 → 88.3 반올림, `daysLeft` 마지막 날 0일, 하루 여러 트립 연속 기록, 3km 경계), `SeasonRankingTest` 2개(중간 순위 P125의 6계단·0.9점, L7 동점), `TripCollectorTest` 3개(L1 2.96km, 3.06km는 3.1km, 자정을 넘긴 트립은 출발한 날), `TripQueriesTest` 4개(리그·거리 필터, 리그 없는 트립만 배정, 연속 기록 범위 L3, 짧은 트립은 연속 기록에 영향 없음).
  - `TripRepository`는 파일과 `org.json`을 써서 Android 단위 테스트에서 바로 돌릴 수 없음. 판단 부분만 순수 함수 `core/repository/TripQueries.kt`로 옮기고(`getAllTrips`·`assignUnassignedTrips`·`getStreak`가 그대로 부름) 그것을 테스트함. JSON 저장·읽기는 테스트하지 않음.
  - 남은 공백: 결과 카드 이긴 날/아쉬운 날 판정(`showResult`의 `>= 70`)은 Activity 안에 있어 테스트하지 않음(점수가 정수 저장이라 경계 문제는 H1 테스트로 막힘).
- M6(코드 변경 없음) 부팅 자동 시작 판단 재료
  - 지금: 수집 서비스는 `MainActivity.onCreate`와 위치 동의 뒤에만 시작한다. 매니페스트에 부팅 수신기와 `RECEIVE_BOOT_COMPLETED`가 없다. `START_STICKY`라 프로세스가 죽으면 다시 살아나지만, 헤드유닛을 재부팅하면 앱을 열 때까지 기록하지 않는다.
  - 시연에는 영향 없음: 시연은 앱을 열고 시작한다. 실제 사용자가 시동 후 앱을 열지 않고 달리는 경우에만 트립이 빠진다.
  - 추가할 때 볼 것(추정 포함, 확인 필요)
    - AAOS 차량은 시동을 끌 때 전원을 끄지 않고 대기(suspend)로 들어가는 경우가 많다. 그러면 시동마다 `BOOT_COMPLETED`가 오지 않고, 서비스가 살아 있으면 필요도 없다(추정). Pleos 헤드유닛이 어느 쪽인지 모른다.
    - AAOS는 시스템 사용자와 운전자 사용자(보통 user 10)가 나뉜다. `BOOT_COMPLETED`는 운전자 사용자가 잠금 해제된 뒤 그 사용자에게 온다(추정).
    - 부팅 직후(백그라운드)에 시작하는 포그라운드 서비스는 위치 「사용 중 허용」 권한을 쓰지 못한다. 그래서 `location` 타입으로 시작하면 실패할 수 있고, 백그라운드 위치 권한을 따로 받거나 경로 없이 `dataSync`만으로 시작해야 한다. targetSdk 35부터는 `BOOT_COMPLETED`에서 `dataSync` 타입 시작도 막힌다(지금은 34).
    - 차량 시동 신호(`IGNITION_STATE`)로 시작하려 해도 그 신호를 받을 프로세스가 먼저 살아 있어야 한다.
    - 개인정보: 앱을 열지 않아도 기록한다는 것을 위치 동의 화면이나 알림에 알려야 한다.
  - 판단에 필요한 실험(사용자 PC): 에뮬레이터를 `adb reboot`한 뒤 앱을 열지 않고 Virtual Console로 주행 → `adb shell dumpsys activity services io.github.ddayoung03.safetyleague`에 서비스가 있는지, `trips.json`이 늘었는지. 에뮬레이터 「전원 끄기/켜기」(suspend)를 쓸 수 있으면 같은 실험.
  - 제안: 코드 동결(11-13)까지는 넣지 않고 「시동 후 앱 열기」를 안내. 넣는다면 `RECEIVE_BOOT_COMPLETED` + 수신기 하나 + 경로 없이 시작이 가장 단순.
- 가정(모호해서 가장 단순한 쪽을 고름)
  - 92점 카드 연속 13일은 「보이지 않는 10월 점수 하나를 70점 미만으로」로 맞춤. 카드만 다른 날짜로 세는 방법은 기준일을 하나로 맞춘 C2와 어긋나서 고르지 않음.
  - L1은 판정을 바꾸지 않고 3km 미만 표시만 버림. 3km 이상 반올림은 그대로(각본 18.4/22.1 유지).
  - L6은 트립 날짜까지의 연속 기록. 같은 날 그 트립 뒤에 달린 트립도 들어감(지난 카드를 다시 열 때만 차이).
  - L7은 「0.1점」 쪽을 고름(문구를 새로 만들지 않음).
- 바꾸지 않음: 배점 30/25/20/15/10, 10km당 환산, 3km 미만 제외, 연속 70점 기준, 시연 머리 숫자 92점(P125 ▲3)·64점(P126 ▼1)·골드 397명 중 P125, `mockups/`.
- 검증(클라우드)
  - Android 앱 빌드는 못 함: Android Gradle 플러그인을 받지 못함(dl.google.com 403). 저장소 밖 JVM Gradle 프로젝트 두 개(커밋 안 함, Kotlin 2.1.0, JUnit 4.13.2, kotlinx-coroutines-core 1.8.1)를 씀.
  - 단위 테스트: 순수 파일(`core/calculator`·`model`·`data`, `core/collector`의 순수 5개, `core/repository/TripQueries.kt`, `weather/WeatherState.kt`, `ui/view`의 순수 2개)과 저장소의 테스트 17개 클래스 전체 → **146개 통과, 실패 0**(병합 직후 131개 통과 → 새 15개).
  - Android 의존 컴파일: Maven Central의 `org.robolectric:android-all:14-robolectric-10818077`(android.car 포함)을 compileOnly로 두고 컴파일 성공: `core/repository/`(TripRepository·SeasonSettlementStore·VehicleProfileStore·LocationConsentStore), `SafetyLeagueApp.kt`, `ui/view/DesignCanvas.kt`, `ui/view/KerbView.kt`, `core/collector/VehicleProfileReader.kt`.
  - **컴파일하지 못한 파일**(AndroidX·Pleos SDK·R·뷰 바인딩이 필요): `ui/MainActivity.kt`, `ui/PermissionActivity.kt`, `ui/LocationConsentScreen.kt`, `service/TripTrackingService.kt`, `weather/VehicleWeatherSource.kt`, `core/collector/PleosVehicleDataSource.kt`, `ui/view/`의 나머지(CheckerFlagView, GlassDrawable, RainScoreView, CrewFaceView, DrivingTrackView, WeatherAmbientView, PositionChartView, CircuitRouteView, AsphaltDrawable, MetalTextView, SkewDrawable, FlameScoreView, ReplayEventIcon).
  - 직접 대조: `ui/`의 바인딩 필드(`b.*`, `binding.*`)를 레이아웃 `@+id` 전체와 스크립트로 대조 → 없는 id 0개. 이번에 바꾼 `MainActivity` 줄이 부르는 것(`PitBoard.pick`, `Advice.word/tip/reason/gain`, `repository.getAllTrips()`, `Trip.date`, `tripsBefore`의 새 인자)은 모두 있음. 지운 `today`는 다른 곳에서 안 씀.

## 사용자 PC 확인 목록 (이 브랜치 기준, 위 항목들의 확인 목록을 한 곳에 모음)
1. 빌드: `.\gradlew.bat assembleDebug lintDebug -q` 통과. 특히 클라우드에서 컴파일하지 못한 `MainActivity.kt`·`PermissionActivity.kt`·`LocationConsentScreen.kt`·`TripTrackingService.kt`·`VehicleWeatherSource.kt`·`PleosVehicleDataSource.kt`.
2. 단위 테스트: `.\gradlew.bat testDebugUnitTest -q` 통과. 저장소의 테스트 17개 클래스를 클라우드에서 모두 돌렸으므로 PC에서도 146개여야 함(새 클래스 `LeagueCalculatorTest`, `TripQueriesTest`).
3. 첫 설치 흐름: 앱 삭제 후 설치 → 권한 창(별도 작업 창, 속도·알림만, 위치 없음) → 차종 선택(자동 인식이면 바로 배치) → 배치 → 「시작하기」 → 위치 동의 → 동의하면 위치 권한 창 → 리그. 주행 중 Pleos 홈 화면이 「주행 중에는 사용할 수 없습니다」로 앱을 가리지 않는지.
4. 세 리그 탭: 차종(기본)·지역·친구 탭 전환, 친구 6명·지역 순위 정상, 지역·친구 탭에서 티어 바·승급 문구 숨김, 차종 탭을 다시 누르면 차종 선택, 앱이 죽지 않음.
5. 리포트: 시연 11.03 ~ 11.09 · 주행 6일, 막대 「화 수 목 금 토 일 월」(「목」 휴식, 93 토만 노랑), 88.8, +3.1, P131 → P125 ▲6. 실제 모드 리그 → 「리포트」 → 「리그」 왕복, 트립 없을 때 빈 상태, 주행 뒤 값이 트립과 맞음.
6. 위치 동의: 「경로 기록하기」 → 주행 뒤 리플레이에 경로, 「기록하지 않기」 → 경로 없음·점수 정상, 재시작 후 유지, 차종 탭 「경로 기록 설정」에서 바꾸기, 주행 중에는 동의 화면이 안 뜸.
7. 시연 92점: 92점, 18.4km, **연속 13일**(시안과 같음), P125 ▲3, P126~P128 「추월」, 「0.3점 차」, 승급 라인까지 6계단. 리플레이 8:02 ~ 8:31 · 18.4km, 「감점 사건 없이 달렸어요」, 「다른 날의 예시」 없음.
8. 시연 64점: 64점, 22.1km, 13일 → 0일, P126 ▼1, 해운대드라이버 「목표」·「0.4점 차」, 피트 보드 「깜빡이!」·「차선 바꿀 때 깜빡이부터 켜기」·「오늘 깜빡이 없이 차선 변경 4회」·「매너 +25점 가능」. 리플레이 6:08 ~ 6:42 · 22.1km와 「다른 날의 예시」. 빨강 없음.
9. 시연 리그: 골드 397명 중 P125, 88.8, 승급 라인까지 6계단 · 0.9점, 21일 남음.
10. 각본 주행: 시연 모드에서 이긴 날·아쉬운 날 각본 → 주행 중 화면에 점수·순위·감점 없음 → 끝나면 92/64 카드. 각본 뒤 `trips.json`이 늘지 않음.
11. 실제 주행 복귀: 시연을 끄고 Virtual Console로 3km 이상 주행 → P → 이번 트립 카드(시연 숫자 아님). 70점 미만이면 피트 보드가 이 트립의 가장 큰 감점 항목(새 글자 「기본기!」·「에코!」·「좋아요!」가 보드 폭에 맞는지). 연속 기록이 트립 날짜 기준. 3km 미만이면 「이번 주행 2.9km」처럼 3.0으로 보이지 않음. 리그 순위·리포트 반영, 재시작 후 유지.
12. 주행 중 제한: 주행 중에는 주행 중 화면만, P로 바꾸면 이번 트립 카드. D단 정지로 제한이 풀리는 설정이면 이전 트립 카드가 아니라 주행 중 화면 유지. 가능하면 CarUxRestrictions가 없는 이미지에서 P·정지 시 풀리는지(M5).
13. 시즌 정산: 에뮬레이터 날짜를 다음 달 1일로 → 리그 화면에 결과 한 줄, 재시작해도 두 번 정산 안 함.
14. M6 실험(선택): `adb reboot` 뒤 앱을 열지 않고 주행해 기록되는지(위 M6 항목).
15. main 병합 여부 결정.

## 2026-11-05 MainActivity 기능별 파일 분리 (claude/split-main-activity)

- 987줄이던 `ui/MainActivity.kt`를 336줄로 줄이고, 화면과 공용 코드를 `ui/` 아래 11개 새 파일로 옮겼다. 화면은 `internal fun MainActivity.showXxx()` 확장 함수이고, 상태는 `MainActivity`에 남겼다.
- 동작 변경 없음: 분리 전후 코드 줄을 전부 비교해 접근 제한자·import 외에는 구역 주석 8줄만 빠진 것을 확인했다.
- 확인: `assembleDebug`·`testDebugUnitTest` 통과(146개). 
- 에뮬레이터(Pleos_Connect, GPU host·cold boot): 실제 모드 결과 카드·리플레이·리그 3탭·리포트, 시연 92/64 카드·리플레이·리그·리포트·주행 중, 배치·차종·위치 동의 화면 통과. 숫자는 확인 목록과 일치, 앱 충돌 0건. 각본 주행 두 종 모두 결과 카드까지 도착했고 `trips.json`은 늘지 않음. 각본이 빨리 끝나 그 사이의 주행 중 화면은 못 봤다.
- Virtual Console 실제 주행 2회 통과: 주행 중 자동으로 주행 중 화면(점수·순위 없음, 시연 모드 진입 막힘), P단에서 이번 트립 카드 자동 표시. 1회 74점 4.4km(연속 2일, P285 → P280 ▲5), 2회 급제동 45점 4.9km(피트 보드 「천천히!」·급제동 4회, 2일 → 0일, P377 ▼97, 시즌 평균 64.0). `trips.json` 저장, 리그·리포트 반영, 앱 충돌 0건. 3km 미만 카드는 이번에 안 돌림.
- 분리와 무관하게 발견: 시연 리포트의 순위 차트 아래 요일이 월~일로 고정(`PositionChartView.kt:71`)이라 11.03(화)~11.09(월)인 막대 요일과 다르다.

## 2026-11-05 리포트 차트 요일·아쉬운 날 내 줄 수정 (claude/report-chart-low-row)

- 시연 리포트의 순위 차트 요일이 월~일 고정이라 막대(화~월)와 달랐다. `PositionChartView.days`를 바꿀 수 있게 하고 막대와 같은 목록을 넘긴다.
- 실제 아쉬운 날 카드의 내 줄이 노란색이었다. `realRows`가 줄 크기를 받게 하고 `rowLow`를 넘겨 시연 64점 카드처럼 흰색으로 맞췄다.
- 확인: `assembleDebug`·`testDebugUnitTest` 통과(146개), 에뮬레이터에서 두 화면 확인, 앱 충돌 0건.

## 2026-11-06 OS 위치 권한 거부 시 경로 기록 차단 (claude/location-permission-fix)

- OS 위치 권한을 거부해도 경로가 기록됐다. `LocationPolicy.sourceOrder`가 동의만 보고, Pleos `FusedLocation`은 OS 권한 없이도 값을 주기 때문이다. `sourceOrder`가 OS 권한도 받게 해 동의와 권한이 모두 있을 때만 위치 출처를 켠다.
- 동의했어도 OS 권한이 없으면 경로 기록 설정 화면에 「지금은 경로를 기록하지 않아요」로 표시한다(`effectiveConsent`).
- 리플레이: 경로가 비어 있으면 「경로를 기록하지 않았어요」(`routeNote`), 3km 미만 트립의 작전 문구는 「3km 이상 달리면 기록돼요」.
- 3km 미만 카드: 순위 머리글과 「다음 주행 70점이면 1일부터 다시」를 숨긴다.
- 확인: `assembleDebug`·`testDebugUnitTest` 통과(151개, 새 테스트 5개는 수정 전 실패 확인). 에뮬레이터에서 `adb emu geo fix`로 GPS를 움직이며 권한 거부 주행 → 로그 「경로 기록 동의 또는 OS 위치 권한 없음」, 경로 0점. 권한 복원 주행 → `FusedLocation`, 경로 51점. 일반 45점 카드 변화 없음, 앱 충돌 0건.
- 남은 한계: 권한 창에서 「다시 묻지 않음」을 고르면 앱에서 다시 물을 수 없어 시스템 설정에서 켜야 한다. 앱 안내는 없다.
