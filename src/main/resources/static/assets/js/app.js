(() => {
  const html = document.documentElement;
  const savedTheme = localStorage.getItem('sm-theme') || 'light';
  html.setAttribute('data-bs-theme', savedTheme);

  document.querySelectorAll('[data-theme-toggle]').forEach(button => button.addEventListener('click', () => {
    const next = html.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
    html.setAttribute('data-bs-theme', next);
    localStorage.setItem('sm-theme', next);
  }));

  const sidebar = document.querySelector('.app-sidebar');
  const overlay = document.querySelector('.sidebar-overlay');
  const closeSidebar = () => { sidebar?.classList.remove('open'); overlay?.classList.remove('show'); };
  document.querySelectorAll('[data-sidebar-toggle]').forEach(button => button.addEventListener('click', () => {
    sidebar?.classList.toggle('open'); overlay?.classList.toggle('show');
  }));
  overlay?.addEventListener('click', closeSidebar);

  const success = document.getElementById('flash-success')?.textContent?.trim();
  const error = document.getElementById('flash-error')?.textContent?.trim();
  const temporaryPassword = document.getElementById('temporary-password')?.textContent?.trim();
  if (success) Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: success, showConfirmButton: false, timer: 2800 });
  if (error) Swal.fire({ icon: 'error', title: 'ไม่สำเร็จ', text: error, confirmButtonColor: '#0878f9' });
  if (temporaryPassword) Swal.fire({ icon: 'info', title: 'รหัสผ่านชั่วคราว', html: `<code class="fs-4">${temporaryPassword}</code><p class="mt-3 mb-0">โปรดคัดลอกและส่งให้ผู้ใช้อย่างปลอดภัย</p>`, confirmButtonColor: '#0878f9' });

  const updatePhoneCounter = input => {
    const limit = Number(input.maxLength) || 10;
    input.value = input.value.slice(0, limit);
    let counter = input.parentElement.querySelector(`[data-phone-counter-for="${input.id}"]`);
    if (!counter) {
      counter = document.createElement('small');
      counter.className = 'form-text text-muted text-end d-block';
      counter.dataset.phoneCounterFor = input.id;
      input.insertAdjacentElement('afterend', counter);
    }
    counter.textContent = `${input.value.length}/${limit}`;
  };
  document.querySelectorAll('input[data-phone-counter]').forEach(input => {
    updatePhoneCounter(input);
    input.addEventListener('input', () => updatePhoneCounter(input));
  });

  document.addEventListener('keydown', event => {
    if (event.key !== 'Escape' || event.defaultPrevented || event.isComposing || Swal.isVisible()) return;
    if (document.querySelector('.flatpickr-calendar.open')) return;
    const slipZoom = document.querySelector('.modal.show [data-slip-zoom]:not([hidden])');
    if (slipZoom) {
      event.preventDefault();
      event.stopImmediatePropagation();
      slipZoom.querySelector('[data-slip-zoom-close]').click();
      return;
    }

    const openModals = [...document.querySelectorAll('.modal.show')];
    if (!openModals.length) return;
    const activeModal = document.activeElement?.closest('.modal.show') || openModals.at(-1);
    event.preventDefault();
    event.stopImmediatePropagation();
    bootstrap.Modal.getOrCreateInstance(activeModal).hide();
  }, true);

  document.addEventListener('show.bs.modal', event => {
    const modal = event.target;
    if (!modal.matches('[data-reset-on-open]')) return;

    const form = modal.querySelector('form');
    if (!form) return;
    const trigger = event.relatedTarget;
    if (trigger?.dataset.editKind) {
      const kind = trigger.dataset.editKind;
      const collection = { service: 'services', product: 'products', user: 'users', customer: 'customers' }[kind];
      form.action = `/${collection}/${trigger.dataset.id}`;
      const fields = {
        service: { serviceName: 'serviceName', description: 'description', defaultPrice: 'defaultPrice', defaultDurationMinutes: 'defaultDurationMinutes' },
        product: { productName: 'productName', category: 'category', unit: 'unit', costPrice: 'costPrice', salePrice: 'salePrice', minStock: 'minStock' },
        user: { firstName: 'firstName', lastName: 'lastName', phone: 'phone', email: 'email', roleId: 'roleId' },
        customer: { customerType: 'customerType', name: 'name', companyName: 'companyName', taxId: 'taxId', primaryPhone: 'primaryPhone', secondaryPhone: 'secondaryPhone', email: 'email', lineId: 'lineId', address: 'address', note: 'note' }
      }[kind];
      Object.entries(fields).forEach(([fieldName, dataName]) => {
        const field = form.elements.namedItem(fieldName);
        if (field) field.value = trigger.dataset[dataName] || '';
      });
      if (kind === 'user') {
        const username = modal.querySelector('[data-username-field]');
        const password = modal.querySelector('[data-password-field]');
        if (username) username.hidden = true;
        if (password) password.hidden = true;
        form.elements.namedItem('username').value = '';
        form.elements.namedItem('username').required = false;
        form.elements.namedItem('password').value = '';
        form.elements.namedItem('password').required = false;
      }
      if (kind === 'product') {
        const openingStock = modal.querySelector('[data-opening-stock-field]');
        if (openingStock) openingStock.hidden = true;
      }
      modal.querySelector('[data-form-title]').textContent = ({ service: 'แก้ไขบริการ', product: 'แก้ไขสินค้า / อะไหล่', user: 'แก้ไขข้อมูลผู้ใช้งาน', customer: 'แก้ไขข้อมูลลูกค้า' })[kind];
      modal.querySelector('[data-form-submit]').textContent = 'บันทึกการแก้ไข';
      return;
    }

    form.action = modal.dataset.createAction || form.action;
    modal.querySelector('[data-form-title]')?.replaceChildren(modal.dataset.createTitle || 'เพิ่มรายการ');
    const submitLabel = modal.dataset.createSubmit;
    if (submitLabel) modal.querySelector('[data-form-submit]')?.replaceChildren(submitLabel);
    const openingStock = modal.querySelector('[data-opening-stock-field]');
    if (openingStock) openingStock.hidden = false;
    const username = modal.querySelector('[data-username-field]');
    const password = modal.querySelector('[data-password-field]');
    if (username) username.hidden = false;
    if (password) password.hidden = false;
    const passwordInput = form.elements.namedItem('password');
    if (passwordInput) passwordInput.required = true;
    const usernameInput = form.elements.namedItem('username');
    if (usernameInput) usernameInput.required = true;
    form.querySelectorAll('input:not([type="hidden"]):not([type="submit"]):not([type="button"]), textarea').forEach(field => {
      if (field.type === 'checkbox' || field.type === 'radio') field.checked = false;
      else field.value = '';
      field.dispatchEvent(new Event('input', { bubbles: true }));
      field.dispatchEvent(new Event('change', { bubbles: true }));
    });
    form.querySelectorAll('select').forEach(select => {
      const emptyOption = [...select.options].find(option => option.value === '');
      select.value = emptyOption ? '' : (select.options[0]?.value ?? '');
      select.dispatchEvent(new Event('change', { bubbles: true }));
    });
    form.applyJobScheduleDefaults?.();
  });
  document.addEventListener('hidden.bs.modal', event => {
    const modal = event.target;
    if (!modal.matches('[data-reset-on-open]')) return;
    const openingStock = modal.querySelector('[data-opening-stock-field]');
    if (openingStock) openingStock.hidden = false;
    const username = modal.querySelector('[data-username-field]');
    const password = modal.querySelector('[data-password-field]');
    if (username) username.hidden = false;
    if (password) password.hidden = false;
  });
  document.getElementById('stockTransactionModal')?.addEventListener('show.bs.modal', event => {
    const source = event.relatedTarget;
    if (!source) return;
    ['date', 'type', 'productCode', 'productName', 'reference', 'quantity', 'before', 'after', 'user', 'note'].forEach(field => {
      const target = event.target.querySelector(`[data-stock-field="${field}"]`);
      if (target) target.textContent = source.dataset[field] || '-';
    });
  });
  let customerDetailsRequest = 0;
  let customerDetailsMarkup = null;
  const customerViewModal = document.getElementById('customerViewModal');
  const customerViewTitle = customerViewModal?.querySelector('[data-customer-view-title]');
  const customerViewCode = customerViewModal?.querySelector('[data-customer-view="code"]');
  const customerViewBack = customerViewModal?.querySelector('[data-customer-view-back]');
  customerViewModal?.addEventListener('show.bs.modal', async event => {
    const source = event.relatedTarget;
    if (!source?.matches('[data-customer-details-url]')) return;
    customerDetailsMarkup = null;
    customerViewTitle.textContent = 'รายละเอียดลูกค้า';
    customerViewCode.textContent = source.dataset.customerCode || '';
    customerViewBack.hidden = true;
    const requestId = ++customerDetailsRequest;
    const body = document.getElementById('customerViewModalBody');
    body.innerHTML = '<div class="d-flex justify-content-center align-items-center gap-2 py-5 text-muted"><span class="spinner-border spinner-border-sm" aria-hidden="true"></span>กำลังโหลดข้อมูลลูกค้า...</div>';
    try {
      const response = await fetch(source.dataset.customerDetailsUrl, { headers: { Accept: 'text/html', 'X-Requested-With': 'XMLHttpRequest' } });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const detailsDocument = new DOMParser().parseFromString(await response.text(), 'text/html');
      if (requestId !== customerDetailsRequest) return;
      body.replaceChildren(...Array.from(detailsDocument.body.childNodes, node => document.importNode(node, true)));
      initializeSortableTables(body);
    } catch (error) {
      if (requestId !== customerDetailsRequest) return;
      body.innerHTML = '<div class="alert alert-danger mb-0">โหลดรายละเอียดและประวัติบริการไม่สำเร็จ กรุณาลองอีกครั้ง</div>';
    }
  });
  customerViewModal?.addEventListener('hidden.bs.modal', () => { customerDetailsRequest += 1; });
  customerViewModal?.addEventListener('click', async event => {
    if (event.target.closest('[data-customer-view-back]')) {
      if (customerDetailsMarkup !== null) customerViewModal.querySelector('#customerViewModalBody').innerHTML = customerDetailsMarkup;
      customerDetailsMarkup = null;
      customerViewTitle.textContent = 'รายละเอียดลูกค้า';
      customerViewBack.hidden = true;
      customerViewCode.hidden = false;
      initializeSortableTables(customerViewModal);
      return;
    }

    const trigger = event.target.closest('[data-customer-job-details]');
    if (!trigger) return;
    event.preventDefault();
    const requestId = ++customerDetailsRequest;
    const body = customerViewModal.querySelector('#customerViewModalBody');
    if (customerDetailsMarkup === null) customerDetailsMarkup = body.innerHTML;
    customerViewTitle.textContent = 'รายละเอียดใบงาน';
    customerViewCode.hidden = true;
    customerViewBack.hidden = false;
    body.innerHTML = '<div class="d-flex justify-content-center align-items-center gap-2 py-5 text-muted"><span class="spinner-border spinner-border-sm" aria-hidden="true"></span>กำลังโหลดรายละเอียดใบงาน...</div>';
    try {
      const response = await fetch(trigger.dataset.jobDetailsUrl, { headers: { Accept: 'text/html', 'X-Requested-With': 'XMLHttpRequest' } });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const detailsDocument = new DOMParser().parseFromString(await response.text(), 'text/html');
      if (requestId !== customerDetailsRequest) return;
      body.replaceChildren(...Array.from(detailsDocument.body.childNodes, node => document.importNode(node, true)));
      initializeSortableTables(body);
    } catch (error) {
      if (requestId !== customerDetailsRequest) return;
      body.innerHTML = '<div class="alert alert-danger mb-0">โหลดรายละเอียดใบงานไม่สำเร็จ กรุณาลองอีกครั้ง</div>';
    }
  });
  if (window.location.hash === '#cancelModal') {
    const cancellationModal = document.getElementById('cancelModal');
    if (cancellationModal) bootstrap.Modal.getOrCreateInstance(cancellationModal).show();
  }

  const initializeConfirmForms = root => root.querySelectorAll('form.js-confirm').forEach(form => form.addEventListener('submit', event => {
    event.preventDefault();
    Swal.fire({ title: form.dataset.confirmTitle || 'ยืนยันรายการ?', text: form.dataset.confirmText || 'กรุณาตรวจสอบข้อมูลก่อนดำเนินการ', icon: 'warning', showCancelButton: true, confirmButtonText: 'ยืนยัน', cancelButtonText: 'ยกเลิก', confirmButtonColor: '#0878f9' })
      .then(result => { if (result.isConfirmed) form.submit(); });
  }));
  initializeConfirmForms(document);

  const search = document.getElementById('global-search');
  const results = document.getElementById('global-search-results');
  let timer;
  search?.addEventListener('input', () => {
    clearTimeout(timer);
    if (search.value.trim().length < 2) { results?.classList.remove('show'); return; }
    timer = setTimeout(async () => {
      const response = await fetch(`/search?q=${encodeURIComponent(search.value.trim())}`);
      if (!response.ok) return;
      const items = await response.json();
      results.innerHTML = items.length ? items.map(item => `<a href="${item.url}"><small class="text-primary fw-bold">${item.type}</small><div>${item.code} · ${item.label}</div></a>`).join('') : '<div class="p-3 text-muted">ไม่พบข้อมูล</div>';
      results.classList.add('show');
    }, 250);
  });
  document.addEventListener('click', event => { if (!event.target.closest('.global-search')) results?.classList.remove('show'); });

  const jobDetailsModal = document.getElementById('jobDetailsModal');
  let jobDetailsRequest = 0;
  jobDetailsModal?.addEventListener('show.bs.modal', async event => {
    const source = event.relatedTarget;
    if (!source?.dataset.jobDetailsUrl) return;

    const requestId = ++jobDetailsRequest;
    const body = document.getElementById('jobDetailsModalBody');
    body.replaceChildren(Object.assign(document.createElement('div'), {
      className: 'd-flex justify-content-center align-items-center gap-2 py-5 text-muted',
      innerHTML: '<span class="spinner-border spinner-border-sm" aria-hidden="true"></span>กำลังโหลดรายละเอียดใบงาน...'
    }));

    try {
      const response = await fetch(source.dataset.jobDetailsUrl, { headers: { Accept: 'text/html' } });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const detailsDocument = new DOMParser().parseFromString(await response.text(), 'text/html');
      if (requestId !== jobDetailsRequest) return;
      body.replaceChildren(...Array.from(detailsDocument.body.childNodes, node => document.importNode(node, true)));
    } catch (error) {
      if (requestId !== jobDetailsRequest) return;
      body.innerHTML = '<div class="alert alert-danger mb-0">โหลดรายละเอียดใบงานไม่สำเร็จ กรุณาลองอีกครั้ง</div>';
    }
  });
  jobDetailsModal?.addEventListener('hidden.bs.modal', () => { jobDetailsRequest += 1; });

  const refreshCurrentJobMenu = async () => {
    const isCalendar = window.location.pathname === '/calendar';
    const requestUrl = new URL(window.location.href);
    if (!isCalendar) {
      const filter = document.querySelector('form[data-ajax-search="jobs"]');
      if (filter) requestUrl.search = new URLSearchParams(new FormData(filter)).toString();
      requestUrl.searchParams.delete('page');
    }
    const response = await fetch(requestUrl, { headers: { Accept: 'text/html', 'X-Requested-With': 'XMLHttpRequest' } });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const pageDocument = new DOMParser().parseFromString(await response.text(), 'text/html');
    if (isCalendar) {
      const currentCalendar = document.querySelector('.calendar-grid');
      const nextCalendar = pageDocument.querySelector('.calendar-grid');
      if (!currentCalendar || !nextCalendar) throw new Error('ไม่พบปฏิทิน');
      currentCalendar.replaceWith(nextCalendar);
      return;
    }
    const currentTable = document.querySelector('[data-ajax-table="jobs"]');
    const nextTable = pageDocument.querySelector('[data-ajax-table="jobs"]');
    const currentKpis = document.querySelector('.kpi-grid');
    const nextKpis = pageDocument.querySelector('.kpi-grid');
    if (!currentTable || !nextTable) throw new Error('ไม่พบรายการใบงาน');
    currentTable.replaceWith(nextTable);
    if (currentKpis && nextKpis) currentKpis.replaceWith(nextKpis);
    initializeSortableTables(nextTable);
    initializeConfirmForms(nextTable);
  };

  document.addEventListener('submit', async event => {
    const form = event.target.closest('form[data-modal-job-assign]');
    if (!form) return;
    event.preventDefault();
    if (form.matches('[data-job-reschedule]')) {
      const fields = form.elements;
      const start = `${fields.namedItem('startDate').value}T${fields.namedItem('startTime').value}`;
      const end = `${fields.namedItem('endDate').value}T${fields.namedItem('endTime').value}`;
      if (end <= start) {
        form.querySelector('[data-assign-error]').textContent = 'เวลาสิ้นสุดต้องอยู่หลังเวลาเริ่ม';
        return;
      }
    }
    const button = form.querySelector('[type="submit"]');
    if (button.disabled) return;
    button.disabled = true;
    const errorBox = form.querySelector('[data-assign-error]');
    errorBox.textContent = '';
    let saved = false;
    try {
      const response = await fetch(form.action, { method: 'POST', body: new FormData(form), headers: { Accept: 'application/json' } });
      const result = await response.json();
      if (!response.ok) throw new Error(result.message || 'มอบหมายงานไม่สำเร็จ');
      saved = true;
      const details = await fetch(form.dataset.detailsUrl, { headers: { Accept: 'text/html' } });
      if (!details.ok) throw new Error('โหลดรายละเอียดล่าสุดไม่สำเร็จ');
      const html = await details.text();
      if (!form.isConnected) return;
      const body = form.closest('.calendar-job-detail-grid').parentElement;
      body.innerHTML = html;
      initializeSortableTables(body);
      if (['/jobs', '/calendar'].includes(location.pathname)) await refreshCurrentJobMenu();
      Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: result.message, timer: 2500, showConfirmButton: false });
    } catch (error) {
      errorBox.textContent = saved ? 'บันทึกการมอบหมายแล้ว แต่โหลดข้อมูลล่าสุดไม่สำเร็จ กรุณาปิดแล้วเปิดรายละเอียดใหม่' : error.message;
      if (saved && !errorBox.isConnected) Swal.fire({ icon: 'info', title: 'บันทึกการมอบหมายแล้ว', text: 'โหลดรายการล่าสุดไม่สำเร็จ กรุณาค้นหาเพื่อโหลดอีกครั้ง' });
    } finally { button.disabled = false; }
  });

  const updateJobServicePrice = form => {
    const output = form.querySelector('[data-job-service-price]');
    const service = form.elements.namedItem('serviceId');
    if (!output || !service) return;
    const price = service.selectedOptions[0]?.dataset.price;
    const chargeable = form.elements.namedItem('chargeable');
    output.textContent = price == null ? '—' : new Intl.NumberFormat('th-TH', {
      style: 'currency', currency: 'THB'
    }).format(chargeable && !chargeable.checked ? 0 : Number(price));
  };
  document.querySelectorAll('form.job-create-form').forEach(updateJobServicePrice);
  document.addEventListener('change', event => {
    const pricingField = event.target.closest('form.job-create-form [name="serviceId"], form.job-create-form [name="chargeable"]');
    if (pricingField) updateJobServicePrice(pricingField.closest('form'));
    const customer = event.target.closest('form.job-create-form select[name="customerId"]');
    if (!customer) return;
    const form = customer.closest('form');
    const selected = customer.selectedOptions[0];
    const address = form.elements.namedItem('address');
    const phone = form.elements.namedItem('contactPhone');
    if (selected?.dataset.address && !address.value) address.value = selected.dataset.address;
    if (selected?.dataset.phone && !phone.value) phone.value = selected.dataset.phone.slice(0, 10);
    phone.dispatchEvent(new Event('input', { bubbles: true }));
  });

  document.addEventListener('submit', async event => {
    const form = event.target.closest('form[data-modal-job-create="true"]');
    if (!form) return;
    event.preventDefault();
    let saved = false;
    const submitButton = form.querySelector('[type="submit"], button:not([type])');
    if (submitButton) submitButton.disabled = true;
    try {
      const response = await fetch(form.action, {
        method: form.method || 'post',
        body: new FormData(form),
        headers: { Accept: 'text/html', 'X-Requested-With': 'XMLHttpRequest' }
      });
      const responseText = await response.text();
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      if (!response.redirected) {
        const responseDocument = new DOMParser().parseFromString(responseText, 'text/html');
        const responseForm = responseDocument.querySelector('form.job-create-form');
        if (!responseForm) throw new Error('ไม่พบแบบฟอร์มสำหรับแสดงข้อผิดพลาด');
        const validationMessages = [...new Set([...responseForm.querySelectorAll('.text-danger')].map(message => message.textContent.trim()).filter(Boolean))];
        Swal.fire({ icon: 'error', title: 'กรุณาตรวจสอบข้อมูลใบงาน', text: validationMessages.join(' · ') || 'โปรดตรวจสอบวันที่ เวลา และข้อมูลที่จำเป็น', confirmButtonColor: '#0878f9' });
        return;
      }
      saved = true;
      bootstrap.Modal.getInstance(form.closest('.modal'))?.hide();
      await refreshCurrentJobMenu();
      Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'สร้างใบงานเรียบร้อยแล้ว', showConfirmButton: false, timer: 2800 });
    } catch (error) {
      Swal.fire({ toast: true, position: 'top-end', icon: saved ? 'success' : 'error', title: saved ? 'บันทึกใบงานแล้ว' : 'บันทึกใบงานไม่สำเร็จ', text: saved ? 'โหลดรายการใหม่ไม่สำเร็จ กรุณารีเฟรชหน้านี้' : 'กรุณาลองใหม่อีกครั้ง', showConfirmButton: false, timer: 3000 });
    } finally {
      if (submitButton?.isConnected) submitButton.disabled = false;
    }
  });

  const thaiMonthNames = [
    'มกราคม', 'กุมภาพันธ์', 'มีนาคม', 'เมษายน', 'พฤษภาคม', 'มิถุนายน',
    'กรกฎาคม', 'สิงหาคม', 'กันยายน', 'ตุลาคม', 'พฤศจิกายน', 'ธันวาคม'
  ];
  const padDatePart = value => String(value).padStart(2, '0');
  const formatThaiDate = date => {
    if (!(date instanceof Date) || Number.isNaN(date.getTime())) return '';
    return `${date.getDate()} ${thaiMonthNames[date.getMonth()]} ${date.getFullYear() + 543}`;
  };
  const formatIsoDate = date => `${date.getFullYear()}-${padDatePart(date.getMonth() + 1)}-${padDatePart(date.getDate())}`;
  const getDefaultJobSchedule = (now = new Date()) => {
    const parts = Object.fromEntries(new Intl.DateTimeFormat('en-CA', {
      timeZone: 'Asia/Bangkok', year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', hourCycle: 'h23'
    }).formatToParts(now).map(part => [part.type, part.value]));
    const start = new Date(Date.UTC(Number(parts.year), Number(parts.month) - 1, Number(parts.day), Number(parts.hour) + 1));
    const end = new Date(start.getTime() + 60 * 60 * 1000);
    return { startDate: start.toISOString().slice(0, 10), startTime: start.toISOString().slice(11, 16),
      endDate: end.toISOString().slice(0, 10), endTime: end.toISOString().slice(11, 16) };
  };
  const parseLocalDate = value => {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value || '');
    if (!match) return null;
    const [, year, month, day] = match;
    return new Date(Number(year), Number(month) - 1, Number(day));
  };
  const updateThaiCalendarYear = instance => {
    const yearInput = instance.currentYearElement;
    const yearHost = yearInput?.parentElement;
    if (!yearInput || !yearHost) return;
    yearHost.classList.add('thai-buddhist-year-host');
    yearInput.tabIndex = -1;
    yearInput.setAttribute('aria-label', `ปีพุทธศักราช ${instance.currentYear + 543}`);
    let label = yearHost.querySelector('.thai-buddhist-year');
    if (!label) {
      label = document.createElement('span');
      label.className = 'thai-buddhist-year';
      label.setAttribute('aria-hidden', 'true');
      yearHost.appendChild(label);
    }
    label.textContent = String(instance.currentYear + 543);
  };
  const updateThaiDateDisplay = (selectedDates, _dateText, instance) => {
    const selectedDate = selectedDates[0];
    const isoDate = selectedDate ? formatIsoDate(selectedDate) : '';
    instance.input.dataset.selectedDate = isoDate;
    if (instance.altInput) {
      instance.altInput.value = selectedDate ? formatThaiDate(selectedDate) : '';
    }
    updateThaiCalendarYear(instance);
  };

  if (window.flatpickr) {
    document.addEventListener('shown.bs.collapse', event => {
      const form = event.target.querySelector('form[data-job-reschedule]');
      if (!form || form.dataset.initialized) return;
      form.dataset.initialized = 'true';
      form.querySelectorAll('[data-reschedule-date]').forEach(input => {
        flatpickr(input, {
          locale: thaiLocale, dateFormat: 'Y-m-d', altInput: true, altFormat: 'j F Y',
          disableMobile: true, allowInput: false,
          onReady: [updateThaiDateDisplay], onOpen: [updateThaiDateDisplay],
          onChange: [updateThaiDateDisplay],
          onMonthChange: [(_dates, _text, picker) => updateThaiCalendarYear(picker)],
          onYearChange: [(_dates, _text, picker) => updateThaiCalendarYear(picker)]
        });
      });
      form.querySelectorAll('[data-reschedule-time]').forEach(select => {
        const times = new Set(Array.from({ length: 96 }, (_, i) => `${String(Math.floor(i / 4)).padStart(2, '0')}:${String((i % 4) * 15).padStart(2, '0')}`));
        times.add(select.dataset.initialTime);
        [...times].sort().forEach(time => select.add(new Option(time, time)));
        select.value = select.dataset.initialTime;
      });
      form.querySelectorAll('select').forEach(select => {
        select.setAttribute('data-searchable-select', '');
        select.dataset.searchPlaceholder = select.matches('[data-reschedule-time]') ? 'ค้นหาเวลา เช่น 08:30' : 'ค้นหาหัวหน้าช่าง...';
      });
      initializeSearchableSelects(form);
    });
    const thaiLocale = {
      ...flatpickr.l10ns.th,
      yearAriaLabel: 'ปี',
      monthAriaLabel: 'เดือน',
      hourAriaLabel: 'ชั่วโมง',
      minuteAriaLabel: 'นาที'
    };

    document.querySelectorAll('[data-time-choice]').forEach(select => {
      const initialTime = (select.dataset.initialTime || '').slice(0, 5);
      for (let minutes = 0; minutes < 24 * 60; minutes += 15) {
        const hour = String(Math.floor(minutes / 60)).padStart(2, '0');
        const minute = String(minutes % 60).padStart(2, '0');
        const value = `${hour}:${minute}`;
        select.add(new Option(value, value, false, value === initialTime));
      }
    });

    const dateTimeStates = Array.from(document.querySelectorAll('[data-thai-datetime-group]')).map(group => {
      const dateValueInput = group.querySelector('[data-date-value]');
      const dateInput = group.querySelector('[data-thai-date]');
      const timeInput = group.querySelector('[data-thai-time]');
      const timeChoice = group.querySelector('[data-time-choice]');
      if (!dateValueInput || !dateInput || !timeInput || !timeChoice) return null;

      const initialDate = parseLocalDate(dateValueInput.value);
      dateInput.autocomplete = 'off';
      let pendingDateSync;

      const syncDateValue = (selectedDates, dateText, instance) => {
        updateThaiDateDisplay(selectedDates, dateText, instance);
        const nextValue = selectedDates[0] ? formatIsoDate(selectedDates[0]) : '';
        clearTimeout(pendingDateSync);
        pendingDateSync = setTimeout(() => {
          if (dateValueInput.value === nextValue) return;
          dateValueInput.value = nextValue;
          group.dispatchEvent(new CustomEvent('datetimechange'));
        }, 0);
      };

      const picker = flatpickr(dateInput, {
        locale: thaiLocale,
        dateFormat: 'Y-m-d',
        altInput: true,
        altInputClass: 'form-control thai-date-display',
        altFormat: 'j F Y',
        ariaDateFormat: 'j F Y',
        allowInput: false,
        disableMobile: true,
        static: true,
        defaultDate: initialDate,
        onReady: [syncDateValue],
        onOpen: [updateThaiDateDisplay],
        onChange: [syncDateValue],
        onValueUpdate: [syncDateValue],
        onMonthChange: [(_dates, _text, instance) => updateThaiCalendarYear(instance)],
        onYearChange: [(_dates, _text, instance) => updateThaiCalendarYear(instance)]
      });

      if (picker?.altInput) {
        picker.altInput.placeholder = dateInput.dataset.datePlaceholder || 'เลือกวันที่';
        picker.altInput.setAttribute('aria-label', picker.altInput.placeholder);
      }

      const notifyDateTimeChange = () => group.dispatchEvent(new CustomEvent('datetimechange'));
      timeInput.addEventListener('input', notifyDateTimeChange);
      timeInput.addEventListener('change', notifyDateTimeChange);

      return { group, dateValueInput, dateInput, timeInput, timeChoice, picker };
    }).filter(Boolean);

    const startState = dateTimeStates.find(state => state.group.dataset.datetimeRole === 'start');
    const endState = dateTimeStates.find(state => state.group.dataset.datetimeRole === 'end');
    const jobForm = dateTimeStates[0]?.group.closest('form');
    jobForm?.addEventListener('submit', () => {
      dateTimeStates.forEach(state => {
        const selectedDate = state.picker.selectedDates[0];
        state.dateValueInput.value = selectedDate ? formatIsoDate(selectedDate) : '';
      });
    });
    if (startState && endState) {
      let constrainedStartDate = '';
      const getSelectedDateValue = state => state.picker.selectedDates[0]
        ? formatIsoDate(state.picker.selectedDates[0])
        : state.dateValueInput.value;
      const validateAppointmentRange = () => {
        const startDate = getSelectedDateValue(startState);
        if (startDate !== constrainedStartDate) {
          constrainedStartDate = startDate;
          endState.picker.set('minDate', startDate || null);
        }

        const currentStartDate = getSelectedDateValue(startState);
        const currentEndDate = getSelectedDateValue(endState);
        const sameDay = currentStartDate && currentStartDate === currentEndDate;
        if (sameDay && startState.timeInput.value) {
          Array.from(endState.timeChoice.options).forEach(option => {
            option.disabled = Boolean(option.value && option.value <= startState.timeInput.value);
          });
          endState.timeChoice.refreshSearchableSelect?.();
          if (endState.timeChoice.value && endState.timeChoice.value <= startState.timeInput.value) {
            endState.timeChoice.value = '';
            endState.timeInput.value = '';
            endState.timeChoice.dispatchEvent(new Event('change', { bubbles: true }));
          }
        } else {
          Array.from(endState.timeChoice.options).forEach(option => { option.disabled = false; });
          endState.timeChoice.refreshSearchableSelect?.();
        }

        const startValue = currentStartDate && startState.timeInput.value
          ? `${currentStartDate}T${startState.timeInput.value.slice(0, 5)}`
          : '';
        const endValue = currentEndDate && endState.timeInput.value
          ? `${currentEndDate}T${endState.timeInput.value.slice(0, 5)}`
          : '';
        const invalidRange = startValue && endValue && endValue <= startValue;
        endState.timeChoice.setCustomValidity(invalidRange ? 'เวลาสิ้นสุดต้องอยู่หลังเวลาเริ่ม' : '');
      };
      startState.group.addEventListener('datetimechange', validateAppointmentRange);
      endState.group.addEventListener('datetimechange', validateAppointmentRange);
      validateAppointmentRange();
      jobForm.applyJobScheduleDefaults = () => {
        const defaults = getDefaultJobSchedule();
        endState.picker.set('minDate', null);
        startState.dateValueInput.value = defaults.startDate;
        endState.dateValueInput.value = defaults.endDate;
        startState.picker.setDate(defaults.startDate, true, 'Y-m-d');
        endState.picker.setDate(defaults.endDate, true, 'Y-m-d');
        startState.timeChoice.value = defaults.startTime;
        endState.timeChoice.value = defaults.endTime;
        startState.timeChoice.dispatchEvent(new Event('change', { bubbles: true }));
        endState.timeChoice.dispatchEvent(new Event('change', { bubbles: true }));
        startState.timeChoice.rebuildSearchableSelect?.();
        endState.timeChoice.rebuildSearchableSelect?.();
        validateAppointmentRange();
      };
      if (!jobForm.closest('.modal') && ![startState.dateValueInput, endState.dateValueInput,
        startState.timeInput, endState.timeInput].some(field => field.value)) jobForm.applyJobScheduleDefaults();
    }
  }

  const initializeSearchableSelects = root => root.querySelectorAll('select[data-searchable-select]').forEach(select => {
    if (select.dataset.searchableReady === 'true') return;
    select.dataset.searchableReady = 'true';

    const wrapper = document.createElement('div');
    wrapper.className = 'searchable-select';
    select.parentNode.insertBefore(wrapper, select);
    wrapper.appendChild(select);
    select.classList.add('searchable-select-native');
    if (select.classList.contains('mb-3')) {
      select.classList.remove('mb-3');
      wrapper.classList.add('mb-3');
    }

    const control = document.createElement('button');
    control.type = 'button';
    control.className = 'searchable-select-control';
    control.setAttribute('role', 'combobox');
    control.setAttribute('aria-haspopup', 'listbox');
    control.setAttribute('aria-expanded', 'false');
    control.disabled = select.disabled;

    const selectedLabel = document.createElement('span');
    selectedLabel.className = 'searchable-select-value';
    const caret = document.createElement('span');
    caret.className = 'searchable-select-caret';
    caret.setAttribute('aria-hidden', 'true');
    control.append(selectedLabel, caret);

    const dropdown = document.createElement('div');
    dropdown.className = 'searchable-select-dropdown';

    const search = document.createElement('input');
    search.type = 'search';
    search.className = 'searchable-select-input';
    search.placeholder = select.dataset.searchPlaceholder || 'พิมพ์เพื่อค้นหา...';
    search.autocomplete = 'off';
    search.setAttribute('aria-label', search.placeholder);

    let options = Array.from(select.options);
    const list = document.createElement('div');
    list.className = 'searchable-select-options';
    list.setAttribute('role', 'listbox');
    const noResults = document.createElement('div');
    noResults.className = 'searchable-select-empty';
    noResults.textContent = 'ไม่พบตัวเลือกที่ค้นหา';
    noResults.hidden = true;

    const buildOptionButtons = () => options.map(option => {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'searchable-select-option';
      button.dataset.value = option.value;
      button.textContent = option.textContent.trim();
      button.disabled = option.disabled;
      button.setAttribute('role', 'option');
      list.appendChild(button);
      return button;
    });
    let optionButtons = buildOptionButtons();

    dropdown.append(search, list, noResults);
    wrapper.append(control, dropdown);

    const updateSelection = () => {
      control.disabled = select.disabled;
      const selected = select.selectedOptions[0] || options[0];
      selectedLabel.textContent = selected?.textContent.trim() || 'เลือกข้อมูล';
      selectedLabel.classList.toggle('is-placeholder', !selected?.value);
      optionButtons.forEach((button, index) => {
        const active = button.dataset.value === select.value;
        button.textContent = options[index].textContent.trim();
        button.disabled = options[index].disabled || select.disabled;
        button.classList.toggle('is-selected', active);
        button.setAttribute('aria-selected', String(active));
      });
      wrapper.classList.remove('is-invalid');
    };
    select.refreshSearchableSelect = updateSelection;

    const filterOptions = () => {
      const query = search.value.trim().toLocaleLowerCase('th-TH');
      let visibleCount = 0;
      optionButtons.forEach((button, index) => {
        const option = options[index];
        const matches = query.length === 0 || option.textContent.toLocaleLowerCase('th-TH').includes(query);
        button.hidden = !matches || (query.length > 0 && !option.value);
        if (!button.hidden) visibleCount += 1;
      });
      noResults.hidden = visibleCount > 0;
    };

    const closeDropdown = () => {
      wrapper.classList.remove('is-open');
      control.setAttribute('aria-expanded', 'false');
      search.value = '';
      filterOptions();
    };
    wrapper.closeSearchableSelect = closeDropdown;

    const openDropdown = () => {
      if (control.disabled) return;
      document.querySelectorAll('.searchable-select.is-open').forEach(openSelect => {
        if (openSelect !== wrapper) openSelect.closeSearchableSelect?.();
      });
      wrapper.classList.add('is-open');
      control.setAttribute('aria-expanded', 'true');
      filterOptions();
      requestAnimationFrame(() => search.focus());
    };

    control.addEventListener('click', () => wrapper.classList.contains('is-open') ? closeDropdown() : openDropdown());
    control.addEventListener('keydown', event => {
      if (['Enter', ' ', 'ArrowDown'].includes(event.key)) {
        event.preventDefault();
        openDropdown();
      }
    });

    search.addEventListener('input', filterOptions);
    search.addEventListener('keydown', event => {
      const visibleOptions = optionButtons.filter(button => !button.hidden && !button.disabled);
      if (event.key === 'ArrowDown' && visibleOptions.length) {
        event.preventDefault();
        visibleOptions[0].focus();
      }
      if (event.key === 'Escape') {
        event.preventDefault();
        closeDropdown();
        control.focus();
      }
      if (event.key === 'Enter' && visibleOptions.length) {
        event.preventDefault();
        visibleOptions[0].click();
      }
    });

    const bindOptionButtons = () => optionButtons.forEach(button => {
      button.addEventListener('click', () => {
        select.value = button.dataset.value;
        select.dispatchEvent(new Event('change', { bubbles: true }));
        updateSelection();
        closeDropdown();
        control.focus();
      });
      button.addEventListener('keydown', event => {
        if (event.key === 'Escape') {
          event.preventDefault();
          closeDropdown();
          control.focus();
          return;
        }
        if (!['ArrowDown', 'ArrowUp'].includes(event.key)) return;
        event.preventDefault();
        const visibleOptions = optionButtons.filter(item => !item.hidden && !item.disabled);
        const currentIndex = visibleOptions.indexOf(button);
        const nextIndex = event.key === 'ArrowDown'
          ? Math.min(currentIndex + 1, visibleOptions.length - 1)
          : Math.max(currentIndex - 1, 0);
        visibleOptions[nextIndex]?.focus();
      });
    });
    bindOptionButtons();
    select.rebuildSearchableSelect = () => {
      options = Array.from(select.options);
      list.replaceChildren();
      optionButtons = buildOptionButtons();
      bindOptionButtons();
      updateSelection();
      filterOptions();
    };

    select.addEventListener('change', updateSelection);
    select.addEventListener('invalid', event => {
      event.preventDefault();
      wrapper.classList.add('is-invalid');
      control.focus();
    });
    updateSelection();
  });
  initializeSearchableSelects(document);

  document.addEventListener('click', event => {
    if (event.target.closest('.searchable-select')) return;
    document.querySelectorAll('.searchable-select.is-open').forEach(wrapper => wrapper.closeSearchableSelect?.());
  });

  document.querySelectorAll('[data-product-select]').forEach(select => select.addEventListener('change', () => {
    const option = select.selectedOptions[0];
    const price = document.querySelector(select.dataset.priceTarget);
    const unit = document.querySelector(select.dataset.unitTarget);
    if (price && option?.dataset.price) price.value = option.dataset.price;
    if (unit && option?.dataset.unit) unit.value = option.dataset.unit;
  }));

  const sortableNumber = value => {
    const normalized = value.replace(/[฿,%\s]/g, '').replace(/,/g, '');
    return /^-?\d+(?:\.\d+)?$/.test(normalized) ? Number(normalized) : null;
  };
  const sortableDate = value => {
    const match = /^(\d{1,2})\/(\d{1,2})\/(\d{4})(?:\s+(\d{1,2}):(\d{2}))?$/.exec(value);
    if (!match) return null;
    const [, day, month, year, hour = '0', minute = '0'] = match;
    return Date.UTC(Number(year), Number(month) - 1, Number(day), Number(hour), Number(minute));
  };
  const sortableValue = cell => {
    const value = (cell.dataset.sortValue || cell.innerText || cell.textContent).trim().replace(/\s+/g, ' ');
    const date = sortableDate(value);
    if (date !== null) return { type: 'number', value: date };
    const number = sortableNumber(value);
    if (number !== null) return { type: 'number', value: number };
    return { type: 'text', value };
  };
  const tableCollator = new Intl.Collator('th', { numeric: true, sensitivity: 'base' });

  const initializeSortableTables = root => root.querySelectorAll('table thead').forEach(head => {
    const table = head.closest('table');
    if (table.closest('#receipt') || table.classList.contains('receipt-table') || !table.tBodies.length) return;

    const headers = Array.from(head.rows[0]?.cells || []);
    const body = table.tBodies[0];
    const originalRows = Array.from(body.rows);
    const dataRows = originalRows.filter(row => row.cells.length === headers.length);
    const sortableHeaders = headers.filter(header => {
      const label = header.innerText.trim();
      return label && label !== 'จัดการ' && !header.querySelector('button, input, select');
    });
    if (!sortableHeaders.length) return;

    let activeHeader = null;
    let direction = 'none';
    sortableHeaders.forEach(header => {
      const label = header.innerText.trim();
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'table-sort-button';
      button.setAttribute('aria-label', `เรียงตาม${label}`);
      const caption = document.createElement('span');
      caption.textContent = label;
      const icon = document.createElement('i');
      icon.className = 'bi bi-arrow-down-up table-sort-icon';
      icon.setAttribute('aria-hidden', 'true');
      button.append(caption, icon);
      header.replaceChildren(button);
      header.classList.add('is-sortable');
      header.setAttribute('aria-sort', 'none');

      button.addEventListener('click', () => {
        if (activeHeader === header) {
          direction = direction === 'ascending' ? 'descending' : direction === 'descending' ? 'none' : 'ascending';
        } else {
          activeHeader = header;
          direction = 'ascending';
        }

        sortableHeaders.forEach(item => {
          item.setAttribute('aria-sort', item === activeHeader && direction !== 'none' ? direction : 'none');
          const itemIcon = item.querySelector('.table-sort-icon');
          itemIcon.className = item === activeHeader && direction === 'ascending'
            ? 'bi bi-sort-up table-sort-icon'
            : item === activeHeader && direction === 'descending'
              ? 'bi bi-sort-down table-sort-icon'
              : 'bi bi-arrow-down-up table-sort-icon';
        });

        const columnIndex = headers.indexOf(header);
        const rows = direction === 'none' ? [...originalRows] : [...dataRows].sort((left, right) => {
          const a = sortableValue(left.cells[columnIndex]);
          const b = sortableValue(right.cells[columnIndex]);
          const result = a.type === 'number' && b.type === 'number'
            ? a.value - b.value
            : tableCollator.compare(a.value, b.value);
          return direction === 'ascending' ? result : -result;
        });
        rows.forEach(row => body.appendChild(row));
      });
    });
  });
  initializeSortableTables(document);

  document.addEventListener('click', async event => {
    const link = event.target.closest('[data-ajax-pagination] a.page-link[href]');
    const table = link?.closest('[data-ajax-table]');
    if (!link || !table || link.closest('.page-item.disabled')) return;

    event.preventDefault();
    table.classList.add('ajax-table-loading');
    table.setAttribute('aria-busy', 'true');
    try {
      const response = await fetch(link.href, {
        headers: { Accept: 'text/html', 'X-Requested-With': 'XMLHttpRequest' }
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);

      const pageDocument = new DOMParser().parseFromString(await response.text(), 'text/html');
      const nextTable = pageDocument.querySelector(`[data-ajax-table="${table.dataset.ajaxTable}"]`);
      if (!nextTable) throw new Error('ไม่พบส่วนตารางในผลลัพธ์');

      table.replaceWith(nextTable);
      initializeSortableTables(nextTable);
      initializeConfirmForms(nextTable);
      nextTable.querySelector('.page-item.active .page-link')?.focus({ preventScroll: true });
    } catch (error) {
      table.classList.remove('ajax-table-loading');
      table.removeAttribute('aria-busy');
      Swal.fire({ toast: true, position: 'top-end', icon: 'error', title: 'โหลดรายการไม่สำเร็จ', text: 'กรุณาลองใหม่อีกครั้ง', showConfirmButton: false, timer: 3000 });
    }
  });

  document.addEventListener('click', event => {
    const card = event.target.closest('[data-job-status-filter]');
    const form = document.querySelector('form[data-ajax-search="jobs"]');
    if (!card || !form) return;
    const table = document.querySelector('[data-ajax-table="jobs"]');
    if (table?.getAttribute('aria-busy') === 'true') return;
    form.elements.namedItem('status').value = card.dataset.jobStatusFilter;
    form.requestSubmit();
  });

  document.addEventListener('submit', async event => {
    const form = event.target.closest('form[data-ajax-search]');
    if (!form) return;

    event.preventDefault();
    const tableId = form.dataset.ajaxSearch;
    const table = document.querySelector(`[data-ajax-table="${tableId}"]`);
    if (!table) return;

    const url = new URL(form.action || window.location.href, window.location.origin);
    url.search = new URLSearchParams(new FormData(form)).toString();
    url.searchParams.delete('page');
    table.classList.add('ajax-table-loading');
    table.setAttribute('aria-busy', 'true');
    try {
      const response = await fetch(url, {
        headers: { Accept: 'text/html', 'X-Requested-With': 'XMLHttpRequest' }
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);

      const pageDocument = new DOMParser().parseFromString(await response.text(), 'text/html');
      const nextTable = pageDocument.querySelector(`[data-ajax-table="${tableId}"]`);
      if (!nextTable) throw new Error('ไม่พบส่วนตารางในผลลัพธ์');

      table.replaceWith(nextTable);
      if (tableId === 'jobs') {
        const selectedStatus = pageDocument.querySelector('form[data-ajax-search="jobs"] select[name="status"]').value;
        document.querySelectorAll('[data-job-status-filter]').forEach(card => {
          card.setAttribute('aria-pressed', String(card.dataset.jobStatusFilter === selectedStatus));
          const nextCard = [...pageDocument.querySelectorAll('[data-job-status-filter]')]
            .find(candidate => candidate.dataset.jobStatusFilter === card.dataset.jobStatusFilter);
          if (nextCard) card.querySelector('.kpi-value').textContent = nextCard.querySelector('.kpi-value').textContent;
        });
      }
      initializeSortableTables(nextTable);
      initializeConfirmForms(nextTable);
    } catch (error) {
      table.classList.remove('ajax-table-loading');
      table.removeAttribute('aria-busy');
      Swal.fire({ toast: true, position: 'top-end', icon: 'error', title: 'ค้นหารายการไม่สำเร็จ', text: 'กรุณาลองใหม่อีกครั้ง', showConfirmButton: false, timer: 3000 });
    }
  });

  const dashboardModal = document.getElementById('dashboardDetailsModal');
  if (dashboardModal) {
    let cardsController;
    const refreshCards = async () => {
      const root = document.querySelector('[data-dashboard-cards]');
      cardsController?.abort();
      cardsController = new AbortController();
      const signal = cardsController.signal;
      const url = new URL('/dashboard/cards', location.origin);
      url.searchParams.set('date', root.querySelector('[data-dashboard-date]').value);
      url.searchParams.set('month', root.querySelector('[data-dashboard-month]').value);
      root.setAttribute('aria-busy', 'true');
      try {
        const response = await fetch(url, { signal, headers: { Accept: 'text/html' } });
        if (!response.ok) throw new Error('โหลดสรุปไม่สำเร็จ');
        const page = new DOMParser().parseFromString(await response.text(), 'text/html');
        const next = page.querySelector('[data-dashboard-cards]');
        if (!next) throw new Error('ไม่พบข้อมูลสรุป');
        if (signal.aborted) return;
        root.querySelector('[data-dashboard-date]')._flatpickr?.destroy();
        root.replaceWith(next);
        initializeDashboardFilters();
      } catch (error) {
        if (!signal.aborted) {
          root.removeAttribute('aria-busy');
          Swal.fire({ icon: 'error', title: 'โหลดสรุปไม่สำเร็จ', text: 'กรุณาลองเลือกช่วงเวลาอีกครั้ง' });
        }
      }
    };
    const initializeDashboardFilters = () => {
      const input = document.querySelector('[data-dashboard-date]');
      if (input && window.flatpickr) flatpickr(input, {
        locale: flatpickr.l10ns.th, dateFormat: 'Y-m-d', altInput: true, altFormat: 'j F Y',
        disableMobile: true, allowInput: false,
        onReady: [updateThaiDateDisplay], onOpen: [updateThaiDateDisplay],
        onChange: [updateThaiDateDisplay, refreshCards],
        onMonthChange: [(_dates, _text, picker) => updateThaiCalendarYear(picker)],
        onYearChange: [(_dates, _text, picker) => updateThaiCalendarYear(picker)]
      });
    };
    initializeDashboardFilters();
    document.addEventListener('change', event => {
      if (!event.target.matches('[data-dashboard-month-number], [data-dashboard-year]')) return;
      const root = document.querySelector('[data-dashboard-cards]');
      const year = root.querySelector('[data-dashboard-year]');
      if (!year.reportValidity()) return;
      root.querySelector('[data-dashboard-month]').value = `${String(Number(year.value) - 543).padStart(4,'0')}-${String(root.querySelector('[data-dashboard-month-number]').value).padStart(2,'0')}`;
      refreshCards();
    });
    document.addEventListener('click', event => {
      const button = event.target.closest('[data-dashboard-reset]');
      if (!button) return;
      const parts = Object.fromEntries(new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Bangkok', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date()).map(part => [part.type, part.value]));
      const today = `${parts.year}-${parts.month}-${parts.day}`;
      if (button.dataset.dashboardReset === 'day') document.querySelector('[data-dashboard-date]')._flatpickr.setDate(today, false);
      else document.querySelector('[data-dashboard-month]').value = today.slice(0, 7);
      refreshCards();
    });
    const body = dashboardModal.querySelector('[data-dashboard-details-body]');
    let controller;
    const loadDetails = async (page = 0) => {
      controller?.abort();
      controller = new AbortController();
      const signal = controller.signal;
      body.innerHTML = '<div class="text-center text-muted py-5"><span class="spinner-border spinner-border-sm me-2"></span>กำลังโหลดรายการ...</div>';
      const url = new URL('/dashboard/details', location.origin);
      url.searchParams.set('kind', dashboardModal.dataset.kind);
      url.searchParams.set('scope', dashboardModal.dataset.scope);
      if (dashboardModal.dataset.date) url.searchParams.set('date', dashboardModal.dataset.date);
      if (dashboardModal.dataset.month) url.searchParams.set('month', dashboardModal.dataset.month);
      url.searchParams.set('page', page);
      try {
        const response = await fetch(url, { signal, headers: { 'X-Requested-With': 'XMLHttpRequest' } });
        if (!response.ok) throw new Error('โหลดรายการไม่สำเร็จ');
        const html = await response.text();
        if (signal.aborted) return;
        body.innerHTML = html;
        initializeSortableTables(body);
      } catch (error) {
        if (!signal.aborted) body.innerHTML = '<div class="text-danger text-center py-4">โหลดรายการไม่สำเร็จ กรุณาปิดแล้วลองใหม่</div>';
      }
    };
    dashboardModal.addEventListener('show.bs.modal', event => {
      dashboardModal.dataset.kind = event.relatedTarget.dataset.dashboardKind;
      dashboardModal.dataset.scope = event.relatedTarget.dataset.dashboardScope;
      dashboardModal.dataset.date = event.relatedTarget.dataset.dashboardSelectedDate || '';
      dashboardModal.dataset.month = event.relatedTarget.dataset.dashboardSelectedMonth || '';
      dashboardModal.querySelector('.modal-title').textContent = event.relatedTarget.querySelector('.kpi-label').textContent;
      loadDetails();
    });
    dashboardModal.addEventListener('click', event => {
      const button = event.target.closest('[data-dashboard-page]');
      if (button && !button.disabled) loadDetails(button.dataset.dashboardPage);
    });
    dashboardModal.addEventListener('hidden.bs.modal', () => { controller?.abort(); body.replaceChildren(); });
  }

  const paymentEntryModal = document.getElementById('recordPaymentModal');
  if (paymentEntryModal) {
    const form = paymentEntryModal.querySelector('[data-payment-entry]');
    const jobInput = form.elements.namedItem('jobId');
    const modalTitle = paymentEntryModal.querySelector('#recordPaymentModalTitle');
    const amount = form.elements.namedItem('amount');
    const type = form.elements.namedItem('paymentType');
    const submitButton = form.querySelector('[data-payment-submit]');
    const errorBox = form.querySelector('[data-payment-error]');
    const remainingLabel = form.querySelector('[data-payment-remaining]');
    const amountHint = form.querySelector('[data-payment-amount-hint]');
    const slipInput = form.querySelector('[data-payment-slip]');
    const slipStatus = form.querySelector('[data-slip-status]');
    const slipCancel = form.querySelector('[data-slip-cancel]');
    const slipPicker = form.querySelector('[data-slip-picker]');
    const slipDropzone = form.querySelector('[data-slip-dropzone]');
    const slipPreview = form.querySelector('[data-slip-preview]');
    const slipEmpty = form.querySelector('[data-slip-empty]');
    const slipRemove = form.querySelector('[data-slip-remove]');
    const slipExpand = form.querySelector('[data-slip-expand]');
    const slipResult = form.querySelector('[data-slip-result]');
    const slipResultTitle = form.querySelector('[data-slip-result-title]');
    const slipReread = form.querySelector('[data-slip-reread]');
    const slipZoom = form.querySelector('[data-slip-zoom]');
    const slipZoomImage = form.querySelector('[data-slip-zoom-image]');
    const money = new Intl.NumberFormat('th-TH', { style: 'currency', currency: 'THB' });
    let jobsRequest = 0;
    let submitting = false;
    let slipReading = false;
    let slipRequest = 0;
    let slipController;
    let slipAmountError = '';
    let previewFile = null;
    let previewUrl = null;
    const syncPaymentControls = () => {
      const remaining = jobInput.dataset.remaining;
      submitButton.disabled = submitting || slipReading || !remaining;
      slipInput.disabled = submitting || slipReading || !remaining || !type.value;
      slipPicker.classList.toggle('disabled', slipInput.disabled);
      slipPicker.setAttribute('aria-disabled', String(slipInput.disabled));
      slipDropzone.setAttribute('aria-disabled', String(slipInput.disabled));
      slipReread.disabled = slipInput.disabled || !previewFile;
      slipRemove.disabled = submitting || !previewFile;
      slipCancel.hidden = !slipReading;
    };
    const cancelSlip = () => {
      slipRequest += 1;
      slipController?.abort();
      slipController = null;
      slipReading = false;
      slipInput.value = '';
      slipStatus.hidden = true;
      syncPaymentControls();
    };
    const clearSlipPreview = () => {
      cancelSlip();
      previewFile = null;
      slipPreview.removeAttribute('src');
      slipZoomImage.removeAttribute('src');
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      previewUrl = null;
      slipPreview.hidden = true;
      slipZoom.hidden = true;
      slipExpand.hidden = true;
      slipEmpty.hidden = false;
      slipResult.hidden = true;
      slipDropzone.classList.remove('is-dragging');
      form.querySelectorAll('[data-slip-value]').forEach(field => { field.textContent = '—'; });
      slipAmountError = '';
      syncPaymentControls();
    };
    const updateBalance = () => {
      slipAmountError = '';
      const remaining = jobInput.dataset.remaining;
      remainingLabel.textContent = remaining ? money.format(Number(remaining)) : 'ไม่สามารถรับชำระใบงานนี้ได้';
      if (remaining) amount.max = remaining;
      else amount.removeAttribute('max');
      const manualAmount = ['DEPOSIT', 'OTHER'].includes(type.value);
      amount.readOnly = !manualAmount || !remaining;
      amount.value = remaining && ['FULL', 'BALANCE'].includes(type.value) ? remaining : '';
      amountHint.textContent = manualAmount ? 'กรอกจำนวนเงินได้ไม่เกินยอดคงเหลือ' :
        type.value ? 'ระบบกำหนดยอดคงเหลือให้อัตโนมัติ ไม่สามารถแก้ไขจำนวนเงินได้' : 'กรุณาเลือกประเภทการชำระก่อน';
      syncPaymentControls();
    };
    type.addEventListener('change', () => { cancelSlip(); updateBalance(); });
    amount.addEventListener('input', () => {
      slipAmountError = '';
      if (amount.max && amount.value !== '' && Number(amount.value) > Number(amount.max)) {
        amount.value = amount.max;
        amountHint.textContent = `กรอกได้สูงสุด ${money.format(Number(amount.max))} เท่านั้น`;
      }
    });
    slipCancel.addEventListener('click', cancelSlip);
    slipRemove.addEventListener('click', clearSlipPreview);
    slipExpand.addEventListener('click', () => {
      if (!previewUrl) return;
      slipZoom.hidden = false;
      slipZoom.querySelector('[data-slip-zoom-close]').focus();
    });
    slipZoom.querySelector('[data-slip-zoom-close]').addEventListener('click', () => {
      slipZoom.hidden = true;
      slipExpand.focus();
    });
    slipZoom.addEventListener('keydown', event => {
      if (event.key === 'Tab') {
        event.preventDefault();
        slipZoom.querySelector('[data-slip-zoom-close]').focus();
      }
    });
    const note = form.elements.namedItem('note');
    note.addEventListener('input', () => { form.querySelector('[data-payment-note-count]').textContent = `${note.value.length}/500`; });
    // Manual edits cancel an in-flight read so OCR cannot overwrite newer input.
    ['input', 'change', 'datetimechange'].forEach(name => form.addEventListener(name, event => {
      if (slipReading && event.target !== slipInput) cancelSlip();
    }));
    const readSlip = async () => {
      let file = previewFile;
      if (!file || slipInput.disabled) return;
      cancelSlip();
      slipResult.hidden = true;
      slipAmountError = '';
      errorBox.hidden = true;
      if (!amount.readOnly) amount.value = '';
      const requestId = slipRequest;
      slipController = new AbortController();
      const controller = slipController;
      slipReading = true;
      syncPaymentControls();
      slipStatus.hidden = false;
      slipStatus.className = 'mt-2 small text-muted';
      slipStatus.textContent = 'กำลังเตรียมอ่านสลิปในเครื่อง...';
      const timeout = setTimeout(() => controller.abort(), 120000);
      try {
        const result = await PaymentSlip.read(file, {
          signal: controller.signal,
          onProgress: progress => {
            if (requestId === slipRequest) slipStatus.textContent = progress === null
              ? 'กำลังเตรียมเครื่องมืออ่านสลิป...' : `กำลังอ่านสลิป ${progress}%`;
          }
        });
        if (requestId !== slipRequest) return;
        slipReading = false;
        const values = { date: result.date ? result.date.split('-').reverse().join('/') : null,
          time: result.time, amount: result.amount ? `${Number(result.amount).toLocaleString('th-TH', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} บาท` : null };
        form.querySelectorAll('[data-slip-value]').forEach(field => { field.textContent = values[field.dataset.slipValue] || '—'; });
        const filled = [];
        const warnings = [];
        if (result.date) {
          form.querySelector('[data-thai-date]')._flatpickr.setDate(result.date, true, 'Y-m-d');
          form.elements.namedItem('paymentDay').value = result.date;
          filled.push('วันที่');
        } else warnings.push('อ่านวันที่ไม่ชัดเจน');
        if (result.time) {
          const time = form.elements.namedItem('paymentTime');
          if (![...time.options].some(option => option.value === result.time)) {
            const option = new Option(result.time, result.time);
            option.dataset.slipTime = 'true';
            time.add(option);
          }
          time.value = result.time;
          time.rebuildSearchableSelect?.();
          time.dispatchEvent(new Event('change', { bubbles: true }));
          filled.push('เวลา');
        } else warnings.push('อ่านเวลาไม่ชัดเจน');
        if (result.amount) {
          const remaining = Number(amount.max);
          if (Number(result.amount) > remaining) {
            slipAmountError = `ยอดในสลิป ${money.format(Number(result.amount))} เกินยอดคงเหลือ ไม่ได้นำยอดมาใส่ กรุณาตรวจสอบ`;
            warnings.push(slipAmountError);
          } else if (amount.readOnly && Number(result.amount) !== remaining) {
            slipAmountError = `ยอดในสลิป ${money.format(Number(result.amount))} ไม่ตรงกับยอดที่ต้องชำระ กรุณาเลือกประเภทมัดจำหรืออื่น ๆ หากชำระบางส่วน`;
            warnings.push(slipAmountError);
          } else {
            amount.value = result.amount;
            filled.push(`ยอดเงิน ${money.format(Number(result.amount))}`);
          }
        } else warnings.push('อ่านยอดเงินไม่ชัดเจน');
        if (!form.elements.namedItem('paymentMethod').value) form.elements.namedItem('paymentMethod').value = 'TRANSFER';
        slipStatus.className = `mt-2 small ${warnings.length ? 'text-warning-emphasis' : 'text-success'}`;
        slipStatus.textContent = [filled.length ? `เติม ${filled.join(', ')} แล้ว` : 'ไม่พบข้อมูลที่อ่านได้แน่ชัด',
          ...warnings, 'โปรดตรวจสอบข้อมูลก่อนบันทึก'].join(' · ');
        slipResult.hidden = false;
        slipResult.classList.toggle('is-warning', warnings.length > 0);
        slipResultTitle.textContent = warnings.length ? 'อ่านข้อมูลได้บางส่วน / กรุณาตรวจสอบ' : '✓ อ่านข้อมูลจากสลิปสำเร็จ';
      } catch (error) {
        if (requestId !== slipRequest) return;
        slipStatus.className = 'mt-2 small text-danger';
        slipStatus.textContent = error.name === 'AbortError'
          ? 'อ่านสลิปนานเกินไป กรุณาลองใหม่หรือกรอกข้อมูลเอง'
          : `${error.message || 'อ่านสลิปไม่สำเร็จ'} สามารถกรอกข้อมูลเองได้ ภาพไม่ถูกเก็บไว้`;
      } finally {
        clearTimeout(timeout);
        file = null;
        if (requestId === slipRequest) {
          slipReading = false;
          slipController = null;
          syncPaymentControls();
        }
      }
    };
    const selectSlip = file => {
      if (!file || slipInput.disabled) return;
      try { PaymentSlip.validateImage(file); }
      catch (error) {
        slipStatus.hidden = false;
        slipStatus.className = 'mt-2 small text-danger';
        slipStatus.textContent = error.message;
        return;
      }
      clearSlipPreview();
      previewFile = file;
      previewUrl = URL.createObjectURL(file);
      slipPreview.src = previewUrl;
      slipZoomImage.src = previewUrl;
      slipPreview.hidden = false;
      slipEmpty.hidden = true;
      slipExpand.hidden = false;
      syncPaymentControls();
      readSlip();
    };
    slipInput.addEventListener('change', () => {
      const file = slipInput.files[0];
      slipInput.value = ''; // Unnamed input: images never enter payment FormData.
      selectSlip(file);
    });
    slipReread.addEventListener('click', readSlip);
    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(name => slipDropzone.addEventListener(name, event => {
      event.preventDefault();
      event.stopPropagation();
      slipDropzone.classList.toggle('is-dragging', !slipInput.disabled && ['dragenter', 'dragover'].includes(name));
      if (name === 'drop' && !slipInput.disabled) {
        if (event.dataTransfer.files.length !== 1) {
          slipStatus.hidden = false;
          slipStatus.className = 'mt-2 small text-danger';
          slipStatus.textContent = 'กรุณาเลือกภาพสลิปครั้งละ 1 ภาพ';
          return;
        }
        selectSlip(event.dataTransfer.files[0]);
      }
    }));
    // Prevent dropping an image outside the target from navigating away from this menu.
    ['dragover', 'drop'].forEach(name => document.addEventListener(name, event => {
      if (paymentEntryModal.classList.contains('show')) event.preventDefault();
    }));

    document.addEventListener('show.bs.modal', async event => {
      if (event.target !== paymentEntryModal) return;
      clearSlipPreview();
      form.elements.namedItem('paymentTime').querySelectorAll('[data-slip-time]').forEach(option => option.remove());
      form.elements.namedItem('paymentTime').rebuildSearchableSelect?.();
      const requestId = ++jobsRequest;
      errorBox.hidden = true;
      form.querySelector('[data-thai-date]')._flatpickr?.clear();
      amount.removeAttribute('max');
      amount.readOnly = true;
      amountHint.textContent = 'กรุณาเลือกประเภทการชำระก่อน';
      const selectedJob = event.relatedTarget?.dataset.paymentJobId;
      const selectedJobNo = event.relatedTarget?.dataset.paymentJobNo;
      jobInput.value = '';
      delete jobInput.dataset.remaining;
      modalTitle.textContent = selectedJobNo ? `บันทึกการชำระเงิน · ${selectedJobNo}` : 'บันทึกการชำระเงิน';
      submitButton.disabled = true;
      remainingLabel.textContent = 'กำลังโหลด...';
      syncPaymentControls();
      if (!selectedJob) {
        remainingLabel.textContent = 'ไม่พบใบงาน';
        errorBox.textContent = 'กรุณากดชำระเงินจากรายการใบงานที่ต้องการ';
        errorBox.hidden = false;
        return;
      }
      try {
        const response = await fetch('/payments/payable-jobs', { headers: { Accept: 'application/json' } });
        if (!response.ok) throw new Error('โหลดใบงานไม่สำเร็จ กรุณาลองอีกครั้ง');
        const jobs = await response.json();
        if (requestId !== jobsRequest) return;
        const job = jobs.find(item => String(item.id) === selectedJob);
        if (job && Number(job.remaining) > 0) {
          jobInput.value = String(job.id);
          jobInput.dataset.remaining = job.remaining;
          modalTitle.textContent = `บันทึกการชำระเงิน · ${job.jobNo}`;
        } else {
          errorBox.textContent = 'ใบงานนี้ไม่มียอดคงเหลือ หรือไม่สามารถรับชำระได้แล้ว';
          errorBox.hidden = false;
        }
        updateBalance();
      } catch (error) {
        if (requestId !== jobsRequest) return;
        remainingLabel.textContent = 'โหลดไม่สำเร็จ';
        errorBox.textContent = 'โหลดใบงานไม่สำเร็จ กรุณาปิดแล้วลองอีกครั้ง';
        errorBox.hidden = false;
      }
    });
    paymentEntryModal.addEventListener('hide.bs.modal', clearSlipPreview);
    paymentEntryModal.addEventListener('hidden.bs.modal', () => { jobsRequest += 1; });

    form.addEventListener('submit', async event => {
      event.preventDefault();
      if (slipAmountError) {
        errorBox.textContent = slipAmountError;
        errorBox.hidden = false;
        return;
      }
      if (submitting || slipReading || !jobInput.value || !jobInput.dataset.remaining || !form.reportValidity()) return;
      submitting = true;
      syncPaymentControls();
      submitButton.disabled = true;
      submitButton.textContent = 'กำลังบันทึก...';
      errorBox.hidden = true;
      try {
        const response = await fetch(form.action, {
          method: 'POST', body: new FormData(form),
          headers: { Accept: 'application/json', 'X-Requested-With': 'XMLHttpRequest' }
        });
        if (!response.headers.get('Content-Type')?.includes('application/json')) throw new Error('ไม่สามารถบันทึกได้ กรุณาเข้าสู่ระบบใหม่แล้วลองอีกครั้ง');
        const result = await response.json();
        if (!response.ok) throw new Error(result.message || 'บันทึกการชำระเงินไม่สำเร็จ');
        bootstrap.Modal.getOrCreateInstance(paymentEntryModal).hide();
        Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: result.message, showConfirmButton: false, timer: 3000 });
        try {
          const filter = document.querySelector('form[data-ajax-search="payments"]');
          const url = new URL('/payments', location.origin);
          url.search = new URLSearchParams(new FormData(filter)).toString();
          const tableResponse = await fetch(url, { headers: { Accept: 'text/html', 'X-Requested-With': 'XMLHttpRequest' } });
          if (!tableResponse.ok) throw new Error('โหลดตารางไม่สำเร็จ');
          const page = new DOMParser().parseFromString(await tableResponse.text(), 'text/html');
          const nextTable = page.querySelector('[data-ajax-table="payments"]');
          if (!nextTable) throw new Error('ไม่พบตาราง');
          document.querySelector('[data-ajax-table="payments"]').replaceWith(nextTable);
          initializeSortableTables(nextTable);
        } catch (error) {
          Swal.fire({ icon: 'info', title: 'บันทึกการชำระแล้ว', text: 'โหลดตารางล่าสุดไม่สำเร็จ กรุณากดค้นหาเพื่อโหลดรายการอีกครั้ง' });
        }
      } catch (error) {
        errorBox.textContent = error.message;
        errorBox.hidden = false;
      } finally {
        submitting = false;
        syncPaymentControls();
        submitButton.textContent = 'บันทึกการชำระ';
      }
    });
  }

  if (!document.body.classList.contains('receipt-page')) {
    const receiptModal = document.createElement('div');
    receiptModal.className = 'modal fade';
    receiptModal.id = 'receiptPreviewModal';
    receiptModal.tabIndex = -1;
    receiptModal.setAttribute('aria-labelledby', 'receiptPreviewModalTitle');
    receiptModal.setAttribute('aria-hidden', 'true');
    receiptModal.innerHTML = `
      <div class="modal-dialog modal-xl modal-dialog-centered modal-fullscreen-lg-down">
        <div class="modal-content">
          <div class="modal-header">
            <h2 class="modal-title fs-5" id="receiptPreviewModalTitle">ใบเสร็จรับเงิน / ใบคืนเงิน</h2>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="ปิด"></button>
          </div>
          <div class="modal-body p-0">
            <div class="text-center text-muted p-4" data-receipt-message role="status">กำลังโหลดเอกสาร...</div>
            <iframe title="ใบเสร็จรับเงิน / ใบคืนเงิน" class="w-100 border-0" style="height:70vh" hidden></iframe>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-primary" data-receipt-print disabled><i class="bi bi-printer me-2"></i>พิมพ์ / บันทึกเป็นไฟล์ PDF</button>
            <button type="button" class="btn btn-outline-primary" data-receipt-download disabled><i class="bi bi-image me-2"></i>ดาวน์โหลดรูปภาพ PNG</button>
            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">ปิด</button>
          </div>
        </div>
      </div>`;
    document.body.appendChild(receiptModal);
    const preview = bootstrap.Modal.getOrCreateInstance(receiptModal);
    const frame = receiptModal.querySelector('iframe');
    const message = receiptModal.querySelector('[data-receipt-message]');
    const printButton = receiptModal.querySelector('[data-receipt-print]');
    const downloadButton = receiptModal.querySelector('[data-receipt-download]');
    let previousModal = null;
    let receiptOpening = false;
    let receiptLoadRequest = 0;

    document.addEventListener('click', async event => {
      const link = event.target.closest('a[data-receipt-preview]');
      if (!link) return;
      event.preventDefault();
      if (receiptOpening) return;
      receiptOpening = true;
      const requestId = ++receiptLoadRequest;
      const url = new URL(link.href);
      url.searchParams.set('embedded', 'true');
      message.hidden = false;
      message.textContent = 'กำลังโหลดเอกสาร...';
      frame.hidden = true;
      printButton.disabled = true;
      downloadButton.disabled = true;
      previousModal = link.closest('.modal.show');
      if (previousModal) {
        previousModal.addEventListener('hidden.bs.modal', () => preview.show(), { once: true });
        bootstrap.Modal.getOrCreateInstance(previousModal).hide();
      } else {
        preview.show();
      }
      try {
        const response = await fetch(url, { headers: { Accept: 'text/html', 'X-Requested-With': 'XMLHttpRequest' } });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const markup = await response.text();
        if (requestId !== receiptLoadRequest) return;
        const receiptDocument = new DOMParser().parseFromString(markup, 'text/html');
        if (!receiptDocument.getElementById('receipt')) throw new Error('ไม่พบเอกสาร');
        frame.srcdoc = markup;
      } catch (error) {
        if (requestId !== receiptLoadRequest) return;
        message.textContent = 'โหลดเอกสารไม่สำเร็จ กรุณาปิดแล้วลองอีกครั้ง';
      }
    });

    frame.addEventListener('load', () => {
      if (!receiptOpening || frame.contentWindow.location.href === 'about:blank') return;
      if (!frame.contentDocument.getElementById('receipt')) {
        message.textContent = 'โหลดเอกสารไม่สำเร็จ กรุณาปิดแล้วลองอีกครั้ง';
        return;
      }
      message.hidden = true;
      frame.hidden = false;
      printButton.disabled = false;
      downloadButton.disabled = false;
      frame.contentDocument.addEventListener('keydown', event => {
        if (event.key !== 'Escape') return;
        event.preventDefault();
        event.stopImmediatePropagation();
        preview.hide();
      }, true);
    });
    printButton.addEventListener('click', () => frame.contentWindow.print());
    downloadButton.addEventListener('click', () => frame.contentWindow.downloadReceiptPng());
    receiptModal.addEventListener('hidden.bs.modal', () => {
      receiptOpening = false;
      receiptLoadRequest += 1;
      frame.removeAttribute('srcdoc');
      frame.src = 'about:blank';
      if (previousModal?.isConnected) bootstrap.Modal.getOrCreateInstance(previousModal).show();
      previousModal = null;
    });
  }

  window.downloadReceiptPng = () => {
    const receipt = document.getElementById('receipt');
    if (!receipt) return;
    const scale = 2, width = receipt.offsetWidth, height = receipt.scrollHeight;
    const copy = receipt.cloneNode(true);
    const originals = [receipt, ...receipt.querySelectorAll('*')];
    const copies = [copy, ...copy.querySelectorAll('*')];
    originals.forEach((element, index) => {
      const style = getComputedStyle(element);
      for (const property of style) copies[index].style.setProperty(property, style.getPropertyValue(property));
    });
    Object.assign(copy.style, { margin: '0', boxShadow: 'none', width: `${width}px` });
    const markup = new XMLSerializer().serializeToString(copy);
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}"><foreignObject width="100%" height="100%">${markup}</foreignObject></svg>`;
    const image = new Image();
    const fail = () => Swal.fire({ icon: 'error', title: 'ดาวน์โหลดภาพไม่สำเร็จ', text: 'กรุณาลองอีกครั้ง หรือบันทึกเป็นไฟล์ PDF' });
    image.onerror = fail;
    image.onload = () => {
      try {
        const canvas = document.createElement('canvas');
        canvas.width = width * scale;
        canvas.height = height * scale;
        const context = canvas.getContext('2d');
        context.scale(scale, scale);
        context.fillStyle = '#fff';
        context.fillRect(0, 0, width, height);
        context.drawImage(image, 0, 0);
        const link = document.createElement('a');
        link.download = 'receipt.png';
        link.href = canvas.toDataURL('image/png');
        link.click();
      } catch (error) {
        fail();
      }
    };
    image.src = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
  };
})();
