const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');

test('table job numbers are plain text rather than navigation or modal controls', () => {
  for (const file of ['job/list.html', 'customer/detail.html', 'payment/list.html']) {
    const template = readFileSync(join(__dirname, '../../main/resources/templates', file), 'utf8');
    const jobNumbers = [...template.matchAll(/<([a-z]+)\b([^>]*th:text="\$\{(?:p\.job|job)\.jobNo\}"[^>]*)>/g)];
    assert.ok(jobNumbers.length > 0, file);
    for (const [, tag, attributes] of jobNumbers) {
      assert.equal(tag, 'span', file);
      assert.doesNotMatch(attributes, /href|data-bs-toggle|data-job-details-url/, file);
    }
  }
});

test('payments keep the explicit view-job button opening the existing modal', () => {
  const template = readFileSync(join(__dirname, '../../main/resources/templates/payment/list.html'), 'utf8');
  assert.match(template, /<button\b[^>]*data-bs-target="#jobDetailsModal"[^>]*>[\s\S]*?ดูใบงาน\s*<\/button>/);
});

test('payment entry is bound to the clicked record, with a hidden job ID and no picker', () => {
  const template = readFileSync(join(__dirname, '../../main/resources/templates/payment/list.html'), 'utf8');
  assert.match(template, /<input type="hidden" th:field="\*\{jobId\}"/);
  assert.doesNotMatch(template, /<select[^>]*th:field="\*\{jobId\}"/);
  const triggers = [...template.matchAll(/data-bs-target="#recordPaymentModal"\s+th:attr="([^"]+)"/g)];
  assert.equal(triggers.length, 1);
  assert.equal((template.match(/data-bs-target="#recordPaymentModal"/g) || []).length, 1);
  assert.match(triggers[0][0], /data-payment-job-id/);
  assert.match(triggers[0][0], /data-payment-job-no/);
});

test('jobs view-data action opens the existing detail modal, not a detail-page link', () => {
  const template = readFileSync(join(__dirname, '../../main/resources/templates/job/list.html'), 'utf8');
  assert.match(template, /<button\b[^>]*data-bs-target="#jobDetailsModal"[^>]*>[\s\S]*?ดูข้อมูล<\/button>/);
  assert.match(template, /id="jobDetailsModalBody"/);
  assert.doesNotMatch(template, /<a\b[^>]*>[\s\S]*?ดูข้อมูล<\/a>/);
});
