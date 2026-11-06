# UI·움직임 전면 개편 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 앱 전체를 D 타이밍 타워 방향으로 바꾸고, 움직임·효과음·토끼 레이서를 넣는다.

**Architecture:** 판단은 `ui/motion/`의 순수 함수에 두고 단위 테스트로 고정한다. 진행 중인 움직임은 `MotionScope` 하나가 관리해 주행 시작, 화면 이동, 화면 터치 때 최종 상태로 끝낸다. 기존 `MainActivity.show()` 구조와 화면별 파일은 유지한다.

**Tech Stack:** Kotlin, Android View, ViewBinding, AndroidX DynamicAnimation, SoundPool, AAOS CarUxRestrictions

**Spec:** `docs/superpowers/specs/2026-11-06-ui-motion-redesign-design.md`

## Global Constraints

- `core/`와 시연 숫자를 바꾸지 않는다.
- 새 의존성은 `androidx.dynamicanimation:dynamicanimation:1.0.0` 하나다.
- 바탕 `#0D0F13`, 빛 `#1C2028`, 강조 `#FFC52E`, 글자 `#F4F4F1`, 보조 `#8A909B`. 빨강은 출발 신호등에만 쓴다.
- 움직임 시간은 120ms, 240ms, 420ms. 목록 간격 40ms. 로딩 최대 1.5초.
- `transform`과 투명도만 움직인다.
- 주행 중에는 점수, 순위, 감점, 토끼, 효과음이 없다.
- 아쉬운 날은 빨강과 신호등이 없다.
- 실명, 연락처, 이메일, 소속을 넣지 않는다.
- 빌드와 테스트: `.\gradlew.bat testDebugUnitTest assembleDebug`. 에뮬레이터는 GPU host, cold boot.
- 기존 시연 확인 목록: `docs/app-dev-log.md`의 "시연 92점", "시연 64점", "시연 리그", "리포트" 항목.

## Review Focus

1. 결과 연출 중 주행이 시작됨: 즉시 주행 중 화면으로 가고 소리가 멈춘다. (Task 1, 5)
2. 연출 중 버튼을 빠르게 연속으로 누름: 앱이 죽지 않고 마지막 화면의 값이 맞는다. (Task 1)
3. 연출 중 앱이 백그라운드로 감: 돌아왔을 때 최종 상태다. (Task 1)
4. 순위가 그대로거나 내려감, 첫 트립이라 이전 순위가 없음: 추월 움직임 없이 결과가 나온다. (Task 3)
5. 100점, 한 자리 점수, 긴 운전자 이름: 숫자와 이름이 잘리지 않는다. (Task 3, 4)

---

### Task 1: 움직임 기반과 새 색

**Files:**
- Modify: `app/build.gradle.kts`, `app/src/main/res/values/colors.xml`, `app/src/main/res/layout/activity_main.xml`, `ui/MainActivity.kt`
- Create: `ui/motion/MotionTokens.kt`, `ui/motion/MotionScope.kt`, `ui/motion/ScreenTransition.kt`
- Test: `app/src/test/java/.../ui/motion/MotionScopeTest.kt`

**Interfaces:**
- Produces: `object MotionTokens { FAST=120L; BASE=240L; SLOW=420L; STAGGER=40L; LOADER_MAX=1500L }`
- Produces: `class MotionScope { fun add(onFinish: () -> Unit = {}, cancel: () -> Unit): Handle; fun finishAll() }`. `Handle.done()`는 끝난 항목을 지운다.
- Produces: `MainActivity.motion: MotionScope`, `fun ScreenTransition.enter(container: ViewGroup, scope: MotionScope)`
- Produces: `animationsEnabled(): Boolean` = `ValueAnimator.areAnimatorsEnabled()`

- [ ] **Step 1: 테스트 작성** `finishAllRunsEachFinalOnce`, `finishedItemIsNotFinalizedAgain`, `finishAllTwiceDoesNothing`, `itemAddedInsideFinishIsAlsoFinished`.
- [ ] **Step 2: 실패 확인** `.\gradlew.bat testDebugUnitTest --tests "*MotionScopeTest"`. 클래스가 없어 실패한다.
- [ ] **Step 3: 구현** `MotionScope`, `MotionTokens`, 새 색(`ink`, `ink_glow`, `ink_text`, `ink_muted`, `signal_red`)을 넣는다. 기존 색 이름은 Task 11까지 유지한다.
- [ ] **Step 4: 연결** `show()` 시작에서 `motion.finishAll()`, `onPause()`에서 `motion.finishAll()`을 부른다. `restricted` 대상 화면에는 전환 없이 바로 넣는다. 그 밖에는 새 화면을 420ms 안에 페이드와 이동으로 들인다.
- [ ] **Step 5: 확인** 테스트와 빌드 통과. 에뮬레이터에서 화면 이동이 부드럽고, 탭을 빠르게 연타해도 죽지 않는다.
- [ ] **Step 6: 커밋** 설계 문서, 이 계획, `design/`을 함께 커밋한다.

### Task 2: 체커기 로딩

**Files:**
- Create: `ui/view/FlagLoaderView.kt`
- Delete: `ui/view/CheckerFlagView.kt`와 사용처 교체
- Test: `.../ui/view/FlagWaveTest.kt`

**Interfaces:**
- Produces: `internal fun flagWave(x: Float, width: Float, timeSec: Float): Float`, 깃대 쪽 x=0에서 0을 돌려준다.
- Produces: `FlagLoaderView.start()`, `stop()`. 화면에 없으면 그리지 않는다.

시안의 `flag()` 함수(`design/prototypes/motion-hybrid.html`)를 옮긴다. 12×8칸, 사인파 두 개를 더한다.

- [ ] **Step 1: 테스트** `poleSideDoesNotMove`, `freeEndMovesOverTime`, `waveStaysInsideLimit`(절댓값 14 이하).
- [ ] **Step 2: 실패 확인**
- [ ] **Step 3: 구현**. 문구는 `기록 집계 중`.
- [ ] **Step 4: 통과 확인**, 에뮬레이터에서 앱 시작 때 오른쪽 아래에 깃발이 펄럭인다.
- [ ] **Step 5: 커밋**

### Task 3: 이긴 날 결과

**Files:**
- Create: `ui/motion/ResultMotion.kt`, `ui/motion/CountUp.kt`, `ui/view/StartLightsView.kt`, `ui/view/SpeedLinesView.kt`
- Modify: `res/layout/view_result_win.xml`, `ui/ResultWinScreen.kt`, `ui/TowerRow.kt`, `ui/LeagueStanding.kt`, `ui/MainActivity.kt`
- Test: `.../ui/motion/ResultMotionTest.kt`

**Interfaces:**
- Produces: `enum class ResultIntro { BIG, CALM, QUICK, NONE }`
- Produces: `fun resultIntro(eligible: Boolean, win: Boolean, justFinished: Boolean, animations: Boolean): ResultIntro`
- Produces: `fun overtakeRanks(before: Int?, after: Int?, max: Int = 3): List<Int>`
- Produces: `fun MainActivity.rankMove(trip: CompletedTrip): Pair<Int, Int>?`. 기존 `rankChange`의 계산을 재사용한다.
- Consumes: `MotionScope`, `MotionTokens`, `FlagLoaderView`

순서: 로딩 → 신호등 5개(300ms 간격) → 소등 → 속도선 + 점수 카운트 → 착지 → 내 줄 추월 → 정보와 버튼 등장. 화면을 누르면 `motion.finishAll()`. 스프링은 `SpringAnimation`.

`justFinished`는 `onTripResult`와 시연 각본 끝에서만 참이고, 표시 후 거짓이 된다.

- [ ] **Step 1: 테스트**
```kotlin
assertEquals(BIG, resultIntro(true, true, true, true))
assertEquals(CALM, resultIntro(true, true, false, true))
assertEquals(CALM, resultIntro(true, false, true, true))
assertEquals(QUICK, resultIntro(false, false, true, true))
assertEquals(NONE, resultIntro(true, true, true, false))
assertEquals(listOf(127, 126, 125), overtakeRanks(128, 125))
assertEquals(listOf(127, 126, 125), overtakeRanks(140, 125))
assertEquals(emptyList<Int>(), overtakeRanks(125, 125))
assertEquals(emptyList<Int>(), overtakeRanks(125, 126))
assertEquals(emptyList<Int>(), overtakeRanks(null, 125))
```
- [ ] **Step 2: 실패 확인**
- [ ] **Step 3: 구현** 레이아웃을 시안 D로 바꾼다. 불꽃, 금속 숫자를 쓰지 않는다. 최종 값은 연출 전에 뷰에 넣어 두고, 연출은 표시만 바꾼다.
- [ ] **Step 4: 통과 확인**
- [ ] **Step 5: 에뮬레이터 확인** 시연 92점: 92, 18.4km, 연속 13일, P125 ▲3, `추월`, `0.3점 차`, 승급까지 6계단. 100점과 긴 이름을 임시 값으로 넣어 잘리지 않는지 본다. 연출 중 Virtual Console로 D단을 넣으면 바로 주행 중 화면으로 간다.
- [ ] **Step 6: 커밋**

### Task 4: 아쉬운 날과 3km 미만

**Files:**
- Modify: `res/layout/view_result_low.xml`, `ui/ResultLowScreen.kt`

**Interfaces:**
- Consumes: `resultIntro`, `CountUp`, `MotionScope`

신호등과 속도선은 없다. 3km 미만은 `QUICK`이다. `layoutSeasonHeader`, `tvStreakHint` 숨김은 유지한다.

- [ ] **Step 1: 구현** 시안 D 스타일. 빨강 없음. 피트 보드는 스프링으로 한 번 등장.
- [ ] **Step 2: 확인** 시연 64점: 64, 22.1km, 13일 → 0일, P126 ▼1, `0.4점 차`, `깜빡이!`, `매너 +25점 가능`. 한 자리 점수와 3km 미만 카드 표시.
- [ ] **Step 3: 커밋**

### Task 5: 효과음

**Files:**
- Create: `ui/motion/SoundFx.kt`, `ui/motion/SoundPolicy.kt`, `app/src/main/res/raw/fx_light.ogg`, `fx_go.ogg`, `fx_overtake.ogg`, `fx_win.ogg`
- Modify: `ui/ResultWinScreen.kt`, `ui/MainActivity.kt`, `design/README.md`
- Test: `.../ui/motion/SoundPolicyTest.kt`

**Interfaces:**
- Produces: `fun canPlaySound(gear: MotionGear, speedMps: Float?, resumed: Boolean, restricted: Boolean, focus: Boolean): Boolean`. `canAnimateCrew`와 `focus`를 함께 본다.
- Produces: `class SoundFx(context) { fun play(id: Fx); fun stopAll(); fun release() }`, `enum class Fx { LIGHT, GO, OVERTAKE, WIN }`

음원은 Python 표준 라이브러리로 합성해 만들고, 생성 스크립트를 `design/sound/`에 남긴다. `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`을 요청한다.

- [ ] **Step 1: 테스트** `parkedWithFocusPlays`, `driveNeverPlays`, `restrictedNeverPlays`, `movingNeverPlays`, `noFocusStaysSilent`.
- [ ] **Step 2: 실패 확인**
- [ ] **Step 3: 구현** `MotionScope`의 마감 처리에 `stopAll()`을 넣어 건너뛰면 소리도 멈춘다. 아쉬운 날은 소리를 내지 않는다.
- [ ] **Step 4: 확인** 테스트 통과. 에뮬레이터에서 92점 각본에 소리가 나고, 연출 중 D단을 넣으면 멈춘다.
- [ ] **Step 5: 커밋**

### Task 6: 토끼 픽셀 어셋 (사용자 확인 필요)

**Files:**
- Create: `design/rabbit/`(정리된 스프라이트, 부위, 미리보기), `design/rabbit/make_sprites.py`
- Modify: `design/README.md`

- [ ] **Step 1: 격자 정리** `design/generated/rabbit-pixel-sheet-1.png`의 네 포즈를 진짜 픽셀 격자로 줄인다. 팔레트를 제한하고 배경을 투명하게 한다.
- [ ] **Step 2: 부위와 프레임** 동작 9종(대기, 승리, 추월, 피트 보드, 라디오, 로딩, 맞이, 끄덕임, 가리키기)의 프레임을 만든다. 표정과 귀는 부위로 조합한다.
- [ ] **Step 3: 움직이는 미리보기** `design/rabbit/preview.html`을 만든다.
- [ ] **Step 4: 사용자 확인** 움직임 품질 승인을 받는다.
- [ ] **Step 5: 커밋**

### Task 7: RabbitView

**Files:**
- Create: `ui/view/RabbitView.kt`, `ui/view/RabbitPose.kt`, `app/src/main/res/drawable-nodpi/rabbit_*.png`
- Modify: `ui/ResultWinScreen.kt`, `ui/ResultLowScreen.kt`, `ui/view/CrewEntrance.kt`
- Delete: `res/drawable-nodpi/crew_*.webp`, `ui/view/CrewFaceView.kt`
- Test: `.../ui/view/RabbitPoseTest.kt`

**Interfaces:**
- Produces: `enum class Pose { IDLE, WIN, OVERTAKE, PIT, RADIO, LOAD, WELCOME, NOD, POINT }`
- Produces: `fun frameIndex(pose: Pose, timeMs: Long, frameCount: Int): Int`
- Produces: `RabbitView.pose: Pose`, `start()`, `stop()`. 정수 배율과 최근접 보간으로 그린다.

- [ ] **Step 1: 테스트** `frameLoopsWithinCount`, `oneShotPoseStopsOnLastFrame`, `zeroTimeIsFirstFrame`, `everyPoseHasFrames`.
- [ ] **Step 2: 실패 확인**
- [ ] **Step 3: 구현** `canAnimateCrew`가 거짓이면 첫 프레임에서 멈춘다.
- [ ] **Step 4: 확인** 결과 카드에서 승리와 피트 보드 동작 표시. 주행 중 화면에는 없음.
- [ ] **Step 5: 커밋**

### Task 8: 리그

**Files:**
- Modify: `res/layout/view_league.xml`, `res/layout/view_league_placement.xml`, `ui/LeagueScreen.kt`, `ui/TowerRow.kt`
- Consumes: `ScreenTransition`, `MotionScope`, `RabbitView`

- [ ] **Step 1: 구현** 버튼 위치에서 퍼지는 전환, 줄 40ms 간격 등장, 탭 전환 시 줄 교체, 토끼 `POINT`. 지역·친구 탭의 기존 숨김 규칙 유지.
- [ ] **Step 2: 확인** 시연 리그: 골드 397명 중 P125, 88.8, 승급 라인까지 6계단 · 0.9점, 21일 남음. 세 탭을 빠르게 전환해도 줄이 섞이지 않는다.
- [ ] **Step 3: 커밋**

### Task 9: 리플레이

**Files:**
- Modify: `res/layout/view_replay.xml`, `ui/ReplayScreen.kt`, `ui/view/CircuitRouteView.kt`
- Test: `.../ui/view/RouteProgressTest.kt`

**Interfaces:**
- Produces: `CircuitRouteView.progress: Float`(0~1)
- Produces: `internal fun visibleEvents(eventPositions: List<Float>, progress: Float): Int`

- [ ] **Step 1: 테스트** `noEventsBeforeStart`, `allEventsAtEnd`, `eventsAppearInOrder`.
- [ ] **Step 2: 실패 확인**
- [ ] **Step 3: 구현** 경로가 그려지고 차 점이 달리며 사건이 시간순으로 뜬다. 토끼 `RADIO`. 경로가 없으면 `routeNote` 문구를 유지한다.
- [ ] **Step 4: 확인** 시연 리플레이와 `geo fix` 실제 경로. 누르면 끝 상태로 건너뛴다.
- [ ] **Step 5: 커밋**

### Task 10: 주간 리포트

**Files:**
- Modify: `res/layout/view_report.xml`, `ui/ReportScreen.kt`, `ui/view/PositionChartView.kt`

- [ ] **Step 1: 구현** 막대가 자라고 순위 선이 그려지며 평균 점수가 카운트된다. 토끼 `NOD`. 빈 상태 유지.
- [ ] **Step 2: 확인** 시연: 11.03 ~ 11.09 · 주행 6일, 막대 `화 수 목 금 토 일 월`, 88.8, +3.1, P131 → P125 ▲6.
- [ ] **Step 3: 커밋**

### Task 11: 나머지 화면과 정리

**Files:**
- Modify: `res/layout/view_vehicle_selection.xml`, `view_location_consent.xml`, `view_driving.xml`, `activity_main.xml`, `ui/VehicleSetupScreen.kt`, `ui/LocationConsentScreen.kt`, `ui/DrivingScreen.kt`, `ui/ScreenStyle.kt`, `res/values/colors.xml`, `res/values/themes.xml`
- Delete: 쓰이지 않게 된 `FlameScoreView`, `RainScoreView`, `MetalTextView`, `KerbView`, `AsphaltDrawable`, `GlassDrawable`, 옛 색

- [ ] **Step 1: 구현** 차종 선택은 차례로 등장, 토끼 `WELCOME`. 위치 동의는 차분한 등장. 주행 중은 색과 글꼴만 바꾸고 기존 움직임 규칙을 유지한다.
- [ ] **Step 2: 정리** `rg`로 사용처가 없는 View와 색을 확인하고 삭제한다. `black_han_sans.ttf`는 사용자 확인 전에는 지우지 않는다.
- [ ] **Step 3: 확인** 첫 설치 흐름, 주행 중 날씨 4종, 시연 모드 진입이 정상이다.
- [ ] **Step 4: 커밋**

### Task 12: 최종 확인과 문서

**Files:**
- Create: `design/submission/`
- Modify: `README.md`, `docs/CODEMAP.md`, `docs/app-dev-log.md`, `../AGENTS.md`, `../project-notes.md`

- [ ] **Step 1: 전체 확인** 설계 문서의 완료 기준을 전부 돌린다. 애니메이션 배율 0은 `adb shell settings put global animator_duration_scale 0`으로 보고 원래 값으로 되돌린다.
- [ ] **Step 2: 성능 기록** `adb shell dumpsys gfxinfo io.github.ddayoung03.safetyleague`로 결과 연출의 끊긴 프레임 비율을 기록한다.
- [ ] **Step 3: 제출 시안** 11/13까지 끝났으면 앱 화면 6장을 `design/submission/`에 캡처하고 README 이미지를 바꾼다. 기획서의 캐릭터 표현을 확인한다.
- [ ] **Step 4: 개인정보 검사** `personal-info.txt` 문자열이 변경분에 없는지 확인한다.
- [ ] **Step 5: 사용자 확인 후 병합과 push**
