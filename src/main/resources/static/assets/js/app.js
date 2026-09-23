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

  document.querySelectorAll('form.js-confirm').forEach(form => form.addEventListener('submit', event => {
    event.preventDefault();
    Swal.fire({ title: form.dataset.confirmTitle || 'ยืนยันรายการ?', text: form.dataset.confirmText || 'กรุณาตรวจสอบข้อมูลก่อนดำเนินการ', icon: 'warning', showCancelButton: true, confirmButtonText: 'ยืนยัน', cancelButtonText: 'ยกเลิก', confirmButtonColor: '#0878f9' })
      .then(result => { if (result.isConfirmed) form.submit(); });
  }));

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
    }
  }

  document.querySelectorAll('select[data-searchable-select]').forEach(select => {
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

    const options = Array.from(select.options);
    const list = document.createElement('div');
    list.className = 'searchable-select-options';
    list.setAttribute('role', 'listbox');
    const noResults = document.createElement('div');
    noResults.className = 'searchable-select-empty';
    noResults.textContent = 'ไม่พบตัวเลือกที่ค้นหา';
    noResults.hidden = true;

    const optionButtons = options.map(option => {
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

    dropdown.append(search, list, noResults);
    wrapper.append(control, dropdown);

    const updateSelection = () => {
      const selected = select.selectedOptions[0] || options[0];
      selectedLabel.textContent = selected?.textContent.trim() || 'เลือกข้อมูล';
      selectedLabel.classList.toggle('is-placeholder', !selected?.value);
      optionButtons.forEach((button, index) => {
        const active = button.dataset.value === select.value;
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

    optionButtons.forEach(button => {
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

    select.addEventListener('change', updateSelection);
    select.addEventListener('invalid', event => {
      event.preventDefault();
      wrapper.classList.add('is-invalid');
      control.focus();
    });
    updateSelection();
  });

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

  document.querySelectorAll('table thead').forEach(head => {
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

  window.downloadReceiptPng = () => {
    const receipt = document.getElementById('receipt');
    if (!receipt) return;
    const scale = 2, width = 794, height = Math.max(receipt.scrollHeight, 900);
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}"><foreignObject width="100%" height="100%"><div xmlns="http://www.w3.org/1999/xhtml" style="background:white;width:${width}px;min-height:${height}px">${receipt.outerHTML}</div></foreignObject></svg>`;
    const image = new Image(); const canvas = document.createElement('canvas'); canvas.width = width * scale; canvas.height = height * scale;
    image.onload = () => { const ctx = canvas.getContext('2d'); ctx.scale(scale, scale); ctx.fillStyle = '#fff'; ctx.fillRect(0,0,width,height); ctx.drawImage(image,0,0); URL.revokeObjectURL(image.src); const link=document.createElement('a');link.download='receipt.png';link.href=canvas.toDataURL('image/png');link.click(); };
    image.src = URL.createObjectURL(new Blob([svg], {type:'image/svg+xml;charset=utf-8'}));
  };
})();
