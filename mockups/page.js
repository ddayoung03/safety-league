// 모든 시안 페이지 마지막에 호출. 웹 글꼴이 다 불러와진 뒤에 렌더 검사가 읽는 표시를 body에 남긴다.
// data-fonts: 제목 글꼴과 숫자 글꼴이 실제로 불러와졌으면 'loaded'
// data-overflow: 장식(aria-hidden)이 아닌 요소가 1280×720 밖으로 나가거나 글자가 잘리면 'yes',
// data-offender: 처음 걸린 요소 (디버깅용).
function finishPage() {
  document.fonts.ready.then(() => {
    const ok = f => document.fonts.check(`800 40px "${f}"`) && [...document.fonts].some(x => x.family.replace(/"/g, '') === f && x.status === 'loaded');
    document.body.dataset.fonts = ok('Pretendard') && ok('Saira Condensed') ? 'loaded' : 'missing';

    // 글꼴마다 글자 폭이 달라서, 실제 글꼴로 그려진 뒤에 재야 한다.
    const bad = [...document.body.querySelectorAll('*')].find(el => {
      if (el.closest('[aria-hidden="true"]') || el.tagName === 'SCRIPT') return false;
      const cs = getComputedStyle(el);
      // 인라인 상자는 글꼴 여백까지 잡혀 실제 글자보다 크다. 블록 상자만 본다.
      if (cs.display === 'inline') return false;
      const r = el.getBoundingClientRect();
      if (r.width === 0 && r.height === 0) return false;
      const outside = r.left < -1 || r.top < -1 || r.right > 1281 || r.bottom > 721;
      // 말줄임은 별명 칸(.n)에서만 허용. 순위·점수가 잘리면 안 된다.
      const clipped = cs.overflow === 'hidden' && !el.classList.contains('n') && el.scrollWidth > el.clientWidth + 1;
      return outside || clipped;
    });
    document.body.dataset.overflow = bad ? 'yes' : 'none';
    document.body.dataset.audit = carAudit().join(' | ');
    if (bad) document.body.dataset.offender = bad.tagName + '.' + bad.className + ' ' + bad.textContent.slice(0, 20) + ' ' + JSON.stringify(bad.getBoundingClientRect());
  });
}

// DEMO.route(100×60 좌표)를 svg에 그린다: 넓은 회색 노면 + 노란 점선 중앙선 + 출발점 + 도착 체커기.
// opts.events가 있으면 사건 지점에 종류별 표시를 찍는다.
function drawRoute(svg, opts = {}) {
  const pts = DEMO.route.map(([x, y]) => [x * 10, y * 10]);
  const d = 'M' + pts.map(p => p.join(' ')).join(' L');
  const [sx, sy] = pts[0], [fx, fy] = pts[pts.length - 1];
  let s = `<path d="${d}" stroke="#000" stroke-opacity=".35" stroke-width="${(opts.width || 34) + 10}" stroke-linejoin="round" stroke-linecap="round" fill="none" transform="translate(0 8)"/>`
    + `<path d="${d}" stroke="#F2F1EC" stroke-opacity="${opts.road || .07}" stroke-width="${opts.width || 34}" stroke-linejoin="round" stroke-linecap="round" fill="none"/>`
    + `<path d="${d}" stroke="#FFC52E" stroke-opacity="${opts.line || .25}" stroke-width="3" stroke-dasharray="2 14" fill="none"/>`
    + `<circle cx="${sx}" cy="${sy}" r="12" fill="#F2F1EC" fill-opacity="${opts.marks || .35}"/>`
    + `<g opacity="${opts.marks || .35}"><rect x="${fx - 16}" y="${fy - 16}" width="32" height="32" fill="#F2F1EC"/><path d="M${fx - 16} ${fy - 16}h16v16h-16zM${fx} ${fy}h16v16h-16z" fill="#24272C"/></g>`;
  for (const e of opts.events || []) {
    const [x, y] = DEMO.pointAt(pts, e.t);
    s += `<g transform="translate(${x} ${y})">${EVENT_ICON[e.kind]}</g>`;
  }
  svg.setAttribute('viewBox', '0 0 1000 600');
  svg.innerHTML = s;
}

// 사건 종류: 이름과 아이콘(원 + 선 기호). 깜빡이 누락만 노랑(고칠 것), 나머지는 흰색.
const EVENT_NAME = { lane: '깜빡이 없이 차선 변경', brake: '급제동', accel: '급가속' };
const EVENT_ICON = {
  lane: '<circle r="22" fill="#FFC52E"/><path d="M-7 11V1l12-10M-1-9h6v6" fill="none" stroke="#24272C" stroke-width="3.5" stroke-linejoin="round" stroke-linecap="round"/>',
  brake: '<circle r="22" fill="#F2F1EC"/><path d="M0-11v12M0 8v2" stroke="#24272C" stroke-width="4.5" stroke-linecap="round"/>',
  accel: '<circle r="22" fill="#F2F1EC"/><path d="M-9-7l8 7-8 7M1-7l8 7-8 7" fill="none" stroke="#24272C" stroke-width="3.5" stroke-linejoin="round" stroke-linecap="round"/>',
};

// Android Automotive 가독성 기준 검사. 어긋난 곳을 "글자: 이유" 목록으로 돌려준다.
// 글씨 24px 이상, 글씨와 배경 대비 4.5:1 이상, 링크·버튼은 76×76px 이상.
const PAGE_BG = [48, 52, 59]; // 조명이 가장 밝은 곳(#30343B). 밝은 글씨에는 가장 불리한 바탕.
function carAudit() {
  const rgba = s => (s.match(/[\d.]+/g) || [0, 0, 0, 0]).map(Number).concat(1).slice(0, 4);
  const lum = c => { const v = c.slice(0, 3).map(x => { x /= 255; return x <= .03928 ? x / 12.92 : ((x + .055) / 1.055) ** 2.4; }); return .2126 * v[0] + .7152 * v[1] + .0722 * v[2]; };
  const mix = (fg, bg, a) => fg.slice(0, 3).map((x, i) => x * a + bg[i] * (1 - a));
  const out = [];
  for (const el of document.body.querySelectorAll('*')) {
    if (el.closest('[aria-hidden="true"]') || el.tagName === 'SCRIPT') continue;
    const r = el.getBoundingClientRect();
    if (r.width === 0) continue;
    const label = (el.textContent || el.getAttribute('aria-label') || el.tagName).trim().slice(0, 14);
    if ((el.tagName === 'A' || el.tagName === 'BUTTON') && Math.min(r.width, r.height) < 75.5) out.push(`${label}: 누르는 영역 ${Math.round(r.width)}×${Math.round(r.height)}`);
    const own = [...el.childNodes].some(n => n.nodeType === 3 && n.textContent.trim());
    if (!own) continue;
    const cs = getComputedStyle(el);
    const size = parseFloat(cs.fontSize);
    if (size < 23.5) out.push(`${label}: 글씨 ${size}px`);
    // 바탕: 색이 칠해진 가장 가까운 조상, 없으면 페이지 바탕. 투명도는 글씨에 곱한다.
    let bg = PAGE_BG, alpha = rgba(cs.color)[3];
    for (let p = el; p && p !== document.body; p = p.parentElement) {
      const ps = getComputedStyle(p);
      alpha *= parseFloat(ps.opacity);
      const b = rgba(ps.backgroundColor);
      if (b[3] > .5 && bg === PAGE_BG) bg = b.slice(0, 3);
    }
    const fg = mix(rgba(cs.color), bg, alpha);
    const [hi, lo] = [lum(fg), lum(bg)].sort((a, b) => b - a);
    const ratio = (hi + .05) / (lo + .05);
    if (ratio < 4.5) out.push(`${label}: 대비 ${ratio.toFixed(1)}`);
  }
  return out;
}
