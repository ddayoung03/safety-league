# app 브랜치 검토 결과 (2026-11-03, Claude 검토단 3명)

대상: `git diff main..app` (작업 1~5). 단위 테스트 15개 통과는 직접 확인함.
고칠 때는 버그마다 **재현 테스트를 먼저** 쓰고 고친다. 다 고치면 `docs/app-dev-log.md`에 항목 번호와 함께 기록한다.

## 1순위 — 시연·심사에서 바로 문제가 되는 것

1. **실제 차량 신호가 하나도 안 들어온다.** `PleosVehicleDataSource.kt:50-55` `registerSdkListeners`가 주석뿐이고, SDK 초기화도 리플렉션으로 클래스 이름을 추측한다(실패해도 로그만 남김). FusedLocation·NaviHelper는 의존성만 있고 호출이 없다. 지금 앱은 `MockVehicleDataSource`로만 돈다.
   → Pleos Vehicle SDK를 정식 import로 호출하고, 기어·속도·브레이크·조향·방향지시등·안전벨트·경고등·도어·주행거리·전비/연비 리스너를 실제로 연결. FusedLocation으로 경로 기록. 에뮬레이터 + Virtual Console에서 신호가 들어오는 것을 로그로 확인.
2. **CarUxRestrictionsManager를 안 쓴다** (주석에만 있음). 주행 중에도 탭 바로 결과·리그 화면에 들어갈 수 있다.
   → 주행 중(제한 활성)에는 주행 중 화면만 보이게 하고 나머지 화면 진입 차단. 매니페스트 `distractionOptimized` 메타데이터는 주행 중 화면에만. `automotive_app_desc.xml`의 `<uses name="media"/>`는 이 앱에 맞지 않으니 확인 후 수정.
3. **주행 중 화면에 애니메이션과 버튼이 있다** (스펙 금지). `WeatherAmbientView`가 1초 무한 반복으로 햇살·비·눈 입자를 그리고, 날씨는 실제 데이터 없이 버튼 3개로 바꾸는 가짜 토글이다.
   → 주행 중 화면에서 WeatherAmbientView와 날씨 버튼 제거. 시안대로 정지 화면 + 질감만.
4. **P단 → 결과 카드가 동작하지 않는다.** 서비스에서 `startActivity`(Android 10+ 백그라운드 실행 제한에 막힘), `EXTRA_SHOW_RESULT_CARD`를 MainActivity가 읽지 않음(`onNewIntent`/`getBooleanExtra` 없음), `onCreate`는 항상 이긴 날 카드.
   → 앱이 앞에 있으면 액티비티에 직접 알림, 뒤에 있으면 full-screen intent 알림. 인텐트 extra를 읽어 그날 트립 결과를 표시.
5. **트립 거리 버그.** `TripCollector.kt:33,148-149` `startOdometerKm`이 한 번만 설정되고 트립 시작 때 초기화되지 않는다. 두 번째 트립부터 거리가 누적값으로 계산되어 3km 제외·10km당 환산이 모두 틀어진다. (재현: 100→118 트립 뒤 118→125 트립이 25km로 계산됨)
6. **위치 권한을 런타임에 요청하지 않는다.** API 34에서 `foregroundServiceType=location`으로 `startForeground`하면 SecurityException.
7. **시연용 컨트롤이 항상 보인다.** 상단 탭 바(6개)와 [이긴날 시연]/[아쉬운날 시연] 버튼이 심사 스크린샷에 찍힌다.
   → 디버그 플래그(또는 숨은 제스처) 뒤로. 버튼이 보일 때도 76dp·24sp 이상.

## 2순위 — 규칙·스펙과 어긋난 것

8. **가독성 미달.** 레이아웃·코드에 14/16/17/18/20/22sp 글씨 다수, 버튼 36~52dp. 기준: 글씨 24sp 이상, 대비 4.5:1, 누르는 영역 76dp 이상.
9. **아쉬운 날 화면에 빨강.** 리플레이 급제동 글자 `R.color.red`(`MainActivity.kt:296`), 서킷 급제동 마커 `#C8102E`(`CircuitRouteView.kt:138`). 시안대로 흰색. 강등 라인 글자 `#F07A80`, 상단 `#151719`는 토큰에 없는 색.
10. **글꼴 미포함.** `res/font` 없음 → 시스템 글꼴로 나옴. Black Han Sans, Saira Condensed(OFL)를 `res/font`에 넣고 적용.
11. **숫자 하드코딩.** 주행 중(11.2/18.4km, 13일째), 리그(P125, 397명, 21일 남음 등), 리포트(88.8, +3.1, 85.7, P131→P125 ▲6), 아쉬운 날(64, 13일→0일, P126 ▼1)이 문자열로 박혀 있다. `DemoData`/계산 함수에서 채운다.
12. **점수 계산이 스펙과 다름** (`ScoreCalculator.kt`).
    - 에코는 `efficiencyRatio=1.0` 고정이라 항상 만점(placeholder). 같은 차종 평균 대비로 계산하고, NaN 방어.
    - 꾸준함은 `5.0 + streak*0.5`라 연속 0일이어도 5점. 스펙의 "주행거리" 요소 없음.
    - EV/내연기관 분리 로직 없음.
    - 감점 계수(-3/-4/-5/-8/-2)에 근거 메모 없음 → 정한 값과 이유를 로그에 기록.
    - `TripTrackingService.kt:46-50` streak을 날짜 "2026-11-07"로 고정 → 실제 트립 기록에서 계산.
13. **급가속·급제동 중복 집계.** `TripCollector.kt:164-170` 디바운스 없음 → 한 번의 급가속이 샘플마다 카운트(5Hz·3초면 약 15회). 한 사건은 한 번만 세도록. 3.0 m/s² 임계값 근거 기록.
14. **깜빡이 없는 차선 변경 감지가 부정확.** 조향 휠 15° 이상 기준은 고속도로 차선 변경(휠 약 3~8°)을 놓치고, 교차로·램프·곡선은 오탐한다. 깜빡이가 자동으로 꺼진 직후 조향도 오탐. 스펙은 "비율"인데 개수만 셈(분모 없음).
    → 속도 구간별 임계값, 회전 지속 시간·복귀 패턴으로 차선 변경과 회전 구분, 깜빡이는 최근 N초 안에 켜졌는지로 판정, 비율로 계산. 각각 테스트.
15. **트립 경계.** 비-P 기어면 무조건 트립 시작(P→R→P 주차도 트립). 트립 시작 때 `warningStartMs`·`isSteeringManeuver`·`lastSpeed` 초기화 안 함. 트립 종료 시 켜져 있는 경고 미집계. `onDoorChanged` 비어 있음.
16. **결과 카드 불꽃·비 모션** (설계에 없음 — 사용자 확인 중). 유지한다면: 화면이 안 보이거나 `onPause`일 때 애니메이션 정지, 일정 시간(예: 3초) 뒤 멈춤, 이긴 날 불꽃에 섞인 빨강은 괜찮지만 아쉬운 날 비에는 빨강 금지.

## 3순위 — 정리

17. 패키지 `ai.pleos.safetyleague`는 Pleos 도메인처럼 보인다. 자체 ID(예: `io.github.ddayoung03.safetyleague`)로 변경 검토.
18. `TripRepository`가 메모리에만 저장 → 앱 재시작 시 사라짐. 시연 범위면 괜찮지만 로그에 명시.
19. `TripCollectorTest.testLowDayScenarioReplication`은 카운터를 직접 올려 센서 감지 경로를 거치지 않음. 감점 경로·0점 하한·두 번째 트립 거리 테스트 없음.
20. 이모지(☀️🌧️❄️) 사용 금지 규칙 위반, `allowBackup=true`, 기본 아이콘.
21. `LeagueCalculator.kt:70,77` Float 반올림 → Double로. `DemoDataTest.kt:112` 시각 문자열 비교는 "10:05"부터 깨짐.

## 확인된 좋은 점
- 시안 테스트(demo-data.test.js) 8개 케이스가 같은 기대값으로 모두 이식됨. DemoData 값이 JS와 동일.
- 연속 기록·승급/강등(P119/P279)·남은 일수·반올림·경로 보간 로직이 JS와 동일하게 동작.
- 커밋에 비밀키·개인정보·기기 경로 없음. `local.properties`는 커밋되지 않음.
- Pleos Nexus 저장소·SDK 버전(Vehicle 2.3.2, NaviHelper 3.0.5, FusedLocation 1.1.3)과 cold boot 요구사항 정리는 유용함.
