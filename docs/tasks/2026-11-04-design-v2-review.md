# Claude 검토: codex/design-v2 (2026-11-04 03:40)

기준: 커밋 484bd8c..4d9b9d3 (9개), 캡처 `safety-league-codex/captures/design-v2/` (03:19~03:21, 마지막 커밋 이후).
확인: `testDebugUnitTest assembleDebug` 통과(미커밋 WeatherStateTest 변경 제외). `core/` 변경 없음, 하드코딩 색 없음, 24sp 미만 글씨 추가 없음, 주행 중 화면 캐릭터 없음.

## 고칠 것 (순서대로, 항목마다 커밋)
1. **[P1] 이긴 날: 디버그 오류 패널이 사용자 화면에 노출** — 오른쪽 아래 "차량 신호 연결 문제 / WINDSHIELD_WIPERS_STATE: unsupported or permission denied / HEADLIGHTS_STATE …"가 결과 카드 위에 떠서 "리플레이 보기" 버튼을 가림. 신호 미지원은 로그(Logcat·app-dev-log)로만 남기고, 화면에선 조용히 시연 값/맑음으로 대체. 어느 화면에도 오류 문구를 띄우지 말 것.
2. **[P1] 이긴 날: 불꽃 입자가 글자 위에 겹침** — "▲3" 칩과 "승급 라인까지 6계단" 위로 주황·노랑 원이 지나감. 불꽃 제외 영역을 점수 숫자뿐 아니라 이 화면의 모든 글자·칩·말풍선·버튼으로 넓힐 것.
3. **[P1] 이긴 날: 캐릭터 위치** — 시안(`mockups/result-win.html` `.crew.c1`: left 444, bottom 24, height 350, 1280 기준)은 왼쪽 열(말풍선)과 순위 타워 사이. 지금은 타워 아래·리플레이 버튼 쪽으로 치우쳐 버튼을 가림. 시안 좌표대로, 리플레이 버튼과 겹치지 않게.
4. **[P1] 아쉬운 날: 캐릭터가 제목·점수를 덮음** — 헬멧이 "오늘의 레이스 종료" 글자와 "64"를 가림. 시안 `.crewbox`(left 436, top 70, 221×380)처럼 점수 오른쪽, 제목 아래에서 시작. 피트보드가 내 순위 줄(P126)과 닿지 않게.
5. **[P2] 리포트: 차트 축 글꼴** — 요일(월~일), P122~P134, "승급 라인 P119"가 다른 굵은 글꼴로 보임(앱 글꼴이 아님). 축 숫자는 Saira 기울임, 요일·라벨은 Pretendard로. "승급 라인" 라벨은 시안처럼 점선 오른쪽 위.
6. **[P2] 리플레이: 사건 아이콘 없음** — 경로 위 점과 오른쪽 목록에 시안의 아이콘(깜빡이 화살표·급제동 !·급가속 ≫, `mockups/page.js` EVENT_ICON)이 없음. VectorDrawable로 옮겨 둘 다에 표시.
7. **[P2] 미완료 테스트** — `WeatherStateTest.sunTransitionsAnimateButParkedSunDoesNotLoop`(미커밋, 5인자 `shouldAnimateWeather`)를 마저 구현하거나, 의도와 다르면 지우고 로그에 이유.
8. **[P2] 정차 판정** — `isAmbientMotionAllowed`가 `speedMps == 0f` 정확 비교. 정차 중 센서 잡음(0.0x m/s)에도 정지로 보도록 작은 임계값(예: < 0.3 m/s) + 테스트.
9. **[P3] 캡처 누락** — 주행 중 화면 캡처가 없음. 주행 중 맑음/비/눈/밤 4장(+정차 중/주행 중 움직임 여부)과 리그를 `captures/design-v2/`에 추가하고 시안과 나란히 비교.
10. **[P3] 작업 기록 누락** — `docs/app-dev-log.md`에 v2 항목(1~8, 10) 기록이 없음. 커밋별 한 일·결정·사용 신호 이름(WINDSHIELD_WIPERS_STATE, ENV_OUTSIDE_TEMPERATURE, HEADLIGHTS_STATE, checkWiperStateCapabilities, checkLowBeamLightStateCapabilities)과 에뮬레이터 지원 여부를 적을 것.
11. 그다음 원래 지시서의 9번(캐릭터 등장 연출)과 a·b·c(비교 반복, 자체 검토, lint).

규칙은 그대로: main 커밋·push 금지, `core/` 변경 금지, 개인정보 금지. 끝나면 로그 맨 아래 「아침 확인용 요약」.
