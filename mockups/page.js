// 모든 시안 페이지 마지막에 호출. 웹 글꼴이 다 불러와진 뒤에 렌더 검사가 읽는 표시를 body에 남긴다.
// data-fonts: 제목 글꼴과 숫자 글꼴이 실제로 불러와졌으면 'loaded'
// data-overflow: 장식(aria-hidden)이 아닌 요소가 1280×720 밖으로 나가거나 글자가 잘리면 'yes',
// data-offender: 처음 걸린 요소 (디버깅용).
function finishPage() {
  document.fonts.ready.then(() => {
    const ok = f => document.fonts.check(`800 40px "${f}"`) && [...document.fonts].some(x => x.family.replace(/"/g, '') === f && x.status === 'loaded');
    document.body.dataset.fonts = ok('Black Han Sans') && ok('Saira Condensed') ? 'loaded' : 'missing';

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
    if (bad) document.body.dataset.offender = bad.tagName + '.' + bad.className + ' ' + bad.textContent.slice(0, 20) + ' ' + JSON.stringify(bad.getBoundingClientRect());
  });
}
