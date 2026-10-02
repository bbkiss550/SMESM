(() => {
  'use strict';
  const root = document.querySelector('[data-dashboard-overview]');
  if (!root) return;
  const content = root.querySelector('[data-overview-content]');
  const periodInput = root.querySelector('[data-overview-period]');
  const dateInput = root.querySelector('[data-overview-date]');
  const caption = root.querySelector('[data-overview-caption]');
  const number = new Intl.NumberFormat('th-TH');
  const money = new Intl.NumberFormat('th-TH', {minimumFractionDigits: 2, maximumFractionDigits: 2});
  const charts = [];
  let current = window.dashboardInitialOverview;
  let controller;

  function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text != null) node.textContent = text;
    return node;
  }
  function icon(name) { return element('i', `bi bi-${name}`); }
  function draw(canvas, configuration) {
    if (typeof Chart !== 'undefined') charts.push(new Chart(canvas, configuration));
  }
  function kpi(card, comparisonLabel) {
    const node = element('section', 'card overview-kpi');
    if (card.money) node.classList.add('overview-kpi-money');
    node.style.setProperty('--metric-color', card.color);
    const head = element('div', 'overview-kpi-head');
    const mark = element('span', 'overview-icon'); mark.append(icon(card.icon));
    const body = element('div', 'overview-kpi-text');
    body.append(element('h2', 'overview-kpi-label', card.label));
    const values = element('div', 'overview-kpi-values');
    values.append(element('strong', 'overview-kpi-value', card.money ? `฿${money.format(card.value)}` : number.format(card.value)));
    let delta = '—', tone = 'neutral';
    if (card.change != null) {
      const change = Number(card.change);
      delta = `${change > 0 ? '↑' : change < 0 ? '↓' : ''} ${Math.abs(change).toLocaleString('th-TH', {maximumFractionDigits: 1})}%`;
      if (change !== 0) tone = (card.inverse ? change < 0 : change > 0) ? 'good' : 'bad';
    }
    values.append(element('span', `overview-delta overview-delta-${tone}`, delta));
    body.append(values, element('small', 'text-muted', card.money ? `${comparisonLabel} (฿${money.format(card.previous)})` : `${comparisonLabel} (${number.format(card.previous)} งาน)`));
    if (card.money) body.append(element('small', 'text-muted', 'รับชำระจริง ก่อนหักคืนเงิน'));
    head.append(mark, body); node.append(head);
    const sparkWrap = element('div', 'overview-sparkline');
    const canvas = element('canvas'); canvas.setAttribute('aria-label', `แนวโน้ม${card.label}`); sparkWrap.append(canvas); node.append(sparkWrap);
    return {node, canvas, card};
  }
  const centerTotal = {
    id: 'overviewCenterTotal',
    afterDraw(chart) {
      const total = chart.options.plugins.overviewCenterTotal?.total;
      if (total == null) return;
      const {ctx, chartArea} = chart;
      if (!chartArea) return;
      const x = (chartArea.left + chartArea.right) / 2, y = (chartArea.top + chartArea.bottom) / 2;
      ctx.save(); ctx.textAlign = 'center'; ctx.textBaseline = 'middle'; ctx.fillStyle = '#102851';
      ctx.font = 'bold 26px sans-serif'; ctx.fillText(number.format(total), x, y - 7);
      ctx.font = '12px sans-serif'; ctx.fillText('รายการ', x, y + 19); ctx.restore();
    }
  };
  function donut(title, groups, total) {
    const node = element('section', 'card overview-chart');
    const heading = element('h2', 'section-title'); heading.append(icon('pie-chart-fill'), document.createTextNode(` ${title}`)); node.append(heading);
    const body = element('div', 'overview-donut-body');
    const wrap = element('div', 'overview-donut-canvas'); const canvas = element('canvas'); canvas.setAttribute('aria-label', title); wrap.append(canvas);
    const legend = element('ul', 'overview-legend');
    for (const group of groups) {
      const item = element('li'); const dot = element('span', 'overview-dot'); dot.style.backgroundColor = group.color;
      item.append(dot, element('span', 'overview-group-name', group.label), element('strong', '', number.format(group.value)), element('span', 'text-muted', `(${Number(group.percentage).toFixed(1)}%)`)); legend.append(item);
    }
    if (!groups.length || !total) legend.append(element('li', 'text-muted', 'ไม่มีข้อมูลในช่วงเวลาที่เลือก'));
    body.append(wrap, legend); node.append(body);
    return {node, canvas, groups, total};
  }
  function render(data) {
    charts.splice(0).forEach(chart => chart.destroy());
    const fragment = document.createDocumentFragment();
    const grid = element('div', 'overview-kpi-grid');
    const cards = data.cards.map(card => kpi(card, data.comparisonLabel)); cards.forEach(card => grid.append(card.node)); fragment.append(grid);
    const chartGrid = element('div', 'overview-chart-grid');
    const status = donut(`สถานะงาน (${data.label})`, data.statuses, data.total); chartGrid.append(status.node);
    const trendNode = element('section', 'card overview-chart');
    const trendTitle = element('h2', 'section-title'); trendTitle.append(icon('graph-up-arrow'), document.createTextNode(' แนวโน้มจำนวนงาน')); trendNode.append(trendTitle);
    trendNode.append(element('small', 'text-muted mb-3', 'งานเปิดใหม่รวมงานค้างยกมา แบ่งตามสถานะ ณ สิ้นวัน/เดือน'));
    const trendWrap = element('div', 'overview-trend-canvas'); const trendCanvas = element('canvas'); trendCanvas.setAttribute('aria-label', 'แนวโน้มจำนวนงาน'); trendWrap.append(trendCanvas); trendNode.append(trendWrap); chartGrid.append(trendNode);
    const services = donut(`ประเภทบริการ (${data.label})`, data.services, data.total); services.node.classList.add('overview-service-chart'); chartGrid.append(services.node);
    fragment.append(chartGrid);
    fragment.append(element('p', 'overview-definition text-muted', 'งานทั้งหมด = งานเปิดใหม่ในช่วงที่เลือก + งานค้างยกมาตอนเริ่มช่วง · งานเกินกำหนด = เลยเวลานัดสิ้นสุดและยังไม่ปิด · ช่วงปัจจุบันนับถึงเวลานี้'));
    if (data.historicalLimitations) fragment.append(element('p', 'overview-definition text-muted', 'ข้อมูลย้อนหลังใช้ประวัติสถานะเท่าที่มี งานเก่าที่ไม่มีประวัติครบอาจสะท้อนสถานะปัจจุบัน หรือแสดงเป็นไม่มีประวัติย้อนหลัง วันที่ยกเลิกที่ไม่มีประวัติใช้วันที่ปรับปรุงล่าสุด และเกณฑ์เกินกำหนดใช้เวลานัดล่าสุด'));
    content.replaceChildren(fragment);
    caption.textContent = data.label;
    root.querySelector('.dashboard-today span').textContent = data.todayLabel;
    cards.forEach(({canvas, card}) => draw(canvas, {
      type: 'line', data: {labels: data.trendLabels, datasets: [{data: card.sparkline, borderColor: card.color, backgroundColor: card.color + '18', fill: true, tension: .25, borderWidth: 2, pointRadius: 0}]},
      options: {responsive: true, maintainAspectRatio: false, animation: false, plugins: {legend: {display: false}, tooltip: {enabled: false}}, scales: {x: {display: false}, y: {display: false, beginAtZero: true}}}
    }));
    [status, services].forEach(({canvas, groups, total}) => draw(canvas, {
      type: 'doughnut', plugins: [centerTotal], data: {labels: groups.map(g => g.label), datasets: [{data: total ? groups.map(g => g.value) : [1], backgroundColor: total ? groups.map(g => g.color) : ['#e7edf5'], borderWidth: 0}]},
      options: {responsive: true, maintainAspectRatio: false, cutout: '70%', plugins: {legend: {display: false}, overviewCenterTotal: {total}, tooltip: {enabled: total > 0}}}
    }));
    draw(trendCanvas, {type: 'bar', data: {labels: data.trendLabels, datasets: data.trend.map(s => ({label: s.label, data: s.values, backgroundColor: s.color, borderRadius: 3, maxBarThickness: 34}))},
      options: {responsive: true, maintainAspectRatio: false, plugins: {legend: {position: 'bottom', labels: {usePointStyle: true, boxWidth: 8, padding: 12}}}, scales: {x: {stacked: true, grid: {display: false}, ticks: {maxTicksLimit: 12}}, y: {stacked: true, beginAtZero: true, ticks: {precision: 0}, grid: {color: '#e7edf5'}}}}});
  }
  function syncControls() {
    periodInput.value = current.period;
    picker.setDate(current.date, false);
    const date = new Date(`${current.date}T12:00:00`);
    picker.altInput.value = date.toLocaleDateString('th-TH', current.period === 'YEARLY' ? {year: 'numeric'} : current.period === 'MONTHLY' ? {month: 'long', year: 'numeric'} : {day: 'numeric', month: 'long', year: 'numeric'});
  }
  function updateCalendarYear(instance) {
    const year = instance.currentYearElement;
    if (!year?.parentElement) return;
    year.parentElement.classList.add('thai-buddhist-year-host');
    year.tabIndex = -1;
    year.setAttribute('aria-label', `ปีพุทธศักราช ${instance.currentYear + 543}`);
    let label = year.parentElement.querySelector('.thai-buddhist-year');
    if (!label) { label = element('span', 'thai-buddhist-year'); label.setAttribute('aria-hidden', 'true'); year.parentElement.append(label); }
    label.textContent = String(instance.currentYear + 543);
  }
  const picker = flatpickr(dateInput, {locale: flatpickr.l10ns.th, dateFormat: 'Y-m-d', altInput: true, altFormat: 'j F Y', disableMobile: true,
    onReady: (_, __, instance) => updateCalendarYear(instance), onYearChange: (_, __, instance) => updateCalendarYear(instance), onMonthChange: (_, __, instance) => updateCalendarYear(instance),
    onChange: (_, value) => load(periodInput.value, value)});
  async function load(period, date) {
    controller?.abort(); const request = new AbortController(); controller = request;
    content.setAttribute('aria-busy', 'true'); root.classList.add('overview-loading');
    try {
      const url = new URL('/dashboard/overview', window.location.origin); url.searchParams.set('period', period); url.searchParams.set('date', date);
      const response = await fetch(url, {signal: request.signal, headers: {Accept: 'application/json'}});
      if (!response.ok) throw new Error('Dashboard request failed');
      const data = await response.json(); if (request.signal.aborted) return;
      render(data); current = data; syncControls();
    } catch (error) {
      if (error.name !== 'AbortError') {
        syncControls(); Swal.fire({toast: true, position: 'top-end', icon: 'error', title: 'โหลดข้อมูลไม่สำเร็จ ข้อมูลเดิมยังคงอยู่', showConfirmButton: false, timer: 4000});
      }
    } finally {
      if (controller === request) { root.classList.remove('overview-loading'); content.removeAttribute('aria-busy'); }
    }
  }
  periodInput.addEventListener('change', () => load(periodInput.value, current.date));
  root.querySelectorAll('[data-overview-step]').forEach(button => button.addEventListener('click', () => {
    const date = new Date(`${current.date}T12:00:00`), amount = Number(button.dataset.overviewStep);
    if (current.period === 'DAILY') date.setDate(date.getDate() + amount);
    else if (current.period === 'MONTHLY') { date.setDate(1); date.setMonth(date.getMonth() + amount); }
    else { date.setMonth(0, 1); date.setFullYear(date.getFullYear() + amount); }
    const iso = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    load(current.period, iso);
  }));
  render(current); syncControls();
})();
