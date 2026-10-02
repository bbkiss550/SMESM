(() => {
  'use strict';
  const hub = document.querySelector('.report-hub');
  if (!hub) return;
  const content = hub.querySelector('#report-content');
  const cards = [...hub.querySelectorAll('[data-report-code]')];
  const codes = new Set(['SERVICE', 'OVERDUE', 'APPOINTMENT', 'PAYMENT', 'EXPENSE',
    'TECHNICIAN', 'CUSTOMER', 'REWORK', 'STOCK']);
  function selectReport(card) {
    const code = card.dataset.reportCode;
    if (!codes.has(code)) return;
    cards.forEach(item => item.setAttribute('aria-pressed', String(item === card)));
    content.dataset.reportCode = code;
    content.querySelector('[data-report-content-title]').textContent = card.dataset.reportTitle;
    content.hidden = false;
    // Future detail loaders can listen here; the hub makes no detail requests or URL changes.
    hub.dispatchEvent(new CustomEvent('report:selected', { bubbles: true, detail: { code } }));
  }
  hub.addEventListener('click', event => {
    const card = event.target.closest('[data-report-code]');
    if (card && cards.includes(card)) selectReport(card);
  });
})();
