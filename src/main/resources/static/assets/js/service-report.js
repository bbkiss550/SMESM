(() => {
  'use strict';
  const hub = document.querySelector('.report-hub');
  if (!hub) return;
  const content = hub.querySelector('#report-content');
  const groups = hub.querySelector('.report-groups');
  const heading = hub.querySelector('.report-hub-heading');
  const placeholder = content.innerHTML;
  const preview = document.querySelector('#serviceReportPreviewModal');
  const pdfPages = preview.querySelector('[data-pdf-pages]');
  const pdfLoading = preview.querySelector('[data-pdf-loading]');
  const pdfError = preview.querySelector('[data-pdf-error]');
  const pdfDownload = preview.querySelector('[data-pdf-download]');
  const pdfPrint = preview.querySelector('[data-pdf-print]');
  let pdfRequest, pdfUrl, pdfDocument;
  let request, applied = new URLSearchParams(), selectedCard;
  let selectedCode = 'SERVICE';
  const detailUrl = () => selectedCode === 'SERVICE' ? '/reports/service' : `/reports/detail/${selectedCode}`;
  const destroyDates = () => content.querySelectorAll('input[name="start"], input[name="end"]')
    .forEach(input => input._flatpickr?.destroy());
  function initializeDates() {
    if (!window.flatpickr) return;
    content.querySelectorAll('input[type="date"]').forEach(input => {
      const picker = flatpickr(input, {
        locale: { ...flatpickr.l10ns.th, yearAriaLabel: 'ปี', monthAriaLabel: 'เดือน' }, dateFormat: 'Y-m-d', altInput: true, altFormat: 'd/m/Y',
        disableMobile: true,
        onReady: [thaiDisplay], onChange: [thaiDisplay], onOpen: [thaiDisplay],
        onYearChange: [thaiDisplay], onMonthChange: [thaiDisplay]
      });
      picker.altInput.id = `${input.id}-display`;
      content.querySelector(`label[for="${input.id}"]`).htmlFor = picker.altInput.id;
    });
  }
  function thaiDisplay(dates, value, picker) {
    if (dates[0]) picker.altInput.value = `${String(dates[0].getDate()).padStart(2,'0')}/${String(dates[0].getMonth()+1).padStart(2,'0')}/${dates[0].getFullYear()+543}`;
    const year = picker.currentYearElement;
    if (year) { year.type = 'text'; year.readOnly = true; year.value = String(picker.currentYear + 543); }
  }
  function back() {
    request?.abort(); destroyDates();
    content.innerHTML = placeholder; content.hidden = true;
    content.classList.remove('service-report-container');
    content.removeAttribute('aria-busy');
    groups.hidden = false; heading.hidden = false;
    hub.querySelectorAll('[data-report-code]').forEach(card => card.setAttribute('aria-pressed','false'));
    selectedCard?.focus();
  }
  async function load(params = applied) {
    request?.abort(); const current = new AbortController(); request = current;
    content.setAttribute('aria-busy','true');
    let loading = content.querySelector('[data-report-loading]');
    if (!loading) { loading = document.createElement('div'); loading.dataset.reportLoading = '';
      loading.className = 'alert alert-info'; content.prepend(loading); }
    loading.textContent = 'กำลังโหลดรายงาน…';
    content.querySelector('[data-report-load-error]')?.remove();
    try {
      const response = await fetch(`${detailUrl()}?${params}`, { signal: current.signal,
        headers: { 'X-Requested-With': 'XMLHttpRequest' } });
      if (!response.ok || response.redirected) throw new Error('request failed');
      const html = await response.text();
      if (current.signal.aborted) return;
      destroyDates(); content.innerHTML = html; initializeDates();
      if (!content.querySelector('[role="alert"]')) {
        applied = new URLSearchParams(new FormData(content.querySelector('[data-service-report-form]')));
        if (params.has('page')) applied.set('page', params.get('page'));
      }
    } catch (error) {
      if (error.name === 'AbortError') return;
      loading.remove();
      const alert = document.createElement('div'); alert.dataset.reportLoadError = '';
      alert.className = 'alert alert-danger'; alert.setAttribute('role','alert');
      alert.textContent = 'โหลดรายงานไม่สำเร็จ กรุณาลองอีกครั้ง หรือเข้าสู่ระบบใหม่หากเซสชันหมดอายุ ';
      const retry = document.createElement('button'); retry.type = 'button';
      retry.className = 'btn btn-sm btn-outline-danger'; retry.textContent = 'ลองอีกครั้ง';
      retry.addEventListener('click', () => load(params)); alert.append(retry); content.prepend(alert);
    } finally { if (request === current) content.removeAttribute('aria-busy'); }
  }
  hub.addEventListener('report:selected', event => {
    request?.abort();
    selectedCode = event.detail.code;
    applied = new URLSearchParams();
    selectedCard = hub.querySelector(`[data-report-code="${selectedCode}"]`);
    groups.hidden = true; heading.hidden = true;
    content.classList.add('service-report-container');
    content.innerHTML = '<button type="button" class="btn btn-outline-secondary mb-3" data-report-back>กลับหน้ารวมรายงาน</button>';
    load();
  });
  content.addEventListener('submit', event => {
    const form = event.target.closest('[data-service-report-form]');
    if (!form) return;
    event.preventDefault();
    if (!form.reportValidity()) return;
    const params = new URLSearchParams(new FormData(form));
    if (params.get('end') < params.get('start')) {
      window.Swal?.fire({ icon: 'warning', text: 'วันที่สิ้นสุดต้องไม่น้อยกว่าวันที่เริ่มต้น' }); return;
    }
    load(params);
  });
  content.addEventListener('click', event => {
    if (event.target.closest('[data-report-back]')) return back();
    if (event.target.closest('[data-report-reset]')) return load(new URLSearchParams());
    if (event.target.closest('[data-service-pdf]')) return showPdf();
    const page = event.target.closest('[data-service-page]');
    if (page && !page.disabled) { const params = new URLSearchParams(applied);
      params.set('page',page.dataset.servicePage); load(params); }
  });
  async function showPdf() {
    pdfRequest?.abort();
    const current = new AbortController(); pdfRequest = current;
    if (pdfUrl) { URL.revokeObjectURL(pdfUrl); pdfUrl = null; }
    pdfPages.hidden = true; pdfPages.replaceChildren();
    pdfDownload.hidden = true; pdfPrint.disabled = true; pdfError.hidden = true; pdfLoading.hidden = false;
    bootstrap.Modal.getOrCreateInstance(preview).show();
    const params = new URLSearchParams(applied); params.delete('page');
    try {
      // Fetch JSON so browser download managers do not intercept the preview request as a PDF download.
      const response = await fetch(`${detailUrl()}/preview?${params}`, { signal: current.signal,
        headers: { 'Accept': 'application/json' } });
      if (!response.ok || response.redirected || !response.headers.get('content-type')?.includes('application/json')) throw new Error('Preview request failed');
      const { pdfBase64 } = await response.json();
      if (!pdfBase64) throw new Error('Missing preview document');
      const bytes = Uint8Array.from(atob(pdfBase64), char => char.charCodeAt(0));
      const blob = new Blob([bytes], { type: 'application/pdf' });
      if (current.signal.aborted) return;
      const pdfLib = await import('/assets/vendor/pdfjs/pdf.mjs');
      pdfLib.GlobalWorkerOptions.workerSrc = '/assets/vendor/pdfjs/pdf.worker.mjs';
      pdfDocument = await pdfLib.getDocument({ data: bytes }).promise;
      if (current.signal.aborted) return;
      pdfUrl = URL.createObjectURL(blob);
      const title = selectedCard?.dataset.reportTitle || 'งานบริการ';
      preview.querySelector('#serviceReportPreviewTitle').textContent = `หน้ากระดาษรายงาน${title}`;
      pdfPages.setAttribute('aria-label', `ตัวอย่างหน้ากระดาษรายงาน${title}`);
      pdfDownload.download = `${selectedCode.toLowerCase()}-report.pdf`;
      pdfDownload.href = pdfUrl; pdfDownload.hidden = false;
      pdfPages.hidden = false;
      for (let number = 1; number <= pdfDocument.numPages; number++) {
        if (current.signal.aborted) return;
        const page = await pdfDocument.getPage(number);
        const viewport = page.getViewport({ scale: Math.min(1.5, Math.max(.6, (pdfPages.clientWidth - 32) / page.view[2])) });
        const wrapper = document.createElement('div'); wrapper.className = 'service-report-pdf-page';
        const label = document.createElement('span'); label.textContent = `หน้า ${number} / ${pdfDocument.numPages}`;
        const canvas = document.createElement('canvas');
        const ratio = Math.max(window.devicePixelRatio || 1, 2);
        canvas.width = Math.round(viewport.width * ratio); canvas.height = Math.round(viewport.height * ratio);
        canvas.style.width = `${viewport.width}px`;
        wrapper.append(label, canvas); pdfPages.append(wrapper);
        await page.render({ canvasContext: canvas.getContext('2d'), viewport,
          transform: ratio === 1 ? null : [ratio, 0, 0, ratio, 0, 0] }).promise;
        if (number === 1) pdfLoading.hidden = true;
      }
      pdfPrint.disabled = false;
    } catch (error) {
      if (error.name !== 'AbortError') { pdfLoading.hidden = true; pdfError.hidden = false; }
    }
  }
  pdfPrint.addEventListener('click', () => {
    if (!pdfPrint.disabled && !pdfPages.hidden && pdfPages.childElementCount === pdfDocument?.numPages) window.print();
  });
  preview.addEventListener('hidden.bs.modal', () => {
    pdfRequest?.abort(); pdfPrint.disabled = true; pdfPages.replaceChildren(); pdfDocument?.destroy(); pdfDocument = null;
    if (pdfUrl) { URL.revokeObjectURL(pdfUrl); pdfUrl = null; }
  });
})();
