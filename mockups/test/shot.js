// 눈으로 확인용 스크린샷: node mockups/test/shot.js <page.html> <out.png>
const { execFileSync } = require('node:child_process');
const path = require('node:path');
const { CHROME, pageUrl } = require('./chrome.js');
const [page, out] = process.argv.slice(2);
execFileSync(CHROME, ['--headless=new', '--disable-gpu', '--hide-scrollbars', '--window-size=1280,720',
  '--virtual-time-budget=10000', '--screenshot=' + path.resolve(out), pageUrl(page)], { stdio: 'ignore' });
