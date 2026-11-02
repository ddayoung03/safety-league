const { execFileSync } = require('node:child_process');
const path = require('node:path');
const { pathToFileURL } = require('node:url');

const CHROME = process.env.CHROME || 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const MOCKUPS = path.join(__dirname, '..');

const pageUrl = (file) => pathToFileURL(path.join(MOCKUPS, file)).href;

// 페이지를 헤드리스로 열어 스크립트 실행 후의 DOM을 문자열로 돌려준다.
function dumpDom(file) {
  return execFileSync(CHROME, [
    '--headless=new', '--disable-gpu', '--window-size=1280,720',
    '--virtual-time-budget=10000', '--dump-dom', pageUrl(file),
  ], { encoding: 'utf8', timeout: 60000 });
}

module.exports = { CHROME, MOCKUPS, pageUrl, dumpDom };
