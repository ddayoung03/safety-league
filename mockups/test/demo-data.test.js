const test = require('node:test');
const assert = require('node:assert/strict');
const D = require('../demo-data.js');

test('season average', () => {
  assert.equal(D.seasonAverage('2026-11-07'), 88.5);
  assert.equal(D.seasonAverage('2026-11-09'), 88.8);
});

test('weekly averages', () => {
  assert.equal(D.average('2026-11-03', '2026-11-09'), 88.8);
  assert.equal(D.average('2026-10-27', '2026-11-02'), 85.7);
});

test('streak counts drive days >= 70, skips no-drive days', () => {
  assert.equal(D.streakAt('2026-11-06'), 12);
  assert.equal(D.streakAt('2026-11-07'), 13);
  assert.equal(D.streakAt('2026-11-09'), 15);
  assert.equal(D.streakAt('2026-11-05'), 11);
});

test('streak breaks under 70 and on any low trip that day', () => {
  assert.equal(D.streakAt('2026-10-24'), 0);
  assert.equal(D.streakAtFor([{ date: '2026-12-01', score: 90 }, { date: '2026-12-01', score: 60 }], '2026-12-01'), 0);
});

test('league lines', () => {
  assert.equal(D.promotionRank(397), 119);
  assert.equal(D.demotionRank(397), 279);
  assert.equal(D.daysLeft('2026-11-09'), 21);
});

test('neighbors are sorted and include me', () => {
  for (const d of ['2026-11-07', '2026-11-09']) {
    const rows = D.neighbors(d);
    const me = rows.find(r => r.name === '나');
    assert.equal(me.rank, D.ranks[d]);
    assert.equal(me.avg, D.seasonAverage(d));
    for (let i = 1; i < rows.length; i++) {
      assert.ok(rows[i - 1].avg >= rows[i].avg && rows[i - 1].rank < rows[i].rank, `${d} row ${i}`);
    }
  }
});
