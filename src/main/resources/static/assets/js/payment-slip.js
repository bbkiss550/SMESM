/* Slip images and previews live in browser memory only; never uploaded or persisted. */
((root) => {
  const months = [
    ['มกราคม', 'มค', 'jan', 'january'], ['กุมภาพันธ์', 'กพ', 'feb', 'february'],
    ['มีนาคม', 'มีค', 'mar', 'march'], ['เมษายน', 'เมย', 'apr', 'april'],
    ['พฤษภาคม', 'พค', 'may'], ['มิถุนายน', 'มิย', 'jun', 'june'],
    ['กรกฎาคม', 'กค', 'jul', 'july'], ['สิงหาคม', 'สค', 'aug', 'august'],
    ['กันยายน', 'กย', 'sep', 'sept', 'september'], ['ตุลาคม', 'ตค', 'oct', 'october'],
    ['พฤศจิกายน', 'พย', 'nov', 'november'], ['ธันวาคม', 'ธค', 'dec', 'december']
  ];
  const normalize = text => String(text || '').normalize('NFC')
    .replace(/ํา/g, 'ำ')
    .replace(/[๐-๙]/g, digit => String('๐๑๒๓๔๕๖๗๘๙'.indexOf(digit)))
    .replace(/\r/g, '').replace(/[\u200B-\u200D\uFEFF]/g, '');
  const unique = values => [...new Set(values.filter(Boolean))];
  const onlyOne = values => { const found = unique(values); return found.length === 1 ? found[0] : null; };
  const isoDate = (day, month, year) => {
    year = Number(year);
    if (year < 100) year += year >= 40 ? 2500 : 2000;
    if (year >= 2400) year -= 543;
    if (year < 2000 || year > 2100) return null;
    const date = new Date(Date.UTC(year, Number(month) - 1, Number(day)));
    if (date.getUTCFullYear() !== year || date.getUTCMonth() !== Number(month) - 1 || date.getUTCDate() !== Number(day)) return null;
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
  };
  const parse = rawText => {
    const text = normalize(rawText);
    const dates = [];
    for (const match of text.matchAll(/\b(\d{1,2})\s*[/-]\s*(\d{1,2})\s*[/-]\s*(\d{4}|\d{2})\b/g)) {
      dates.push(isoDate(match[1], match[2], match[3]));
    }
    for (const match of text.matchAll(/(\d{1,2})\s+([ก-๙a-zA-Z.]+)\s+(\d{4}|\d{2})(?!\d)/g)) {
      const month = months.findIndex(names => names.includes(match[2].replace(/\./g, '').toLowerCase()));
      if (month >= 0) dates.push(isoDate(match[1], month + 1, match[3]));
    }
    const times = Array.from(text.matchAll(/\b([01]?\d|2[0-3])\s*:\s*([0-5]\d)(?:\s*:\s*[0-5]\d)?\b/g),
      match => `${match[1].padStart(2, '0')}:${match[2]}`);
    const lines = text.split('\n').map(line => line.trim()).filter(Boolean);
    const amountLabel = /จำนวนเงิน|ยอดเงิน|ยอดโอน|amount|total\s*(?:amount)?/i;
    const feeLabel = /ค่าธรรมเนียม|fee/i;
    const amountsIn = line => Array.from(line.matchAll(/(?<![\d.,])((?:\d{1,3}(?:,\d{3})+|\d+)\.\d{2})(?![\d.])/g),
      match => match[1].replace(/,/g, '')).filter(value => Number(value) > 0);
    const labelled = [];
    lines.forEach((line, index) => {
      if (!amountLabel.test(line) || feeLabel.test(line)) return;
      let candidates = amountsIn(line);
      if (!candidates.length && lines[index + 1] && !feeLabel.test(lines[index + 1])) candidates = amountsIn(lines[index + 1]);
      if (!candidates.length) {
        const integer = line.match(/(?:จำนวนเงิน|ยอดเงิน|ยอดโอน|amount)\s*[:：]?\s*(\d+(?:,\d{3})*)\s*(?:บาท|THB|฿|baht)?\s*$/i);
        if (integer && Number(integer[1].replace(/,/g, '')) > 0) candidates = [Number(integer[1].replace(/,/g, '')).toFixed(2)];
      }
      labelled.push(...candidates);
    });
    const fallback = lines.filter(line => !feeLabel.test(line)).flatMap(amountsIn);
    return { date: onlyOne(dates), time: onlyOne(times), amount: onlyOne(labelled.length ? labelled : fallback) };
  };
  const validateImage = file => {
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) throw new Error('กรุณาเลือกภาพ JPG, PNG หรือ WebP');
    if (file.size > 10 * 1024 * 1024) throw new Error('ภาพสลิปต้องมีขนาดไม่เกิน 10 MB');
  };
  let libraryLoading;
  const loadLibrary = () => {
    if (root.Tesseract) return Promise.resolve();
    if (!libraryLoading) libraryLoading = new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = '/assets/vendor/tesseract/tesseract.min.js';
      script.onload = resolve;
      script.onerror = () => { script.remove(); libraryLoading = null; reject(new Error('โหลดเครื่องมืออ่านสลิปไม่สำเร็จ')); };
      document.head.appendChild(script);
    });
    return libraryLoading;
  };
  const read = async (file, { signal, onProgress = () => {} }) => {
    validateImage(file);
    let worker;
    let abortListener;
    const aborted = new Promise((_, reject) => {
      abortListener = () => reject(new DOMException('ยกเลิกการอ่านสลิป', 'AbortError'));
      signal.addEventListener('abort', abortListener, { once: true });
      if (signal.aborted) abortListener();
    });
    let rejectWorker;
    const workerError = new Promise((_, reject) => { rejectWorker = reject; });
    const wait = promise => Promise.race([promise, aborted, workerError]);
    try {
      await wait(loadLibrary());
      const bitmap = await wait(createImageBitmap(file).then(image => {
        if (signal.aborted) image.close();
        return image;
      }));
      const pixels = bitmap.width * bitmap.height;
      bitmap.close();
      if (pixels > 16_000_000) throw new Error('ภาพมีความละเอียดสูงเกินไป กรุณาลดขนาดภาพแล้วลองอีกครั้ง');
      const initialization = root.Tesseract.createWorker(['tha', 'eng'], 1, {
        workerPath: '/assets/vendor/tesseract/worker.min.js',
        corePath: '/assets/vendor/tesseract/core',
        langPath: '/assets/vendor/tesseract/lang',
        gzip: false, cacheMethod: 'none', workerBlobURL: false,
        logger: message => {
          if (!signal.aborted) onProgress(message.status === 'recognizing text' ? Math.round(message.progress * 100) : null);
        },
        errorHandler: () => rejectWorker(new Error('อ่านภาพไม่สำเร็จ กรุณาลองภาพที่ชัดขึ้นหรือกรอกข้อมูลเอง'))
      });
      initialization.then(created => {
        worker = created;
        if (signal.aborted) worker.terminate();
      }, () => {});
      await wait(initialization);
      await wait(worker.setParameters({ tessedit_pageseg_mode: '11' }));
      const result = await wait(worker.recognize(file));
      return parse(result.data.text);
    } finally {
      signal.removeEventListener('abort', abortListener);
      if (worker) await worker.terminate();
      file = null;
    }
  };
  const api = { parse, validateImage, read };
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  else root.PaymentSlip = api;
})(typeof window !== 'undefined' ? window : globalThis);
