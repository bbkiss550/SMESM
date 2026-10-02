const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');
const source = readFileSync(join(__dirname, '../../main/resources/static/assets/js/app.js'), 'utf8');
const helper = source.slice(source.indexOf('const getDefaultJobSchedule ='), source.indexOf('const parseLocalDate ='));
const calculate = timestamp => JSON.parse(JSON.stringify(vm.runInNewContext(`${helper}\ngetDefaultJobSchedule(new Date(timestamp))`, { timestamp })));

test('uses Bangkok today, rounds to the next whole hour and ends one hour later', () => {
  assert.deepEqual(calculate('2026-10-02T03:25:00Z'), {
    startDate: '2026-10-02', startTime: '11:00', endDate: '2026-10-02', endTime: '12:00'
  });
});
test('even an exact whole hour defaults to the following hour', () => {
  assert.equal(calculate('2026-10-02T03:00:00Z').startTime, '11:00');
});
test('moves the end date forward when the end time crosses midnight', () => {
  assert.deepEqual(calculate('2026-10-02T15:30:00Z'), {
    startDate: '2026-10-02', startTime: '23:00', endDate: '2026-10-03', endTime: '00:00'
  });
});
test('late-night defaults roll both dates over, including the year boundary', () => {
  assert.deepEqual(calculate('2026-12-31T16:30:00Z'), {
    startDate: '2027-01-01', startTime: '00:00', endDate: '2027-01-01', endTime: '01:00'
  });
});
