# 디자인 어셋 목록

새 디자인 작업의 시작점이다. 새로 만드는 어셋은 이 폴더에 넣고, 아래 표에 한 줄을 추가한다.
실명·연락처·이메일·소속은 넣지 않는다.

## 이 폴더

| 위치 | 내용 | 상태 |
|---|---|---|
| `prototypes/motion-hybrid.html` | 결과 화면 B·D·E 비교, 움직임 포함 | D 타이밍 타워 선택 |
| `prototypes/motion-directions.html` | 첫 비교 A·B·C | 참고용 |
| `prototypes/character-concepts.html` | 레이싱 슈트 캐릭터 후보 4종, 동작 6종 | 토끼 레이서 선택 |
| `previews/` | 위 시안의 캡처 PNG | 참고용 |
| `generated/` | 외부 도구로 만든 이미지. 생성할 때 만든다 | 비어 있음 |
| `sound/make_fx.py` | 결과 연출 효과음 4종(`app/src/main/res/raw/fx_*.wav`)을 파이썬 표준 라이브러리로 합성하는 스크립트. 사인과 잡음, 외부 음원 없음 | 사용 중 |

HTML은 브라우저에서 바로 열린다. 글꼴은 CDN에서 받으므로 인터넷이 필요하다.

## 다른 곳에 있는 어셋

경로를 참조하는 코드와 테스트가 있어서 옮기지 않았다.

| 위치 | 내용 |
|---|---|
| `../mockups/` | 제출용 화면 시안 v2 HTML |
| `../mockups/png/` | 제출용 시안 PNG |
| `../mockups/character/` | 현재 캐릭터 PNG와 원본 |
| `../app/src/main/res/drawable/` | 앱 벡터 리소스 |
| `../app/src/main/res/drawable-nodpi/` | 앱 캐릭터 이미지 |
| `../app/src/main/res/font/` | 앱 글꼴 |
| `../docs/app-icon.svg` | 앱 아이콘 |

## 이미지 생성 기록

`generated/`에 넣을 때 파일 이름, 사용 도구, 프롬프트를 여기에 남긴다.

| 파일 | 도구 | 프롬프트 |
|---|---|---|
| `generated/rabbit-chatgpt-sheet-1.png` | ChatGPT | 어린 토끼 레이서, 빨간 오픈페이스 헬멧, 헬멧을 뚫은 긴 귀, 노란 슈트, 3D 토온, 네 포즈 |
| `generated/rabbit-flow-1.*` | Google Flow (Nano Banana 2.1) | 같은 설정, 셀 셰이딩 한 포즈 |
| `generated/rabbit-pixel-sheet-1.png` | ChatGPT | 같은 토끼의 96x96 픽셀 아트, 네 포즈 |
| `generated/rabbit-flow-video-1.mp4` | Google Flow (Omni 1.1 Flash) | `rabbit-a-jump.png`에서 4초 루프, 깡충 뛰며 깃발 흔들기 |
