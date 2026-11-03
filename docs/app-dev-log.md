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
   - `assembleDebug` 및 `testDebugUnitTest` (15개 전체 통과) $\rightarrow$ [`app-debug.apk`](file:///C:/Users/PTY/Desktop/현대차앱개발공모전/safety-league/app/build/outputs/apk/debug/app-debug.apk) (18MB) 생성 완료.

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
