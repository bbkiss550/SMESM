const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../../..');
const js = fs.readFileSync(path.join(root, 'src/main/resources/static/assets/js/dashboard-overview.js'), 'utf8');
const html = fs.readFileSync(path.join(root, 'src/main/resources/templates/dashboard/index.html'), 'utf8');

test('dashboard has one global period filter and no detail table', () => {
  assert.equal((html.match(/data-overview-period/g) || []).length, 1);
  for (const period of ['DAILY', 'MONTHLY', 'YEARLY']) assert.ok(html.includes(`value="${period}"`));
  assert.ok(!html.includes('<table')); assert.ok(!html.includes('recentJobs'));
});
test('period requests do not navigate or mutate browser history', () => {
  assert.ok(js.includes("new URL('/dashboard/overview'"));
  assert.ok(!/location\.(?:href\s*=|assign|replace|reload)|history\.(?:pushState|replaceState)/.test(js));
  assert.ok(js.includes('AbortController')); assert.ok(js.includes('chart.destroy()'));
});
test('loading errors retain existing content and restore controls', () => {
  assert.ok(js.includes("content.setAttribute('aria-busy', 'true')"));
  assert.ok(js.includes('ข้อมูลเดิมยังคงอยู่'));
  assert.ok(js.includes('syncControls(); Swal.fire'));
  assert.ok(!js.includes('innerHTML'));
});
