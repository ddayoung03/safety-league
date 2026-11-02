// 시안 5장을 1280×720 PNG로 내보낸다: node mockups/export.js
// 글꼴이 안 불러와진 페이지가 있으면 아무것도 찍지 않고 실패한다.
const { execFileSync } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
const { CHROME, MOCKUPS, pageUrl, dumpDom } = require('./test/chrome.js');

const PAGES = [
  ['result-win.html', '1-result-win.png'],
  ['result-low.html', '2-result-low.png'],
  ['league.html', '3-league.png'],
  ['driving.html', '4-driving.png'],
  ['report.html', '5-report.png'],
];

function main() {
  for (const [html] of PAGES) {
    if (!/data-fonts="loaded"/.test(dumpDom(html))) throw new Error(`${html}: 글꼴을 불러오지 못함`);
  }
  fs.mkdirSync(path.join(MOCKUPS, 'png'), { recursive: true });
  for (const [html, png] of PAGES) {
    execFileSync(CHROME, ['--headless=new', '--disable-gpu', '--hide-scrollbars', '--window-size=1280,720',
      '--virtual-time-budget=10000', '--screenshot=' + path.join(MOCKUPS, 'png', png), pageUrl(html)], { stdio: 'ignore' });
    console.log('png/' + png);
  }
}

if (require.main === module) main();
module.exports = { PAGES };
