const { test } = require('node:test');
const assert = require('node:assert/strict');
const { parse, validateImage, read } = require('../../main/resources/static/assets/js/payment-slip.js');

test('Thai abbreviated month, Buddhist year, precise time and labelled amount', () => {
  assert.deepEqual(parse('1 ต.ค. 2569 เวลา 10:37:42\nจำนวนเงิน 4,125.50 บาท\nค่าธรรมเนียม 0.00\nเลขที่รายการ 20261001ABC123'),
    { date: '2026-10-01', time: '10:37', amount: '4125.50' });
});
test('Thai digits and full month name', () => {
  assert.deepEqual(parse('๑ ตุลาคม ๒๕๖๙\n๑๐:๓๗\nยอดเงิน\n๑,๐๐๐.๒๕'),
    { date: '2026-10-01', time: '10:37', amount: '1000.25' });
});
test('English date and two-digit Gregorian year', () => {
  assert.equal(parse('1 October 2026\n09:01\nAmount 125.00').date, '2026-10-01');
  assert.equal(parse('01/10/26\n09:01\nAmount 125.00').date, '2026-10-01');
});
test('numeric Buddhist short year and date validation', () => {
  assert.equal(parse('01/10/69').date, '2026-10-01');
  assert.equal(parse('31/02/2569').date, null);
  assert.equal(parse('29/02/2567').date, '2024-02-29');
});
test('next-line amount ignores fee and account numbers', () => {
  assert.equal(parse('Account 1234567890\nจำนวนเงิน\n500.00 THB\nค่าธรรมเนียม 5.00').amount, '500.00');
});
test('labelled integer amount', () => {
  assert.equal(parse('จำนวนเงิน: 500 บาท').amount, '500.00');
});
test('ambiguous amounts and multiple dates are not guessed', () => {
  assert.equal(parse('100.00\n200.00').amount, null);
  assert.equal(parse('Amount 100.00\nAmount 200.00').amount, null);
  assert.equal(parse('01/10/2026\n02/10/2026').date, null);
  assert.equal(parse('10:37\n11:37').time, null);
});
test('deduplicates repeated OCR values', () => {
  assert.equal(parse('01/10/2026 10:37\n01/10/2026 10:37').time, '10:37');
});
test('empty and unreadable image text leave fields unfilled', () => {
  assert.deepEqual(parse(''), { date: null, time: null, amount: null });
});
test('transaction reference is not extracted', () => {
  assert.deepEqual(parse('Reference No:\nABC123456789'), { date: null, time: null, amount: null });
});

test('OCR decomposed Thai sara am is normalized', () => {
  assert.equal(parse('จํานวนเงิน 500 บาท').amount, '500.00');
});

const withWorker = async (recognize, run) => {
  let terminated = 0;
  const previousTesseract = global.Tesseract;
  const previousBitmap = global.createImageBitmap;
  global.createImageBitmap = async () => ({ width: 800, height: 1000, close() {} });
  global.Tesseract = { createWorker: async (_languages, _engine, options) => {
    assert.equal(options.cacheMethod, 'none');
    assert.equal(options.workerBlobURL, false);
    for (const key of ['workerPath', 'corePath', 'langPath']) assert.ok(options[key].startsWith('/assets/vendor/tesseract/'));
    return { setParameters: async () => {}, recognize, terminate: async () => { terminated += 1; } };
  } };
  try { await run(); assert.equal(terminated, 1); }
  finally { global.Tesseract = previousTesseract; global.createImageBitmap = previousBitmap; }
};
const image = { type: 'image/png', size: 1000 };

test('successful OCR releases the worker and returns only extracted fields', async () => {
  await withWorker(async file => {
    assert.equal(file, image);
    return { data: { text: '01/10/2026 10:37\nAmount 500.25' } };
  }, async () => {
    assert.deepEqual(await read(image, { signal: new AbortController().signal }),
      { date: '2026-10-01', time: '10:37', amount: '500.25' });
  });
});
test('failed OCR still terminates worker', async () => {
  await withWorker(async () => { throw new Error('Unreadable image'); }, async () => {
    await assert.rejects(read(image, { signal: new AbortController().signal }));
  });
});
test('cancelled OCR terminates worker and never returns stale results', async () => {
  const controller = new AbortController();
  await withWorker(async () => { controller.abort(); return new Promise(() => {}); }, async () => {
    await assert.rejects(read(image, { signal: controller.signal }), { name: 'AbortError' });
  });
});
test('rejects unsupported and oversized files before OCR starts', async () => {
  await assert.rejects(read({ type: 'application/pdf', size: 1000 }, { signal: new AbortController().signal }));
  await assert.rejects(read({ type: 'image/png', size: 11 * 1024 * 1024 }, { signal: new AbortController().signal }));
});
test('only date, time and amount are returned even with labelled party details', () => {
  const result = parse('01/10/2026 10:37\nAmount 100.00\nธนาคารผู้โอน: กสิกรไทย\nชื่อผู้โอน: นาย ทดสอบ\nบัญชีผู้โอน: xxx-x-x1234-x\nReceiver name: Test Company\nReference No: ABC123456789');
  assert.deepEqual(result, { date: '2026-10-01', time: '10:37', amount: '100.00' });
});
test('accepts 10 MB boundary and rejects oversized/unsupported previews', () => {
  assert.doesNotThrow(() => validateImage({ type: 'image/png', size: 10 * 1024 * 1024 }));
  assert.throws(() => validateImage({ type: 'image/png', size: 10 * 1024 * 1024 + 1 }));
  assert.throws(() => validateImage({ type: 'image/svg+xml', size: 100 }));
});
