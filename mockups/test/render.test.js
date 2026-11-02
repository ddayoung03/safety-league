const test = require('node:test');
const assert = require('node:assert/strict');
const { dumpDom } = require('./chrome.js');

// 모든 화면 공통: 기대 문구가 있고, 빈 값이 없고, 1280×720 밖으로 넘치지 않는다.
function checkPage(file, expected) {
  const dump = dumpDom(file);
  for (const s of expected) assert.ok(dump.includes(s), `${file}: "${s}" 없음`);
  const text = dump.replace(/<script[\s\S]*?<\/script>/g, '');
  assert.ok(!/\b(undefined|NaN|null)\b/.test(text), `${file}: 빈 값이 화면에 찍힘`);
  assert.match(dump, /data-overflow="none"/, `${file}: 1280×720 밖으로 넘침`);
  return dump;
}

test('result-win', () => {
  const dump = checkPage('result-win.html',
    ['오늘의 레이스 종료', '92', 'P125', '▲3', '승급 라인까지 6계단', '13일', '18.4km', '추월']);
  assert.ok(!/플래티넘 승급|골드 졸업/.test(dump));
});

module.exports = { checkPage };
