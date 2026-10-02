const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

test('remove/close releases preview File and Blob URL and clears sensitive display', () => {
  const source = fs.readFileSync(require('node:path').join(__dirname, '../../main/resources/static/assets/js/app.js'), 'utf8');
  const start = source.indexOf('    const clearSlipPreview = () => {');
  const end = source.indexOf('    const updateBalance', start);
  assert.ok(start > 0 && end > start);
  const revoked = [];
  const image = () => ({ hidden: false, srcRemoved: false, removeAttribute(name) { assert.equal(name, 'src'); this.srcRemoved = true; } });
  const values = [{ textContent: 'Bank Name' }, { textContent: 'Sender Name' }];
  let cancelled = 0;
  const context = {
    previewFile: { type: 'image/png' }, previewUrl: 'blob:test-slip',
    slipPreview: image(), slipZoomImage: image(), slipZoom: { hidden: false },
    slipExpand: { hidden: false }, slipEmpty: { hidden: true }, slipResult: { hidden: false },
    slipDropzone: { classList: { remove() {} } }, slipAmountError: 'Mismatch',
    form: { querySelectorAll: () => values }, cancelSlip: () => { cancelled += 1; },
    syncPaymentControls() {}, URL: { revokeObjectURL: url => revoked.push(url) }
  };
  vm.createContext(context);
  vm.runInContext(source.slice(start, end) + 'clearSlipPreview(); clearSlipPreview();', context);
  assert.equal(context.previewFile, null);
  assert.equal(context.previewUrl, null);
  assert.deepEqual(revoked, ['blob:test-slip']);
  assert.equal(cancelled, 2);
  assert.equal(context.slipPreview.srcRemoved, true);
  assert.equal(context.slipZoomImage.srcRemoved, true);
  assert.equal(context.slipZoom.hidden, true);
  assert.equal(context.slipResult.hidden, true);
  assert.ok(values.every(value => value.textContent === '—'));
});
