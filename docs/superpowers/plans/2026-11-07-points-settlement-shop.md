# 포인트 정산과 상점 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 시즌 정산 때 포인트를 지급해 시상대 화면으로 보여 주고, 토끼 피트샵에서 시연용 쿠폰으로 교환한다.

**Architecture:** 계산과 교환 규칙은 `core/calculator`의 순수 함수로 둔다. 잔액, 쿠폰, 표시할 정산 카드는 티어와 같은 `vehicle_profile` SharedPreferences에 저장해 정산을 한 번의 `commit`으로 끝낸다. 화면은 `MainActivity.show()`의 새 `Screen` 둘로 붙이고, 리그로 가는 길 한 곳에서 미표시 정산을 가로채 정산 화면으로 보낸다.

**Tech Stack:** Kotlin, Android View + ViewBinding, JUnit 4. 새 라이브러리 없음.

**Spec:** `docs/superpowers/specs/2026-11-07-points-settlement-shop-design.md`

## Global Constraints

- `core/`, `ui/`로 시작하는 경로는 `app/src/main/java/io/github/ddayoung03/safetyleague/` 아래이고, `res/`는 `app/src/main/res/` 아래다.
- 브랜치 `claude/points-shop`. 사용자 확인 전에는 main 병합과 push를 하지 않는다.
- 테스트: `./gradlew.bat testDebugUnitTest --console=plain`, 빌드: `./gradlew.bat assembleDebug --console=plain`. 시작 기준 192개 통과.
- 티어 배율 브론즈 1.0, 실버 1.2, 골드 1.5, 플래티넘 2.0, 다이아 2.5. 월 인정 거리 1,000km.
- 색은 `@color/ink`, `yellow`, `ink_glow`, `ink_text`, `ink_muted`, `track_empty`만 쓴다. 빨강 금지. 숫자는 `numberFont`, 글자는 Pretendard.
- 화면 캔버스는 1280×720dp(`DesignCanvas`). 글자는 19sp 이상. 기울어진 바탕은 `skew()`, 눌림은 `pressFeedback()`.
- 주행 중에는 정산, 상점, 쿠폰함이 열리지 않는다(`show()`의 `restricted` 규칙을 유지).
- 시스템 알림창을 쓰지 않는다(에뮬레이터 자동화와 차량 UX 때문).
- 저장소에 실명, 연락처, 이메일을 넣지 않는다.

## Review Focus

- 교환 버튼을 빠르게 두 번 누름: 쿠폰은 한 장만 생기고 잔액은 한 번만 준다. (Task 2 `exchangeTwiceNeedsEnoughBalance`, Task 6 버튼 잠금)
- 저장된 지갑 문자열이 깨짐: 앱이 죽지 않고 빈 지갑으로 시작한다. (Task 2 `decodeBrokenTextGivesEmptyWallet`)
- 쿠폰 만료일 당일: 아직 유효하고 다음 날부터 만료다. (Task 2 `couponExpiresAfterNinetyDays`)
- 차종을 전기차에서 내연기관차로 바꿈: 충전 크레딧 목표가 풀리고 받은 쿠폰은 쿠폰함에 남는다. (Task 2 `goalLeavesWhenItemUnavailable`)
- 정산 화면을 보다가 주행 시작: 주행 중 화면으로 바뀌고, 주차하면 정산 화면이 다시 뜬다. (Task 4 `redirectsOnlyRealLeagueWithCard`, Task 7 확인)

---

### Task 1: 포인트 계산

**Files:**
- Create: `app/src/main/java/io/github/ddayoung03/safetyleague/core/calculator/PointCalculator.kt`
- Test: `app/src/test/java/io/github/ddayoung03/safetyleague/core/calculator/PointCalculatorTest.kt`

**Interfaces:**
- Produces:
  - `data class SeasonPoints(val season: YearMonth, val tier: String, val km: Double, val average: Double?, val bonusPercent: Int, val multiplierPercent: Int, val points: Int)`
  - `object PointCalculator`: `bonusPercent(average: Double?): Int`, `multiplierPercent(tier: String): Int`, `points(km: Double, average: Double?, tier: String): Int`, `forSeason(trips: List<Trip>, season: YearMonth, tier: String): SeasonPoints`

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
@Test fun specExamples() {
    assertEquals(550, PointCalculator.points(500.0, 75.0, "브론즈"))
    assertEquals(75, PointCalculator.points(50.0, 95.0, "브론즈"))
    assertEquals(750, PointCalculator.points(500.0, 95.0, "브론즈"))
    assertEquals(1375, PointCalculator.points(500.0, 75.0, "다이아"))
    assertEquals(188, PointCalculator.points(50.0, 95.0, "다이아"))
    assertEquals(1248, PointCalculator.points(640.0, 88.8, "골드"))
    assertEquals(3750, PointCalculator.points(1200.0, 95.0, "다이아"))
    assertEquals(300, PointCalculator.points(300.0, 60.0, "브론즈"))
}
@Test fun bonusBoundaries() {
    assertEquals(0, PointCalculator.bonusPercent(null))
    assertEquals(0, PointCalculator.bonusPercent(69.9))
    assertEquals(10, PointCalculator.bonusPercent(70.0))
    assertEquals(10, PointCalculator.bonusPercent(79.9))
    assertEquals(30, PointCalculator.bonusPercent(80.0))
    assertEquals(30, PointCalculator.bonusPercent(89.9))
    assertEquals(50, PointCalculator.bonusPercent(90.0))
}
@Test fun multipliers() {
    assertEquals(listOf(100, 120, 150, 200, 250), TierMovement.tiers.map(PointCalculator::multiplierPercent))
    assertEquals(100, PointCalculator.multiplierPercent("없는 티어"))
}
@Test fun seasonUsesEligibleTripsOfThatMonthOnly() {
    val trips = listOf(Trip("2026-10-01", 80.0, 100.0), Trip("2026-10-31", 80.0, 2.9), Trip("2026-11-01", 100.0, 500.0))
    val s = PointCalculator.forSeason(trips, YearMonth.of(2026, 10), "골드")
    assertEquals(100.0, s.km, 0.0); assertEquals(80.0, s.average!!, 0.0)
    assertEquals(30, s.bonusPercent); assertEquals(150, s.multiplierPercent); assertEquals(195, s.points)
}
@Test fun emptySeasonIsZero() {
    val s = PointCalculator.forSeason(emptyList(), YearMonth.of(2026, 10), "골드")
    assertEquals(0, s.points); assertNull(s.average)
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew.bat testDebugUnitTest --tests "*PointCalculatorTest" --console=plain`
Expected: `PointCalculator` 참조 오류로 FAIL

- [ ] **Step 3: `PointCalculator.kt` 구현**

`forSeason`은 `SeasonRanking.calculate`와 같이 `km >= 3.0`과 시즌 날짜로 거르고 평균은 `LeagueCalculator.average`를 쓴다(리그 화면 평균과 같은 값). `points`는 부동소수 오차를 피해 `BigDecimal`로 `min(km, 1000) × (100+bonus) × multiplier ÷ 10000`을 계산하고 `RoundingMode.HALF_UP`으로 한 번만 반올림한다.

- [ ] **Step 4: 통과 확인** — 위 명령, Expected: PASS

- [ ] **Step 5: 커밋** — `feat: 시즌 포인트 계산 추가`

---

### Task 2: 지갑, 상품, 말풍선 규칙

**Files:**
- Create: `core/calculator/Wallet.kt`, `core/calculator/ShopCatalog.kt`
- Test: `app/src/test/.../core/calculator/WalletTest.kt`, `ShopCatalogTest.kt`

**Interfaces:**
- Consumes: `Powertrain` (`core/model`)
- Produces:
  - `data class ShopItem(val id: String, val name: String, val price: Int, val powertrain: Powertrain?)` (null은 전체 대상)
  - `object ShopCatalog`: `items: List<ShopItem>`, `itemsFor(powertrain: Powertrain): List<ShopItem>`, `item(id: String): ShopItem?`, `talk(balance: Int, target: ShopItem?): String`
  - `data class Coupon(val id: String, val itemId: String, val issuedOn: LocalDate)`: `expiresOn: LocalDate`, `isExpired(today: LocalDate): Boolean`
  - `data class Wallet(val balance: Int = 0, val goalId: String? = null, val coupons: List<Coupon> = emptyList())`: `plus(points: Int): Wallet`, `withGoal(id: String?): Wallet`, `exchange(item: ShopItem, today: LocalDate): Wallet?`, `withoutUnavailableGoal(items: List<ShopItem>): Wallet`, `target(items: List<ShopItem>): ShopItem?`, `encode(): String`; `Wallet.decode(text: String?): Wallet`

상품 id: `charge_10k`(1,000P, EV), `service_10k`(1,000P, 전체), `service_50k`(5,000P, 전체), `engine_oil`(10,000P, ICE). 이름은 설계 문서 표 그대로.

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
private val day = LocalDate.of(2026, 11, 7)
private val small = ShopCatalog.item("service_10k")!!

@Test fun catalogFollowsPowertrain() {
    assertEquals(listOf("charge_10k", "service_10k", "service_50k"), ShopCatalog.itemsFor(Powertrain.EV).map { it.id })
    assertEquals(listOf("service_10k", "service_50k", "engine_oil"), ShopCatalog.itemsFor(Powertrain.ICE).map { it.id })
}
@Test fun exchangeTakesPointsAndClearsGoal() {
    val w = Wallet(1500, "service_10k").exchange(small, day)!!
    assertEquals(500, w.balance); assertNull(w.goalId); assertEquals("service_10k", w.coupons.single().itemId)
}
@Test fun exchangeTwiceNeedsEnoughBalance() {
    val once = Wallet(1000).exchange(small, day)!!
    assertNull(once.exchange(small, day))
    val twice = Wallet(2000).exchange(small, day)!!.exchange(small, day)!!
    assertEquals(2, twice.coupons.map { it.id }.distinct().size)
}
@Test fun couponExpiresAfterNinetyDays() {
    val c = Coupon("a", "service_10k", day)
    assertEquals(day.plusDays(90), c.expiresOn)
    assertFalse(c.isExpired(day.plusDays(90))); assertTrue(c.isExpired(day.plusDays(91)))
}
@Test fun goalLeavesWhenItemUnavailable() {
    val w = Wallet(0, "charge_10k", listOf(Coupon("a", "charge_10k", day)))
        .withoutUnavailableGoal(ShopCatalog.itemsFor(Powertrain.ICE))
    assertNull(w.goalId); assertEquals(1, w.coupons.size)
}
@Test fun targetIsGoalThenCheapestUnclaimedThenCheapest() {
    val items = ShopCatalog.itemsFor(Powertrain.ICE)
    assertEquals("service_50k", Wallet(0, "service_50k").target(items)!!.id)
    assertEquals("service_10k", Wallet().target(items)!!.id)
    assertEquals("service_50k", Wallet(0, null, listOf(Coupon("a", "service_10k", day))).target(items)!!.id)
    val all = items.mapIndexed { i, it -> Coupon("$i", it.id, day) }
    assertEquals("service_10k", Wallet(0, null, all).target(items)!!.id)
}
@Test fun encodeRoundTrip() {
    val w = Wallet(3420, "service_50k", listOf(Coupon("service_10k-1", "service_10k", day)))
    assertEquals(w, Wallet.decode(w.encode()))
    assertEquals(Wallet(), Wallet.decode(Wallet().encode()))
}
@Test fun decodeBrokenTextGivesEmptyWallet() {
    assertEquals(Wallet(), Wallet.decode(null))
    assertEquals(Wallet(), Wallet.decode("망가진 값"))
}
@Test fun talkShowsWhatIsLeft() {
    val big = ShopCatalog.item("service_50k")!!
    assertEquals("정비 쿠폰 5만 원까지 1,580P 남았어요.", ShopCatalog.talk(3420, big))
    assertEquals("정비 쿠폰 5만 원, 지금 교환할 수 있어요!", ShopCatalog.talk(5000, big))
}
```

- [ ] **Step 2: 실패 확인** — `--tests "*WalletTest" --tests "*ShopCatalogTest"`, Expected: 컴파일 FAIL

- [ ] **Step 3: 구현**

`encode`는 org.json이 단위 테스트에서 동작하지 않으므로 줄 단위 문자열로 한다. 1줄 잔액, 2줄 goalId(없으면 빈 줄), 3줄부터 `id|itemId|yyyy-MM-dd`. `decode`는 `runCatching`으로 감싸 실패하면 `Wallet()`. 쿠폰 id는 `"${item.id}-$today-${coupons.size + 1}"`. 천 단위 쉼표는 `Locale.US` 형식을 쓴다. `talk`의 `target`이 null이면 `어서 와요!`를 돌려준다.

- [ ] **Step 4: 통과 확인** — Expected: PASS

- [ ] **Step 5: 커밋** — `feat: 지갑과 상점 규칙 추가`

---

### Task 3: 정산 카드 계산과 저장

**Files:**
- Create: `core/calculator/SettlementCard.kt`, `core/repository/PointWalletStore.kt`
- Modify: `core/repository/SeasonSettlementStore.kt`, `core/data/DemoData.kt`
- Test: `app/src/test/.../core/calculator/SettlementCardTest.kt`, `app/src/test/.../core/DemoDataTest.kt`

**Interfaces:**
- Consumes: `PointCalculator.forSeason`, `Wallet`, `TierMovement.settleSince`/`summarize`/`message`
- Produces:
  - `data class SettlementCard(val seasonLabel: String, val tier: String, val points: Int, val balance: Int, val km: Int, val average: Double?, val bonusPercent: Int, val multiplierPercent: Int, val seasons: Int, val promotedTo: String?, val message: String)`: `encode(): String`, `SettlementCard.decode(text: String?): SettlementCard?`
  - `SettlementCard.of(trips: List<Trip>, results: List<SeasonResult>, balanceBefore: Int): SettlementCard?` (합계 0P면 null)
  - `class PointWalletStore(context)`: `load(): Wallet`, `save(wallet: Wallet): Boolean` (`commit`). `companion`: `KEY_WALLET = "point_wallet"`, prefs 이름 `vehicle_profile`
  - `SeasonSettlementStore`: `card(): SettlementCard?`, `clearCard()`
  - `DemoData.settlementCard: SettlementCard`, `DemoData.startWallet(): Wallet`

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
private val oct = YearMonth.of(2026, 10)
private val nov = YearMonth.of(2026, 11)
private fun result(s: YearMonth, from: String, to: String, move: TierMove) = SeasonResult(s, from, to, move, 10)

@Test fun oneSeasonKeepsBreakdown() {
    val c = SettlementCard.of(listOf(Trip("2026-10-05", 85.0, 640.0)), listOf(result(oct, "골드", "골드", TierMove.STAYED)), 2172)!!
    assertEquals("10월 시즌", c.seasonLabel); assertEquals(1248, c.points); assertEquals(3420, c.balance)
    assertEquals(640, c.km); assertEquals(30, c.bonusPercent); assertEquals(150, c.multiplierPercent)
    assertEquals(1, c.seasons); assertNull(c.promotedTo)
}
@Test fun tierOfThatSeasonIsUsedAfterPromotion() {
    val c = SettlementCard.of(listOf(Trip("2026-10-05", 85.0, 100.0)), listOf(result(oct, "골드", "플래티넘", TierMove.PROMOTED)), 0)!!
    assertEquals(195, c.points); assertEquals("플래티넘", c.promotedTo)
}
@Test fun skippedSeasonsAddUpEachWithItsTier() {
    val trips = listOf(Trip("2026-10-05", 85.0, 100.0), Trip("2026-11-05", 85.0, 100.0))
    val c = SettlementCard.of(trips, listOf(result(oct, "골드", "플래티넘", TierMove.PROMOTED), result(nov, "플래티넘", "플래티넘", TierMove.STAYED)), 0)!!
    assertEquals(195 + 260, c.points); assertEquals(2, c.seasons); assertEquals("2개 시즌 합산", c.seasonLabel)
}
@Test fun noPointsNoCard() {
    assertNull(SettlementCard.of(emptyList(), listOf(result(oct, "골드", "골드", TierMove.STAYED)), 100))
}
@Test fun encodeRoundTripAndBrokenText() {
    val c = SettlementCard("10월 시즌", "골드", 1248, 3420, 640, 86.4, 30, 150, 1, null, "안내")
    assertEquals(c, SettlementCard.decode(c.encode()))
    assertNull(SettlementCard.decode("망가진 값")); assertNull(SettlementCard.decode(null))
}
```

`DemoDataTest`에 추가:

```kotlin
@Test fun demoSettlementMatchesCalculator() {
    val c = DemoData.settlementCard
    assertEquals(PointCalculator.points(c.km.toDouble(), c.average, c.tier), c.points)
    assertEquals("10월 시즌", c.seasonLabel); assertEquals(1248, c.points)
    val w = DemoData.startWallet()
    assertEquals(3420, w.balance); assertEquals("service_50k", w.goalId); assertEquals(1, w.coupons.size)
    assertEquals(w.balance, c.balance)
}
```

- [ ] **Step 2: 실패 확인** — `--tests "*SettlementCardTest" --tests "*DemoDataTest"`, Expected: 컴파일 FAIL

- [ ] **Step 3: 구현**

`message`는 `TierMovement.message(TierMovement.summarize(results)!!)`를 그대로 쓴다. `promotedTo`는 summarize 결과가 `PROMOTED`일 때 `toTier`. `seasons > 1`이면 `tier`는 마지막 시즌 `fromTier`.

`settleIfNeeded`: `settleSince` 결과를 변수에 두고, 기존 `edit()` 사슬에 `putString(KEY_WALLET, wallet.plus(points).encode())`와 `putString(KEY_CARD, card.encode())`를 **같은 `commit()`** 으로 넣는다. 카드가 null이면 지갑과 카드 키는 건드리지 않는다. `trips`는 기존 `repository.getAllTrips(profile.leagueKey)`를 재사용한다. 반환값 의미는 그대로 둔다.

시연 값: 골드, 640km, 평균 86.4, 가산 30, 배율 150, 1,248P, 잔액 3,420P, 메시지 `10월 시즌 결과 · 골드 리그를 지켰어요. 이번 달엔 승급권에 도전해 봐요`, 쿠폰 1장 `service_10k`(발급일 `DemoData.today`).

- [ ] **Step 4: 통과 확인** — 전체 `testDebugUnitTest`, Expected: PASS

- [ ] **Step 5: 커밋** — `feat: 시즌 정산에 포인트 지급과 정산 카드 저장 추가`

---

### Task 4: 시상대 정산 화면과 연결

**Files:**
- Create: `ui/SettlementScreen.kt`, `res/layout/view_settlement.xml`
- Modify: `ui/MainActivity.kt`, `res/layout/activity_main.xml`
- Test: `app/src/test/.../ui/SettlementRouteTest.kt`

**Interfaces:**
- Consumes: `SeasonSettlementStore.card/clearCard`, `DemoData.settlementCard`, `DemoData.startWallet`, `TextView.countUp`, `Pose.WIN`
- Produces:
  - `Screen.SETTLEMENT` (`포인트 상점 보기` 버튼은 Task 5가 연결할 때까지 숨긴다)
  - `internal fun redirectToSettlement(target: Screen, hasCard: Boolean, demoMode: Boolean): Boolean`
  - `MainActivity`: `internal var demoWallet: Wallet`, `internal val wallet: Wallet`, `internal fun saveWallet(w: Wallet): Boolean`, `internal val walletStore`, `internal fun showSettlement()`

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
@Test fun redirectsOnlyRealLeagueWithCard() {
    assertTrue(redirectToSettlement(Screen.LEAGUE, hasCard = true, demoMode = false))
    assertFalse(redirectToSettlement(Screen.LEAGUE, hasCard = false, demoMode = false))
    assertFalse(redirectToSettlement(Screen.LEAGUE, hasCard = true, demoMode = true))
    assertFalse(redirectToSettlement(Screen.WIN, hasCard = true, demoMode = false))
    assertFalse(redirectToSettlement(Screen.DRIVING, hasCard = true, demoMode = false))
}
```

- [ ] **Step 2: 실패 확인** — Expected: 컴파일 FAIL

- [ ] **Step 3: 연결 구현 (`MainActivity`)**

`show()` 맨 앞, `restricted` 검사 뒤에서 `redirectToSettlement(...)`가 참이면 `target`을 `Screen.SETTLEMENT`로 바꾼다. 앱 시작, 주차, 결과 카드의 리그 버튼이 모두 `show(Screen.LEAGUE)`를 지나므로 다른 곳은 고치지 않는다.

`demoWallet`은 `DemoData.startWallet()`으로 시작하고 시연 모드를 켤 때와 시연 바의 새 버튼 `btn_settlement`("시즌 정산")를 누를 때 초기화한다. `wallet`은 `demoMode`면 `demoWallet`, 아니면 `walletStore.load()`다.

- [ ] **Step 4: 화면 구현 (`SettlementScreen.kt`, `view_settlement.xml`)**

시안 `settlement-layout.html` A안을 1280×720dp로 옮긴다. 위 체커 띠, 왼쪽에 `시즌 정산` 제목과 티어, 큰 `+1,248P`, 배율 태그, 거리·가산 줄, 내 포인트, 버튼 2개. 오른쪽에 `RabbitView`(`Pose.WIN`). `seasons > 1`이면 태그와 거리·가산 줄을 숨긴다. `promotedTo`가 있으면 태그 자리에 `플래티넘 리그로 승급!`을 쓰고 배율은 거리 줄 뒤에 붙인다. `bonusPercent == 0`이면 격려 문구 `다음 달엔 보너스에 도전해 봐요`를 쓴다.

숫자는 `countUp`, 소리는 `requestSound()`가 참일 때 승리 연출과 같은 `Fx`를 쓴다. 두 버튼 모두 실제 모드에서 `seasonStore.clearCard()`를 먼저 부른다. `리그로 가기`는 `show(Screen.LEAGUE)`.

- [ ] **Step 5: 테스트와 빌드** — `testDebugUnitTest`, `assembleDebug`, Expected: 둘 다 성공

- [ ] **Step 6: 커밋** — `feat: 시상대 정산 화면과 시연 버튼 추가`

---

### Task 5: 토끼 피트샵

**Files:**
- Create: `ui/ShopScreen.kt`, `res/layout/view_shop.xml`, `res/drawable-nodpi/shop_garage.png`, `design/shop/garage-source.png`
- Modify: `ui/MainActivity.kt`, `ui/LeagueScreen.kt`, `ui/SettlementScreen.kt`, `res/layout/view_league.xml`

**Interfaces:**
- Consumes: `ShopCatalog`, `Wallet`, `wallet`, `saveWallet`, `Pose.WELCOME`
- Produces: `internal fun MainActivity.showShop()`, `internal var shopTab: ShopTab` (`enum class ShopTab { SHOP, COUPONS }`), `internal val shopPowertrain: Powertrain` (시연은 EV, 실제는 `profileStore.load()?.powertrain ?: EV`), `internal fun itemRow(...)`

- [ ] **Step 1: 에셋 준비**

프로젝트 폴더의 `.forge/shop-bg/out1/generated.png`를 `design/shop/garage-source.png`로 복사한다. 앱용은 800×800으로 축소한 `res/drawable-nodpi/shop_garage.png`.

- [ ] **Step 2: 화면 구현**

시안 `shop-counter.html`을 옮긴다. 왼쪽 약 610dp 폭 안에 배경 `ImageView`, 간판, `RabbitView`(`Pose.WELCOME`, 크게), 말풍선, 그 위 z순서로 카운터. 카운터 앞면에 대상 쿠폰 이름, `3,420 / 5,000P`, 진행 막대. 오른쪽에 `상점`·`쿠폰함 n` 탭과 상품 줄, 안내 문구 `시연용 상품입니다. 실제 쿠폰은 발급되지 않아요.`

진입 때 `wallet.withoutUnavailableGoal(items)`가 지갑과 다르면 저장한다. 표시 대상은 `wallet.target(items)`, 말풍선은 `ShopCatalog.talk(...)`. 상품 줄을 누르면 잔액이 모자랄 때 `withGoal`로 목표를 바꾸고 다시 그린다. 잔액이 충분하면 Task 6의 확인 패널을 연다(이 Task에서는 목표 변경만 연결).

- [ ] **Step 3: 입구 연결**

`view_league.xml`의 `tier_bar` 아래에 `btn_shop`(높이 76dp, `내 포인트 3,420P · 상점`)을 넣는다. `statsBlock`과 겹치면 `tier_bar` 여백을 줄이지 말고 버튼을 `stats_block` 위 늘어나는 여백 안에 둔다. 정산 화면의 `포인트 상점 보기`도 `show(Screen.SHOP)`으로 연결한다. `Screen` enum에 `SHOP`을 더하고 `show()`의 `when`에 넣는다. 정산 화면의 버튼 숨김을 푼다.

- [ ] **Step 4: 빌드와 화면 확인** — `assembleDebug` 성공, 에뮬레이터 시연 모드에서 시즌 정산 → 상점 → 리그 왕복, 토끼 하반신이 카운터에 가려지는지 스크린샷으로 확인

- [ ] **Step 5: 커밋** — `feat: 토끼 피트샵 화면과 입구 추가`

---

### Task 6: 교환과 쿠폰함

**Files:**
- Create: `ui/CouponBox.kt`, `ui/view/DemoBarcodeView.kt`
- Modify: `ui/ShopScreen.kt`, `res/layout/view_shop.xml`
- Test: `app/src/test/.../ui/view/DemoBarcodeTest.kt`

**Interfaces:**
- Consumes: `Wallet.exchange`, `saveWallet`, `ShopCatalog.item`, `Coupon.isExpired`, `Pose.WIN`
- Produces: `internal fun barcodeBars(id: String, count: Int = 40): List<Int>`, `class DemoBarcodeView`

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
@Test fun barsAreStableForId() {
    assertEquals(barcodeBars("a"), barcodeBars("a"))
    assertNotEquals(barcodeBars("a"), barcodeBars("b"))
    assertEquals(40, barcodeBars("a").size)
    assertTrue(barcodeBars("a").all { it in 1..4 })
}
```

- [ ] **Step 2: 실패 확인** — Expected: 컴파일 FAIL

- [ ] **Step 3: 구현**

바코드는 `id.hashCode()`를 시드로 한 `java.util.Random`으로 막대 폭을 만든다. 뷰는 막대 위에 `시연용` 글자를 겹쳐 그린다.

확인 패널은 `view_shop.xml` 안의 전체 덮개 뷰로 둔다. 상품 이름, 가격, 교환 뒤 잔액, `교환하기`, `취소`. `교환하기`를 누르면 버튼을 즉시 비활성화하고 `wallet.exchange(item, today)`를 부른다. 결과가 null이거나 `saveWallet`이 false면 말풍선에 `지금은 교환할 수 없어요`를 띄우고 잔액을 바꾸지 않는다. 성공하면 토끼를 `Pose.WIN`으로 바꾸고 말풍선 `축하해요! 쿠폰함에 넣어 뒀어요`, 짧은 연출 뒤 쿠폰함 탭을 연다. `today`는 시연이면 `DemoData.today`, 아니면 `LocalDate.now()`.

쿠폰함은 쿠폰별로 이름, 금액, `yyyy.MM.dd까지`, `DemoBarcodeView`를 보여 주고 만료 쿠폰은 회색과 `기간 만료`로 표시한다. 쿠폰이 없으면 `아직 받은 쿠폰이 없어요`.

- [ ] **Step 4: 테스트와 빌드** — 전체 `testDebugUnitTest`, `assembleDebug`, Expected: 성공

- [ ] **Step 5: 커밋** — `feat: 쿠폰 교환과 쿠폰함 추가`

---

### Task 7: 실기 확인과 기록

**Files:**
- Modify: `docs/app-dev-log.md`, `README.md`; 저장소 밖 `AGENTS.md`, `project-notes.md`

- [ ] **Step 1: 시연 흐름 확인**

에뮬레이터(GPU host, cold boot)에서: 시연 모드 → 시즌 정산 `+1,248P`·`10월 시즌` → 상점 3,420P → 충전 크레딧 교환 → 2,420P·쿠폰함 2장 → 시즌 정산 다시 누르면 3,420P·쿠폰 1장 → 시연 끄면 실제 잔액. 빨강이 새로 쓰이지 않고 글자가 잘리지 않는지 스크린샷으로 본다.

- [ ] **Step 2: 실제 정산 확인**

`cmd alarm set-time`으로 달을 넘겨 앱을 연다. 정산 화면 1회, 재시작해도 재지급 없음(`run-as ... --user 10`으로 `vehicle_profile.xml` 잔액 확인), `리그로 가기` 뒤 다시 안 뜸, 승급 경우의 문구. 끝나면 시간을 되돌린다. `animator_duration_scale`은 `settings put ... 1`로 되돌린다.

- [ ] **Step 3: 주행 제약 확인**

Virtual Console로 상점과 정산 화면에서 주행을 시작해 주행 중 화면으로 바뀌는지, 주차 뒤 결과 카드 → 정산 순서로 뜨는지 확인한다. 앱 충돌 0건을 logcat으로 확인한다.

- [ ] **Step 4: 기록과 커밋**

`app-dev-log.md`에 구현과 확인 결과, 테스트 수를 적고 `README.md` 구현 현황을 고친다. `AGENTS.md`의 현재 상태 줄과 `project-notes.md`를 갱신한다. 커밋 `docs: 포인트 정산과 상점 기록`. 사용자 확인 전에는 병합과 push를 하지 않는다.
