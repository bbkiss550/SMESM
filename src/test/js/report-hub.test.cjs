const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '../../..');
const js = fs.readFileSync(path.join(root, 'src/main/resources/static/assets/js/report-hub.js'), 'utf8');
const html = fs.readFileSync(path.join(root, 'src/main/resources/templates/report/index.html'), 'utf8');

test('hub has no detail features or URL navigation', () => {
  assert.ok(html.includes('id="report-content"'));
  assert.ok(html.includes('type="button" class="report-card"'));
  assert.ok(!/<(?:table|canvas)|window\.print|pagination|type="search"/.test(html));
  assert.ok(!/location|history\.|fetch\(|URLSearchParams/.test(js));
});

test('every report code selects locally and emits the future loader event', () => {
  const codes = ['SERVICE', 'OVERDUE', 'APPOINTMENT', 'PAYMENT', 'EXPENSE', 'TECHNICIAN', 'CUSTOMER', 'REWORK', 'STOCK'];
  const cards = codes.map(code => ({ dataset: { reportCode: code, reportTitle: code },
    attrs: {}, setAttribute(key, value) { this.attrs[key] = value; } }));
  const title = { textContent: '' };
  const content = { dataset: {}, hidden: true, querySelector: () => title };
  const events = [];
  let click;
  const hub = { querySelector: () => content, querySelectorAll: () => cards,
    addEventListener(type, handler) { assert.equal(type, 'click'); click = handler; },
    dispatchEvent(event) { events.push(event); } };
  vm.runInNewContext(js, { document: { querySelector: () => hub },
    CustomEvent: class { constructor(type, options) { this.type = type; this.detail = options.detail; } } });
  for (const card of cards) {
    click({ target: { closest: () => card } });
    assert.equal(content.dataset.reportCode, card.dataset.reportCode);
    assert.equal(content.hidden, false);
    assert.equal(title.textContent, card.dataset.reportTitle);
    assert.equal(cards.filter(item => item.attrs['aria-pressed'] === 'true').length, 1);
    assert.equal(events.at(-1).type, 'report:selected');
    assert.equal(events.at(-1).detail.code, card.dataset.reportCode);
  }
  assert.equal(events.length, 9);
});

test('script does nothing outside the report hub', () => {
  assert.doesNotThrow(() => vm.runInNewContext(js, { document: { querySelector: () => null } }));
});
