# 시작 연출과 홈 화면 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 앱을 켜면 2초 스플래시 뒤 새 홈 화면(패독 차고, A안)이 나오고, 홈이 앱의 출발점이 된다.

**Architecture:** `Screen.HOME`을 추가하고 지금 LEAGUE가 맡던 "출발점" 역할(시작, 복귀, 뒤로 가기, 정산 리다이렉트)을 HOME으로 옮긴다. 홈은 `ui/HomeScreen.kt` + `view_home.xml`. 스플래시는 `activity_main.xml` 맨 위 오버레이와 `ui/SplashIntro.kt`.

**Tech Stack:** Kotlin, Android View + ViewBinding, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-11-08-home-and-splash-design.md`

## Global Constraints

- 주행 중(restricted)에는 홈·스플래시를 보이지 않는다. `show()`의 기존 제한 검사를 그대로 탄다.
- 빨강 금지. 색은 `colors.xml`의 기존 값(`ink #0D0F13`, `yellow`, `ink_glow`, `ink_muted`) 재사용.
- 토끼는 기존 `RabbitView` + `Pose`만. 새 그림 없음. 스플래시 아이콘은 `@mipmap/ic_launcher_foreground`.
- 시연 모드는 `DemoData` 숫자(11월 시즌, 골드 등), 실제는 `standing()`, `promotionText()`, `wallet`, `repository.latestCompletedTrip`.
- 애니메이션은 `animationsEnabled()`가 거짓이면 생략, `motion`에 등록.
- 효과음 없음.

## Review Focus

- 안 본 결과가 있을 때 앱 시작: 스플래시 → 결과 카드(홈에 묻히지 않음). Task 4 실기 시험.
- 달 넘김 정산 카드가 있을 때 HOME으로 가면 정산이 먼저 뜸. Task 1 `redirectsHomeWithCard`.
- 첫 사용자(트립 없음) 홈: 순위 `–`, 승급 줄, "아직 주행 기록이 없어요"로 칸이 비지 않음. Task 3 실기.
- 다른 앱 다녀오기·화면 회전 재생성: 스플래시 다시 안 나옴. Task 4 (`savedInstanceState == null`일 때만).
- 스플래시 도중 주행 시작(restricted): 오버레이가 즉시 걷히고 주행 중 화면. Task 4.

---

### Task 1: 흐름을 HOME 중심으로

**Files:**
- Modify: `app/src/main/java/io/github/ddayoung03/safetyleague/ui/ScreenFlow.kt`, `ui/SettlementScreen.kt:17-18,40-43`, `ui/MainActivity.kt`(Screen enum, onCreate, demo 토글, syncWithTrip, show의 when), `ui/ReportScreen.kt:54,63`, `ui/ShopScreen.kt:36`, `ui/GuideScreen.kt:40`, `ui/LocationConsentScreen.kt:48`, `ui/VehicleSetupScreen.kt:23,55,89`
- Test: `app/src/test/java/io/github/ddayoung03/safetyleague/ui/ScreenFlowTest.kt`, `SettlementRouteTest.kt`

**Interfaces:**
- Produces: `Screen.HOME`; `Resume.HOME`(LEAGUE 대체); `backTarget`이 REPORT/SHOP/GUIDE/VEHICLE(프로필 있음)에서 `Screen.HOME`; `redirectToSettlement(Screen.HOME, true, false) == true`; `MainActivity.showHome()`(Task 3이 채움, 이 태스크에서는 `showLeague()`를 부르는 임시 본문 금지 — Task 3과 같은 커밋 전까지 빌드만 되면 됨: 빈 `view_home` 없이 `HomeScreen.kt`에 `internal fun MainActivity.showHome() = showLeague()` 한 줄로 시작하고 Task 3에서 교체).

- [ ] **Step 1: 테스트 수정(실패하게)**
  - `neverStuckOnDrivingAfterTheTripEnded`: `Resume.HOME`
  - `backGoesHomeFromSideScreens`: REPORT, SHOP, GUIDE → `Screen.HOME`
  - `vehicleBackNeedsAProfile`: `Screen.HOME` / null
  - `backDoesNothingSpecialOnMainScreens`: 목록에 `Screen.HOME` 추가, LEAGUE는 빼고 새 테스트 `leagueBacksHome`: `backTarget(Screen.LEAGUE, true) == Screen.HOME`
  - `SettlementRouteTest.redirectsHomeWithCard`: HOME+card → true, HOME+demo → false; 기존 LEAGUE 단언 유지
- [ ] **Step 2:** `./gradlew.bat testDebugUnitTest --tests '*ScreenFlowTest' --tests '*SettlementRouteTest'` → 컴파일 실패(HOME 없음)
- [ ] **Step 3: 구현.** enum에 HOME. `resumeAction`의 DRIVING 끝 분기 → `Resume.HOME`. `backTarget`: REPORT/SHOP/GUIDE/LEAGUE → HOME, VEHICLE → HOME(프로필 있을 때). `redirectToSettlement`: `target == LEAGUE || target == HOME`. 화면 안 뒤로 버튼(리포트·상점·가이드 back, 정산 닫기 `btnSettleLeague`, 위치 동의 완료, 차량 설정 완료·시연 차단)은 `show(Screen.HOME)`. 차량 설정의 "리그로 돌아가기"는 글자를 "홈으로 돌아가기"로 바꾸고 HOME. 결과 카드 `btnLeague`(글자 "리그")는 그대로 LEAGUE. MainActivity: 시작 `show(Screen.HOME)`, 시연 끄기 → HOME, `syncWithTrip`의 `Resume.HOME -> show(Screen.HOME)`, STAY 분기의 정산 재표시 조건을 `screen == LEAGUE || screen == HOME`으로, `show()` when에 `Screen.HOME -> showHome()`.
- [ ] **Step 4:** 같은 테스트 → PASS, `assembleDebug` 성공
- [ ] **Step 5: Commit** `feat: 홈을 앱의 출발점으로(흐름)`

### Task 2: 홈 인사말 (순수 함수)

**Files:**
- Create: `app/src/main/java/io/github/ddayoung03/safetyleague/ui/HomeGreeting.kt`
- Test: `app/src/test/java/io/github/ddayoung03/safetyleague/ui/HomeGreetingTest.kt`

**Interfaces:**
- Produces: `internal fun homeGreeting(hour: Int, inPromotionZone: Boolean, todayScore: Double?): String`

문구(이대로 쓴다):
- 승급권: `"승급권이에요! 이대로만 가요"`
- 오늘 트립 점수 ≥ 70: `"오늘 주행 멋졌어요! 이 기세로 가요"`
- 오늘 트립 점수 < 70: `"오늘도 수고했어요. 다음엔 더 부드럽게 가 봐요"`
- 5~10시: `"좋은 아침이에요! 오늘도 안전하게 가 봐요"`
- 11~17시: `"어서 와요! 오늘도 안전하게 가 봐요"`
- 그 외: `"오늘 하루도 수고했어요. 천천히 가요"`
우선순위는 위에서 아래.

- [ ] **Step 1: 테스트** `promotionWinsOverEverything`(hour 8, true, 50.0 → 승급 문구), `todayScoreAboveSeventyPraises`(70.0 → 칭찬, 경계 포함), `lowScoreIsGentle`(69.9 → 수고 문구, `"!"`·"감점" 미포함 단언), `timeOfDayWhenNoTripToday`(5→아침, 10→아침, 11→어서, 17→어서, 18→저녁, 4→저녁)
- [ ] **Step 2:** 실행 → FAIL(함수 없음)
- [ ] **Step 3:** 구현
- [ ] **Step 4:** PASS
- [ ] **Step 5: Commit** `feat: 홈 인사말`

### Task 3: 홈 화면

**Files:**
- Create: `app/src/main/res/layout/view_home.xml`, `ui/HomeScreen.kt`(Task 1의 한 줄 교체)
- Modify: `res/layout/view_league.xml`(btn_shop, btn_guide 제거), `ui/LeagueScreen.kt`(그 두 버튼 코드 제거), `res/layout/activity_main.xml`(시연 바에 `tab_home` "홈" 버튼, `tab_league` 앞), `MainActivity.setupDemoControls`(`tabHome` → HOME)

**Interfaces:**
- Consumes: `homeGreeting`(Task 2), `standing()`, `promotionText()`, `tierBelt()`, `wallet`, `ShopCatalog.points`, `SafetyGuide.suggested(currentWeather)`, `ScreenTransition.stagger`
- Produces: `MainActivity.showHome()`

레이아웃(1280×720dp 캔버스, A안 시안 기준): 배경 `scene_paddock` 어둡게(알파 약 0.55), 왼쪽 토끼(`@+id/rabbit`, `Pose.WELCOME`, 약 260dp 높이)와 그 위 말풍선(흰 바탕, ink 글자, Black 굵기), 오른쪽 시즌 카드(시즌 제목 / 벨트 + 큰 순위 `P3` Saira 노랑 + 티어 이름·승급 줄·남은 날 / 지난 주행 점수·포인트), 그 아래 2×2 메뉴 칸(제목 + 한 줄 정보, `pressFeedback()`, 클릭 시 `show(target, it)`로 원형 전환).

칸별 한 줄(실제 모드):
- 리그: 순위 있으면 `"같은 차종 ${s.total}명"`(필드 이름은 SeasonStanding에서 확인), 없으면 `promotionText(s)`
- 리포트: 이번 주 평균이 있으면 `"이번 주 평균 %.1f점"`, 없으면 `"이번 주 주행 기록 없음"`
- 피트샵: 쿠폰 있으면 `"쿠폰 N장"`, 없으면 `"포인트 ${ShopCatalog.points(balance)}P"`
- 가이드: `SafetyGuide.suggested(currentWeather)` 있으면 `"오늘은 ${card.title} 카드"`, 없으면 `"재난·상황·초보 꿀팁"`
시즌 카드 빈 상태: 순위 `–`, 지난 주행 `"아직 주행 기록이 없어요"`.

- [ ] **Step 1:** 레이아웃·화면 구현, 리그에서 상점·가이드 버튼 제거, 시연 탭 추가
- [ ] **Step 2:** `testDebugUnitTest assembleDebug` 통과
- [ ] **Step 3:** 에뮬레이터: 실제 모드 홈, 시연 모드 홈, 4칸 왕복(뒤로 버튼·시스템 뒤로 모두 홈), 리그 화면에 버튼 빠진 자리 확인. 캡처를 직접 본다.
- [ ] **Step 4: Commit** `feat: 홈 화면(패독 차고)`

### Task 4: 시작 연출

**Files:**
- Create: `ui/SplashIntro.kt`
- Modify: `res/layout/activity_main.xml`(맨 위 `@+id/splash` FrameLayout: ink 바탕, 아이콘 ImageView 200dp, `SAFETY LEAGUE` TextView(Saira, LEAGUE 노랑 — SpannableString), 200×6dp 바), `MainActivity.onCreate`

**Interfaces:**
- Produces: `internal fun MainActivity.playSplash()`

동작: `onCreate`에서 `savedInstanceState == null`이고 권한 화면으로 넘어가지 않을 때만 부른다. 기존 `showFlagLoader(text = R.string.loader_start)` 호출은 지운다(문자열 `loader_start`가 다른 곳에서 안 쓰이면 함께 삭제). 홈은 그 아래에서 평소대로 만들어진다. 타임라인(합 2000ms): 아이콘 0→300ms 스케일 0.6→1.06, 300→450ms →1.0; 글자 300→800ms 알파 0→1 + letterSpacing 0.4→0.12; 바 400→1600ms 너비 0→100%; 1700→2000ms 오버레이 알파 1→0 후 GONE. 오버레이 터치 시 즉시 GONE. `animationsEnabled()` 거짓이거나 `restricted`면 바로 GONE. `applyRestriction(true)`에서도 GONE. 핸들은 `motion`에 등록하지 않는다(첫 `show()`의 `finishAll()`이 지우면 안 됨) — 오버레이 자체 `animate()`와 `postDelayed`로 처리하고 `onPause`에서 GONE.

- [ ] **Step 1:** 구현
- [ ] **Step 2:** `testDebugUnitTest assembleDebug` 통과
- [ ] **Step 3:** 에뮬레이터: 강제 종료 후 켜기(스플래시 → 홈, 흰 깜빡임 없음), 스플래시 탭해서 건너뛰기, 홈 버튼으로 나갔다 오기(스플래시 없음), 안 본 결과 있는 상태로 켜기(스플래시 → 결과 → 뒤로 → 홈), 첫 사용자 흐름(데이터 지우기 백업 후 → 차량 선택 → 동의 → 홈, 끝나면 복원), 시연 각본 주행 중 restricted 진입 시 오버레이 없음.
- [ ] **Step 4:** `docs/app-dev-log.md` 기록, Commit `feat: 시작 연출`
