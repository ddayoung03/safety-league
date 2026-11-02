# 세이프티 리그 화면 시안 5장 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 설계 문서의 화면 시안 5장을 저장소 안 HTML로 만들고, 1280×720 PNG로 내보내 README 첫 화면에 보여 준다.

**Architecture:** 정적 HTML 5개가 공통 `tokens.css`(색·글꼴·기울기)와 `demo-data.js`(가상 운전자 데이터 + 순수 계산 함수)를 읽는다. 화면에 나오는 숫자는 전부 `demo-data.js`에서 계산해 넣으므로 시안끼리 숫자가 어긋날 수 없다. 계산 함수는 `node:test`로, 렌더 결과는 Chrome 헤드리스 `--dump-dom`으로 검사하고, 같은 Chrome으로 PNG를 찍는다.

**Tech Stack:** HTML/CSS, 브라우저 JS(클래식 스크립트, 모듈 아님: `file://`에서 ES 모듈은 막힘), Node 24 내장 `node:test`, Chrome 헤드리스 (`C:/Program Files/Google/Chrome/Application/chrome.exe`). 추가 설치 없음.

**Spec:** `docs/superpowers/specs/2026-11-03-screen-mockups-design.md`

## Global Constraints

- 화면 크기 1280×720 고정 (`body` 크기 그대로, 스크롤 없음).
- 색: 아스팔트 `#24272C`, 아스팔트 짙음 `#1E2125`, 타워 칸 `#2E3238`, 트랙 화이트 `#F2F1EC`, 회색 글자 `#9BA1A8`, 차선 노랑 `#FFC52E`, 레이스 레드 `#E8323C`, 골드 티어 `#D4A23C`.
- 글꼴: 한글 Black Han Sans, 숫자 Saira Condensed 이탤릭 800~900 (Google Fonts). 띠·탭·순위 줄은 `skewX(-14deg)`, 안의 글자는 `skewX(14deg)`로 폄.
- 노랑은 "지금 노려야 할 것"에만. 아쉬운 날 화면에는 빨강 금지. 아이콘은 선 SVG, 이모지 금지.
- 주행 중 화면: 점수·순위·감점 표시 금지, 애니메이션 금지.
- 문구 톤: 짧고 친근한 존댓말, 훈계 금지. 교통사고 통계 금지.
- 실명·연락처·이메일·소속 금지. 가상 운전자는 별명만 (예: 깜빡이장인, 해운대드라이버). 내 줄은 "나".
- 모든 숫자는 스펙 7절 표와 일치.

## Review Focus

1. Google Fonts가 늦게 오거나 실패한 채 PNG가 찍힘 → 기본 글꼴로 찍힌 PNG가 커밋되면 안 됨. 내보내기 전에 `document.fonts.ready`를 기다리고, 실패 시 export가 에러로 끝나야 함 (Task 7).
2. 긴 별명이나 문구가 1280×720 밖으로 넘치거나 잘림 → 넘침 없이 말줄임. 렌더 검사에서 `scrollWidth/scrollHeight`가 1280/720을 넘지 않는지 확인 (Task 2의 공통 검사).
3. 스펙 숫자가 화면마다 어긋남 → 모든 숫자는 `DEMO` 계산값으로만 채우고, 렌더 테스트가 화면별 기대값을 확인 (Task 2~6).
4. 개인정보가 시안·README·커밋에 섞임 → 실명·연락처 문자열이 `mockups/`와 `README.md`에 없어야 함 (Task 7).
5. `file://`로 열었을 때 스크립트가 안 돌아 빈 칸이 찍힘 → 클래식 `<script src>`만 사용, 렌더 테스트가 빈 값(`undefined`, `NaN`, 빈 문자열) 없음을 확인 (Task 2의 공통 검사).

---

## File Structure

```
mockups/
  tokens.css          공통 색·글꼴·기울기 유틸
  demo-data.js        가상 데이터 + 순수 계산 함수 (브라우저 전역 DEMO, Node에서는 module.exports)
  result-win.html     결과 카드 (이긴 날, 11/7 금)
  result-low.html     결과 카드 (아쉬운 날, 별도 시나리오)
  league.html         리그 순위 (11/9 일)
  driving.html        주행 중 (11/7 금 주행 중)
  report.html         주간 리포트 (11/9 일)
  export.js           Chrome 헤드리스로 PNG 5장 내보내기
  png/                내보낸 PNG (커밋함)
  test/
    demo-data.test.js 계산 함수 테스트
    render.test.js    화면별 렌더 검사 (Chrome --dump-dom)
    chrome.js         Chrome 경로 + dumpDom(file) 헬퍼
README.md             시안 5장 이미지 노출
```

출발점 HTML: 저장소 상위 폴더 `.superpowers/brainstorm/1402-1793622891/content/`의
`mood-a-v2.html`, `mood-a-low-v3.html`, `league-a-v4.html`, `driving-a-v6.html`, `report-a-v2.html`.
그 파일들은 0.75배로 축소해 보여 주는 래퍼(`.stage`, `.scr`, 바깥 `<h2>`, `.mockup`)를 갖고 있으니, 1280×720 본문(`.scr` 안쪽)만 가져온다.

테스트 실행: `node --test mockups/test/` (저장소 루트에서).

---

### Task 1: 가상 데이터와 계산 함수

**Files:**
- Create: `mockups/demo-data.js`
- Test: `mockups/test/demo-data.test.js`

**Interfaces:**
- Produces (브라우저 전역 `DEMO`, Node `require('../demo-data.js')`):
  - `DEMO.trips: Array<{date: 'YYYY-MM-DD', score: number, km?: number}>` — 하루에 여러 개 가능
  - `DEMO.ranks: Record<'YYYY-MM-DD', number>` — 그날 끝난 뒤 시즌 순위
  - `DEMO.league: {total: 397, tier: '골드', model: '아이오닉 5', region: '부산', season: {start:'2026-11-01', end:'2026-11-30'}}`
  - `DEMO.round1(x: number): number` — 소수 첫째 자리 반올림
  - `DEMO.seasonAverage(until: string): number` — 시즌 시작~`until`(포함) 트립 평균, round1
  - `DEMO.average(from: string, to: string): number` — 기간 트립 평균, round1
  - `DEMO.streakAt(date: string, threshold = 70): number` — 그날까지 연속 안전 주행 일수 (`streakAtFor(DEMO.trips, ...)`)
  - `DEMO.streakAtFor(trips, date: string, threshold = 70): number` — 같은 규칙을 임의 트립 목록에 적용 (앱 구현 때 Kotlin으로 옮길 규칙)
  - `DEMO.daysLeft(date: string): number` — 시즌 종료까지 남은 날 (date 다음 날부터 end까지)
  - `DEMO.promotionRank(total: number): number` — 승급 라인 순위 = `Math.round(total * 0.3)`
  - `DEMO.demotionRank(total: number): number` — 강등 구간 첫 순위 = `total - Math.round(total * 0.3) + 1`
  - `DEMO.neighbors(date: string): Array<{rank: number, name: string, avg: number}>` — 그 시점 순위표 줄 (내 줄은 `name: '나'`)

데이터 값 (스펙 7절 + 지난주·연속 기록을 맞추기 위한 10월 트립):

| 날짜 | 10/24 | 10/25 | 10/26 | 10/27 | 10/28 | 10/29 | 10/30 | 10/31 | 11/1 | 11/2 | 11/3 | 11/4 | 11/5 | 11/6 | 11/7 | 11/8 | 11/9 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 점수 | 66 | 80 | 81 | 82 | 85 | 84 | 86 | 86 | 88 | 89 | 84 | 88 | — | 90 | 92 (18.4km) | 86 | 93 |
| 순위 | | | | | | | | | | | 131 | 130 | 130 | 128 | 125 | 126 | 125 |

승급 라인 운전자(P119) 시즌 평균은 89.7로 고정 데이터에 둔다. 이웃 운전자 평균은 각 시점의 내 평균과 순위 순서가 맞게 정한다(위는 더 높고 아래는 더 낮음).

- [ ] **Step 1: 실패하는 테스트 작성** (`node:test` + `node:assert/strict`)

```js
const D = require('../demo-data.js');
test('season average', () => { assert.equal(D.seasonAverage('2026-11-07'), 88.5); assert.equal(D.seasonAverage('2026-11-09'), 88.8); });
test('weekly averages', () => { assert.equal(D.average('2026-11-03','2026-11-09'), 88.8); assert.equal(D.average('2026-10-27','2026-11-02'), 85.7); });
test('streak counts drive days >= 70, skips no-drive days', () => {
  assert.equal(D.streakAt('2026-11-06'), 12); assert.equal(D.streakAt('2026-11-07'), 13);
  assert.equal(D.streakAt('2026-11-09'), 15); assert.equal(D.streakAt('2026-11-05'), 11);
});
test('streak breaks under 70 and on any low trip that day', () => {
  assert.equal(D.streakAt('2026-10-24'), 0);
  // 같은 날 70점 이상 + 미만 트립이 섞이면 0
  assert.equal(D.streakAtFor([{date:'2026-12-01',score:90},{date:'2026-12-01',score:60}], '2026-12-01'), 0);
});
test('league lines', () => { assert.equal(D.promotionRank(397), 119); assert.equal(D.demotionRank(397), 279); assert.equal(D.daysLeft('2026-11-09'), 21); });
test('neighbors are sorted and include me', () => {
  for (const d of ['2026-11-07','2026-11-09']) {
    const rows = D.neighbors(d); const me = rows.find(r => r.name === '나');
    assert.equal(me.rank, D.ranks[d]); assert.equal(me.avg, D.seasonAverage(d));
    for (let i = 1; i < rows.length; i++) assert.ok(rows[i-1].avg >= rows[i].avg && rows[i-1].rank < rows[i].rank);
  }
});
```

- [ ] **Step 2: 실패 확인** — Run: `node --test mockups/test/` / Expected: FAIL, `Cannot find module '../demo-data.js'`

- [ ] **Step 3: `mockups/demo-data.js` 구현**

클래식 스크립트로 `const DEMO = {...}` 를 정의하고 끝에 `if (typeof module !== 'undefined') module.exports = DEMO;`.
`streakAt(date)`는 `streakAtFor(DEMO.trips, date)`를 부른다. `streakAtFor(trips, date, threshold=70)`: `date` 이하 날짜를 최신순으로, 날짜별로 묶어 한 트립이라도 `< threshold`면 멈추고, 아니면 1 더함. 운전 없는 날은 날짜 목록에 없으므로 자연히 건너뜀.

- [ ] **Step 4: 통과 확인** — Run: `node --test mockups/test/` / Expected: PASS (6 tests)

- [ ] **Step 5: Commit** — `git add mockups/demo-data.js mockups/test/demo-data.test.js && git commit -m "feat(mockups): demo data and score helpers"`

---

### Task 2: 공통 스타일, 렌더 검사 도구, 결과 카드(이긴 날)

**Files:**
- Create: `mockups/tokens.css`, `mockups/test/chrome.js`, `mockups/test/render.test.js`, `mockups/result-win.html`

**Interfaces:**
- Consumes: `DEMO` (Task 1)
- Produces:
  - `tokens.css`: CSS 변수 `--asphalt --asphalt-deep --cell --white --grey --yellow --red --gold`, 클래스 `.num`(숫자 글꼴), `.skew`(`skewX(-14deg)`), `.unskew`(`skewX(14deg)`), `body{margin:0;width:1280px;height:720px;overflow:hidden}`.
  - `chrome.js`: `CHROME`(경로, 환경변수 `CHROME`이 있으면 우선), `dumpDom(file: string): string` — `chrome --headless=new --disable-gpu --virtual-time-budget=10000 --dump-dom file:///<절대경로>` 결과.
  - `render.test.js` 공통 검사 `checkPage(file, expectedStrings[])`: dump에 각 문자열이 있고, `undefined`·`NaN`·`null`이 없으며, 페이지가 `data-overflow="none"`을 갖는다.
  - 모든 화면 HTML 공통 규칙: `<link rel="stylesheet" href="tokens.css">`, `<script src="demo-data.js"></script>`, 숫자는 `data-bind`가 붙은 요소에 인라인 스크립트가 `textContent`로 채움. 마지막에 `document.body.dataset.overflow = (document.documentElement.scrollWidth <= 1280 && document.documentElement.scrollHeight <= 720) ? 'none' : 'yes'`.

- [ ] **Step 1: 실패하는 렌더 테스트 작성**

```js
test('result-win', () => checkPage('result-win.html',
  ['오늘의 레이스 종료', '92', 'P125', '▲3', '승급 라인까지 6계단', '13일', '18.4km', '추월']));
```
그리고 `result-win.html`에 "승급"이라는 단어가 "승급 라인" 외에 없음을 확인: `assert.ok(!/플래티넘 승급|골드 졸업/.test(dump))`.

- [ ] **Step 2: 실패 확인** — Run: `node --test mockups/test/render.test.js` / Expected: FAIL (파일 없음)

- [ ] **Step 3: `tokens.css`, `chrome.js`, `result-win.html` 구현**

`result-win.html`은 출발점 `mood-a-v2.html`의 본문을 옮기되 스펙 6-1을 따른다: 빨간 띠는 "▲3 · 승급 라인까지 6계단", 통계는 연속 `DEMO.streakAt('2026-11-07')`일·18.4km, 순위 타워는 `DEMO.neighbors('2026-11-07')`(내 위 2명, 나, 아래 3명), 제친 3명(P126~128)에 "추월", 바로 위 사람과의 차이 문구.

- [ ] **Step 4: 통과 확인 + 눈으로 확인** — `node --test mockups/test/` PASS. Chrome으로 `result-win.html`을 열어 1280×720 안에 잘림 없이 보이는지 확인.

- [ ] **Step 5: Commit** — `git add mockups && git commit -m "feat(mockups): tokens, render checks, win result card"`

---

### Task 3: 결과 카드(아쉬운 날)

**Files:** Create `mockups/result-low.html` · Modify `mockups/test/render.test.js`

**Interfaces:** Consumes `tokens.css`, `checkPage` (Task 2). 이 화면은 별도 시나리오라 `DEMO` 계산값 대신 스펙 6-2의 고정값을 쓴다.

- [ ] **Step 1: 테스트 추가**
```js
test('result-low', () => { const dump = checkPage('result-low.html',
  ['64', '다음 주행 작전', '차선 바꿀 때 깜빡이부터 켜기', '깜빡이 없이 차선 변경 4회', '매너 +9점',
   '13일 → 0일', '다음 주행 70점이면 1일부터 다시', 'P126', '▼1', '목표', '다른 날의 예시']);
  assert.ok(!dump.includes('#E8323C') && !dump.includes('var(--red)')); });
```
- [ ] **Step 2: 실패 확인** — `node --test mockups/test/render.test.js` FAIL
- [ ] **Step 3: 구현** — 출발점 `mood-a-low-v3.html`. 화면 구석에 작게 "다른 날의 예시" 표기 (스펙 7절). 빨강 사용 금지.
- [ ] **Step 4: 통과 + 눈으로 확인**
- [ ] **Step 5: Commit** — `git commit -m "feat(mockups): low-score result card"`

---

### Task 4: 리그 순위

**Files:** Create `mockups/league.html` · Modify `mockups/test/render.test.js`

**Interfaces:** Consumes `DEMO.neighbors('2026-11-09')`, `DEMO.seasonAverage`, `DEMO.daysLeft`, `DEMO.promotionRank`, `DEMO.demotionRank`.

- [ ] **Step 1: 테스트 추가**
```js
test('league', () => checkPage('league.html',
  ['11월 시즌', '골드 리그', '21일 남음', 'P125', '397명', '승급 라인까지 6계단', '0.9점',
   '승급 라인', '상위 30%', '강등 라인', '하위 30%', '₩8,400', '3,120회', '아이오닉 5', '부산', '친구']));
```
- [ ] **Step 2: 실패 확인**
- [ ] **Step 3: 구현** — 출발점 `league-a-v4.html`, 스펙 6-3. "0.9점"은 `round1(89.7 - DEMO.seasonAverage('2026-11-09'))`, "6계단"은 `DEMO.ranks['2026-11-09'] - DEMO.promotionRank(397)`로 계산해 넣는다.
- [ ] **Step 4: 통과 + 눈으로 확인**
- [ ] **Step 5: Commit** — `git commit -m "feat(mockups): league standings"`

---

### Task 5: 주행 중 화면

**Files:** Create `mockups/driving.html` · Modify `mockups/test/render.test.js`

**Interfaces:** Consumes `DEMO.streakAt('2026-11-06')` (주행 중이므로 그 전날까지의 기록 + 1이 목표).

- [ ] **Step 1: 테스트 추가**
```js
test('driving', () => { const dump = checkPage('driving.html',
  ['주행 기록 중', '오늘의 레이스 진행 중', '결과는 주차하면 공개돼요', '3km부터 기록 인정',
   '11.2', '18.4km', '해운대', '70점 넘기면 연속 13일째']);
  assert.ok(!/P\d{2,3}|점수|감점|순위/.test(dump.replace('70점 넘기면', ''))); // 점수·순위 표시 금지
  assert.ok(!/@keyframes|animation:/.test(dump)); });
```
- [ ] **Step 2: 실패 확인**
- [ ] **Step 3: 구현** — 출발점 `driving-a-v6.html`, 스펙 6-4. 트랙 칸 수 = `Math.ceil(18.4)` = 19칸, 노란 칸 = `Math.floor(11.2)` = 11칸, 자동차 아이콘은 11번째 칸 끝. 실루엣 SVG는 출발점 파일의 `.sil`을 그대로 쓰고 투명도 0.15~0.18.
- [ ] **Step 4: 통과 + 눈으로 확인**
- [ ] **Step 5: Commit** — `git commit -m "feat(mockups): driving screen"`

---

### Task 6: 주간 리포트

**Files:** Create `mockups/report.html` · Modify `mockups/test/render.test.js`

**Interfaces:** Consumes `DEMO.average`, `DEMO.trips`, `DEMO.ranks`, `DEMO.promotionRank`.

- [ ] **Step 1: 테스트 추가**
```js
test('report', () => checkPage('report.html',
  ['11월 2주차 리포트', '11.03 ~ 11.09', '주행 6일', '88.8', '지난주보다 +3.1', '지난주 85.7',
   '84', '88', '휴식', '90', '92', '86', '93', 'P131 → P125 ▲6', '승급 라인', '다음 주 작전']));
```
- [ ] **Step 2: 실패 확인**
- [ ] **Step 3: 구현** — 출발점 `report-a-v2.html`, 스펙 6-5. 막대와 포지션 차트 좌표는 `DEMO`에서 계산 (막대 높이 ∝ 점수, 차트 y ∝ 순위). 가장 높은 날(일 93)만 노랑, 순위가 가장 많이 오른 날(금 +3) 강조.
- [ ] **Step 4: 통과 + 눈으로 확인**
- [ ] **Step 5: Commit** — `git commit -m "feat(mockups): weekly report"`

---

### Task 7: PNG 내보내기, README, 개인정보 검사

**Files:** Create `mockups/export.js`, `mockups/png/*.png` · Modify `README.md`, `mockups/test/render.test.js`

**Interfaces:** Consumes `CHROME` (Task 2).
- Produces: `node mockups/export.js` → `mockups/png/1-result-win.png`, `2-result-low.png`, `3-league.png`, `4-driving.png`, `5-report.png` (각 1280×720).

- [ ] **Step 1: 테스트 추가**
```js
test('png exports are 1280x720', () => { for (const f of PNGS) { const b = fs.readFileSync(f);
  assert.equal(b.readUInt32BE(16), 1280); assert.equal(b.readUInt32BE(20), 720); } });
test('no personal info', () => { for (const f of [...htmlFiles, 'README.md'])
  assert.ok(!/PERSONAL_INFO_WORDS/.test(fs.readFileSync(f, 'utf8')), f); });
test('fonts loaded before export', () => { for (const p of pages)
  assert.match(dumpDom(p), /data-fonts="loaded"/); });
```
그리고 각 HTML의 마지막 스크립트에 `document.fonts.ready.then(() => { document.body.dataset.fonts = [...document.fonts].some(f => f.family.includes('Black Han Sans') && f.status === 'loaded') ? 'loaded' : 'missing'; })` 추가.

- [ ] **Step 2: 실패 확인** — `node --test mockups/test/` FAIL (PNG 없음)
- [ ] **Step 3: `export.js` 구현** — 각 HTML마다 `dumpDom`으로 `data-fonts="loaded"`를 먼저 확인하고 아니면 에러로 종료. 그다음 `chrome --headless=new --disable-gpu --hide-scrollbars --virtual-time-budget=10000 --window-size=1280,720 --screenshot=<png> file:///<html>`.
- [ ] **Step 4: 내보내기 실행 + 테스트 통과** — Run: `node mockups/export.js && node --test mockups/test/` / Expected: PNG 5개 생성, 전체 PASS. PNG 5장을 열어 글꼴이 Black Han Sans로 나왔는지 눈으로 확인.
- [ ] **Step 5: README 갱신** — 맨 위에 시안 5장(`mockups/png/*.png`)을 순서대로 넣고, 각 이미지 아래 한 줄 설명. 기존 점수 구성표는 그 아래로.
- [ ] **Step 6: Commit** — `git add mockups README.md && git commit -m "feat(mockups): PNG export and README showcase"`

push와 저장소 공개 전환은 이 계획에 포함하지 않는다 (사용자 확인 후 별도로).
