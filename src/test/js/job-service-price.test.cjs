const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');

const source = readFileSync(join(__dirname, '../../main/resources/static/assets/js/app.js'), 'utf8');
const start = source.indexOf('const updateJobServicePrice =');
const end = source.indexOf("document.querySelectorAll('form.job-create-form').forEach(updateJobServicePrice);", start);
const update = vm.runInNewContext(`${source.slice(start, end)}; updateJobServicePrice`, { Intl });

function display(price, chargeable = null) {
  const output = { textContent: '' };
  update({
    querySelector: () => output,
    elements: { namedItem: name => name === 'serviceId'
      ? { selectedOptions: [{ dataset: price == null ? {} : { price } }] }
      : chargeable }
  });
  return output.textContent;
}

test('no selected service shows no price', () => assert.equal(display(null), '—'));
test('selected service displays its catalog price with decimals', () => assert.equal(display('1250.50'), '฿1,250.50'));
test('free service displays zero', () => assert.equal(display('0'), '฿0.00'));
test('non-chargeable rework displays zero, chargeable uses catalog price', () => {
  assert.equal(display('1250', { checked: false }), '฿0.00');
  assert.equal(display('1250', { checked: true }), '฿1,250.00');
});
