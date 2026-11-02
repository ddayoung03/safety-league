const test = require('node:test');
const assert = require('node:assert/strict');
const { dumpDom } = require('./chrome.js');

// 모든 화면 공통: 기대 문구가 있고, 빈 값이 없고, 1280×720 밖으로 넘치지 않는다.
function checkPage(file, expected) {
  const dump = dumpDom(file);
  // 화면에 보이는 글자만 남긴다 (태그 사이를 이어 붙임).
  const text = dump.replace(/<(script|style)[\s\S]*?<\/\1>/g, '').replace(/<[^>]+>/g, '');
  for (const s of expected) assert.ok(text.includes(s), `${file}: "${s}" 없음`);
  assert.ok(!/\b(undefined|NaN|null)\b/.test(text), `${file}: 빈 값이 화면에 찍힘`);
  assert.match(dump, /data-overflow="none"/, `${file}: 1280×720 밖으로 넘침`);
  return dump;
}

test('result-win', () => {
  const dump = checkPage('result-win.html',
    ['오늘의 레이스 종료', '92', 'P125', '▲3', '승급 라인까지 6계단', '13일', '18.4km', '추월']);
  assert.ok(!/플래티넘 승급|골드 졸업/.test(dump));
});

test('result-low', () => {
  const dump = checkPage('result-low.html',
    ['64', '다음 주행 작전', '차선 바꿀 때 깜빡이부터 켜기', '깜빡이 없이 차선 변경 4회', '매너 +9점',
     '13일 → 0일', '다음 주행 70점이면 1일부터 다시', 'P126', '▼1', '목표', '다른 날의 예시']);
  assert.ok(!dump.includes('#E8323C') && !dump.includes('var(--red)'), '아쉬운 날에 빨강 사용');
});

test('league', () => checkPage('league.html',
  ['11월 시즌', '골드 리그', '21일 남음', 'P125', '397명', '승급 라인까지 6계단', '0.9점',
   '승급 라인', '상위 30%', '강등 라인', '하위 30%', '₩8,400', '3,120회', '아이오닉 5', '부산', '친구']));

module.exports = { checkPage };
