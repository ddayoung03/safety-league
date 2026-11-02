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

test('driving', () => {
  const dump = checkPage('driving.html',
    ['주행 기록 중', '오늘의 레이스 진행 중', '결과는 주차하면 공개돼요', '3km부터 기록 인정',
     '11.2', '18.4km', '해운대', '70점 넘기면 연속 13일째']);
  const text = dump.replace(/<(script|style)[\s\S]*?<\/\1>/g, '').replace(/<[^>]+>/g, '').replace('70점 넘기면', '');
  assert.ok(!/P\d{2,3}|점수|감점|순위|\d+점/.test(text), '주행 중 화면에 점수·순위 표시');
  assert.ok(!/@keyframes|animation:/.test(dump), '주행 중 화면에 애니메이션');
});

test('report', () => checkPage('report.html',
  ['11월 2주차 리포트', '11.03 ~ 11.09', '주행 6일', '88.8', '지난주보다 +3.1', '지난주 85.7',
   '84', '88', '휴식', '90', '92', '86', '93', 'P131 → P125 ▲6', '승급 라인', '다음 주 작전']));

module.exports = { checkPage };

test('overflow is measured with the real fonts, not the fallback', () => {
  assert.match(dumpDom('test/fixtures/fits-after-fonts.html'), /data-overflow="none"/);
});

test('text clipped with the real fonts is still flagged', () => {
  assert.match(dumpDom('test/fixtures/clipped.html'), /data-overflow="yes"/);
});
