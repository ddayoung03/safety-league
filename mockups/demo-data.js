// 시연용 가상 운전자 "나"의 데이터와 점수 규칙. 모든 시안의 숫자는 여기서 나온다.
// 브라우저에서는 전역 DEMO, Node에서는 require('./demo-data.js').
const DEMO = {
  league: {
    total: 397, tier: '골드', model: '아이오닉 5', region: '부산',
    season: { start: '2026-11-01', end: '2026-11-30' },
  },

  trips: [
    { date: '2026-10-24', score: 66 },
    { date: '2026-10-25', score: 80 },
    { date: '2026-10-26', score: 81 },
    { date: '2026-10-27', score: 82 },
    { date: '2026-10-28', score: 85 },
    { date: '2026-10-29', score: 84 },
    { date: '2026-10-30', score: 86 },
    { date: '2026-10-31', score: 86 },
    { date: '2026-11-01', score: 88 },
    { date: '2026-11-02', score: 89 },
    { date: '2026-11-03', score: 84 },
    { date: '2026-11-04', score: 88 },
    { date: '2026-11-06', score: 90 },
    { date: '2026-11-07', score: 92, km: 18.4 },
    { date: '2026-11-08', score: 86 },
    { date: '2026-11-09', score: 93 },
  ],

  // 그날이 끝난 뒤의 시즌 순위
  ranks: {
    '2026-11-03': 131, '2026-11-04': 130, '2026-11-05': 130, '2026-11-06': 128,
    '2026-11-07': 125, '2026-11-08': 126, '2026-11-09': 125,
  },

  // 순위표 이웃 (가상 별명). 승급 라인 P119는 시즌 평균 89.7로 고정.
  board: {
    '2026-11-07': [
      [123, '부드러운출근길', 89.1], [124, '깜빡이장인', 88.8], [125, '나'],
      [126, '해운대드라이버', 88.4], [127, '조용한퇴근', 88.3], [128, '주말여행러', 88.2],
    ],
    '2026-11-09': [
      [1, '광안대교출근', 96.4], [2, '부드러운출근길', 95.8],
      [118, '금정구모범생', 89.8], [119, '동래젠틀맨', 89.7],
      [124, '깜빡이장인', 89.1], [125, '나'], [126, '해운대드라이버', 88.7],
    ],
  },

  // 가상의 출퇴근 경로 (실제 장소 아님). 100×60 상자 안 좌표, 출발 → 도착 순서.
  route: [[6, 52], [14, 50], [22, 44], [26, 34], [34, 28], [46, 30], [54, 24], [58, 14],
    [68, 10], [80, 12], [88, 20], [92, 30], [86, 38], [90, 48]],

  // 아쉬운 날(별도 시나리오) 리플레이. t = 경로 위 위치(0~1). 깜빡이 누락 4회는 피트 보드와 같다.
  lowDay: {
    start: '6:08', end: '6:42', km: 22.1,
    events: [
      { at: '6:12', kind: 'accel', t: 0.10 },
      { at: '6:17', kind: 'lane', t: 0.21 },
      { at: '6:21', kind: 'brake', t: 0.34 },
      { at: '6:26', kind: 'lane', t: 0.47 },
      { at: '6:31', kind: 'lane', t: 0.61 },
      { at: '6:34', kind: 'brake', t: 0.70 },
      { at: '6:38', kind: 'lane', t: 0.86 },
    ],
  },

  // 경로를 따라 전체 길이의 t 비율만큼 간 지점
  pointAt(pts, t) {
    const seg = pts.slice(1).map((p, i) => Math.hypot(p[0] - pts[i][0], p[1] - pts[i][1]));
    let left = t * seg.reduce((a, b) => a + b, 0);
    for (let i = 0; i < seg.length; i++) {
      if (left <= seg[i] || i === seg.length - 1) {
        const k = seg[i] ? Math.min(left / seg[i], 1) : 0;
        return [pts[i][0] + (pts[i + 1][0] - pts[i][0]) * k, pts[i][1] + (pts[i + 1][1] - pts[i][1]) * k];
      }
      left -= seg[i];
    }
    return pts[0];
  },

  round1(x) { return Math.round(x * 10) / 10; },

  average(from, to) {
    const s = DEMO.trips.filter(t => t.date >= from && t.date <= to).map(t => t.score);
    return DEMO.round1(s.reduce((a, b) => a + b, 0) / s.length);
  },

  seasonAverage(until) { return DEMO.average(DEMO.league.season.start, until); },

  // 70점 이상인 운전일이 이어진 일수. 운전 안 한 날은 건너뛰고, 그날 트립 하나라도 미만이면 끊긴다.
  streakAtFor(trips, date, threshold = 70) {
    const byDay = {};
    for (const t of trips) if (t.date <= date) (byDay[t.date] ||= []).push(t.score);
    let n = 0;
    for (const d of Object.keys(byDay).sort().reverse()) {
      if (byDay[d].some(s => s < threshold)) break;
      n++;
    }
    return n;
  },

  streakAt(date, threshold = 70) { return DEMO.streakAtFor(DEMO.trips, date, threshold); },

  daysLeft(date) {
    const day = 86400000;
    return Math.round((Date.parse(DEMO.league.season.end) - Date.parse(date)) / day);
  },

  promotionRank(total) { return Math.round(total * 0.3); },
  demotionRank(total) { return total - Math.round(total * 0.3) + 1; },

  neighbors(date) {
    return DEMO.board[date].map(([rank, name, avg]) =>
      ({ rank, name, avg: name === '나' ? DEMO.seasonAverage(date) : avg }));
  },
};

if (typeof module !== 'undefined') module.exports = DEMO;
