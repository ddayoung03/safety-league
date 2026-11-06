# 코드 지도

작업 시작 때 이 파일부터 보고, 필요한 파일만 `rg`로 찾는다. 패키지 경로: `app/src/main/java/io/github/ddayoung03/safetyleague/`

## 화면 (레이아웃 / 시안)
| 화면 | 레이아웃 `app/src/main/res/layout/` | 시안 `mockups/` |
|---|---|---|
| 이긴 날 결과 | `view_result_win.xml` | `result-win.html`, `png/1-result-win.png` |
| 아쉬운 날 결과 | `view_result_low.xml` | `result-low.html`, `png/2-result-low.png` |
| 리플레이 | `view_replay.xml` | `replay.html` |
| 리그 | `view_league.xml` | `league.html` |
| 주행 중 | `view_driving.xml` | `driving.html?w=sun\|rain\|snow\|night` |
| 주간 리포트 | `view_report.xml` | `report.html` |
| 차종 선택 | `view_vehicle_selection.xml` | 없음 |
| 배치 | `view_league_placement.xml` | 없음 |
| 위치 동의 | `view_location_consent.xml` | 없음 |

화면 전환과 값 연결은 `ui/MainActivity.kt`, 틀은 `activity_main.xml`. 색 기준은 `res/values/colors.xml`, 시각·움직임 규칙은 `docs/superpowers/specs/2026-11-06-ui-motion-redesign-design.md`.

## 코드
- `ui/view/`: 직접 그린 View와 Drawable
  - 결과: `CrewEntrance`(움직임 허용 판단), `StartLightsView`(출발 신호등), `SpeedLinesView`(속도선)
  - 토끼: `RabbitPose`(동작표·프레임·들썩임·배율 순수 함수), `RabbitView`(그리기와 동작), `RabbitHeadView`(팀 라디오 얼굴). 그림은 `res/drawable-nodpi/rabbit_*.png`, 원본은 `design/rabbit-forge/`
  - 경로·차트: `CircuitRouteView`, `ReplayEventIcon`, `PositionChartView`
  - 주행: `DrivingTrackView`, `DrivingTrackState`, `WeatherAmbientView`
  - 공용: `DesignCanvas`, `SkewDrawable`, `FlagLoaderView`
- `ui/motion/`: `MotionTokens`, `MotionScope`, `ScreenTransition`, `ResultMotion`(결과 연출 판단·순서기·이긴 날과 아쉬운 날 연출), `CountUp`(숫자 카운트), `SoundFx`·`SoundPolicy`(효과음)
- `ui/ScreenStyle.kt`: 화면 공용 도우미. `skew`, `applySkew`, `skewButton`(기울어진 버튼과 눌림)
- `weather/`: `WeatherState`(날씨 판정·움직임 허용 순수 함수), `VehicleWeatherSource`(차량 신호)
- `service/TripTrackingService.kt`: 주행 백그라운드 수집
- `core/`: 점수·리그·수집·데이터. **디자인 작업에서는 고치지 않는다.**
  - `calculator/`, `collector/`, `data/DemoData.kt`, `model/`, `repository/`

## 테스트
- 단위 테스트: `app/src/test/java/...`. 날씨는 `weather/WeatherStateTest`, 움직임 허용은 `ui/view/CrewEntranceTest`, 토끼는 `ui/view/RabbitPoseTest`, 리플레이 경로 진행은 `ui/view/RouteProgressTest`, 리포트 순위 선 진행은 `ui/view/PositionChartTest`.
- 작은 UI 수정은 묶어서 묶음 끝에 `.\gradlew.bat testDebugUnitTest assembleDebug -q`를 한 번 돌린다. 로직을 바꿨으면 그 항목에서 바로 돌린다.
- 시안 테스트: `node --test mockups/test/demo-data.test.js mockups/test/export.test.js mockups/test/render.test.js`

## 산출물
- 캡처·빌드 로그는 `captures/<작업이름>/`(git 제외)에 둔다. 저장소 최상위에 로그를 만들지 않는다.
