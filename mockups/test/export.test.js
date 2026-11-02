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

test('no personal info in mockups or README', () => {
  const files = [...fs.readdirSync(MOCKUPS).filter(f => /\.(html|js|css)$/.test(f)).map(f => path.join(MOCKUPS, f)),
    path.join(ROOT, 'README.md')];
  for (const f of files) assert.ok(!/PERSONAL_INFO_WORDS/.test(fs.readFileSync(f, 'utf8')), f);
});

test('fonts loaded on every page', () => {
  for (const [html] of PAGES) assert.match(dumpDom(html), /data-fonts="loaded"/, html);
});
