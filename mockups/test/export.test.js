const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { dumpDom, MOCKUPS } = require('./chrome.js');
const { PAGES } = require('../export.js');

const ROOT = path.join(MOCKUPS, '..');

test('png exports are 1280x720', () => {
  for (const [, png] of PAGES) {
    const b = fs.readFileSync(path.join(MOCKUPS, 'png', png));
    assert.equal(b.readUInt32BE(16), 1280, png);
    assert.equal(b.readUInt32BE(20), 720, png);
  }
});

// 찾을 문자열(실명·연락처 등)은 저장소 밖 ../personal-info.txt에 한 줄씩 둔다. 저장소에 개인정보를 넣지 않기 위해서다.
test('no personal info in mockups or README', (t) => {
  const listFile = path.join(ROOT, '..', 'personal-info.txt');
  if (!fs.existsSync(listFile)) return t.skip('personal-info.txt 없음');
  const words = fs.readFileSync(listFile, 'utf8').split(/\r?\n/).map(w => w.trim()).filter(Boolean);
  const files = [...fs.readdirSync(MOCKUPS).filter(f => /\.(html|js|css)$/.test(f)).map(f => path.join(MOCKUPS, f)),
    path.join(ROOT, 'README.md')];
  for (const f of files) {
    const text = fs.readFileSync(f, 'utf8');
    for (const w of words) assert.ok(!text.includes(w), `${path.basename(f)}에 개인정보`);
  }
});

test('fonts loaded on every page', () => {
  for (const [html] of PAGES) assert.match(dumpDom(html), /data-fonts="loaded"/, html);
});
