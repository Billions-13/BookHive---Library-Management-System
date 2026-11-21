/*
 * BookHive – shared front-end behaviour (vanilla JavaScript, no build step).
 *
 * Templates opt in with data attributes, so pages keep working without JavaScript:
 *   form[data-confirm]               ask before submitting (delete, discard, cancel...)
 *   form[data-confirm-when-value]    only ask when the form's "status" select has one of these values
 *   select[data-autosubmit]          submit the filter form as soon as the value changes
 *   input[data-table-filter=#id]     filter table rows by text (accents ignored)
 *   select[data-row-filter=#id]      filter table rows by their data-status
 *   [data-filter-empty=#id]          message shown when the filters hide every row
 *   input[data-match=otherId]        must equal another field (password confirmation)
 *   input[data-cover-preview=#img]   live preview of a cover image URL
 *   input[data-slug-from=#name]      fill a URL slug from a Vietnamese name
 *   input[type=file][data-max-bytes] check size/extension/header of a CSV before upload
 *   [data-autohide]                  alert that closes itself after a few seconds
 *   [data-history-back]              "Back" button
 *   [data-availability-url]          book detail: refresh stock counters every 30 s
 *
 * window.BookHive exposes fetchJson(), confirmDialog() and notify() for page scripts.
 */
(() => {
    'use strict';

    const $ = (selector, root = document) => root.querySelector(selector);
    const $$ = (selector, root = document) => Array.from(root.querySelectorAll(selector));

    /** Lower-case and strip Vietnamese accents so "nguyen" matches "Nguyễn". */
    const fold = (text) => (text || '')
        .normalize('NFD').replace(/[̀-ͯ]/g, '')
        .replace(/đ/g, 'd').replace(/Đ/g, 'D')
        .toLowerCase();

    const debounce = (fn, wait = 300) => {
        let timer;
        return (...args) => {
            clearTimeout(timer);
            timer = setTimeout(() => fn(...args), wait);
        };
    };

    const formatBytes = (bytes) => bytes >= 1024 * 1024
        ? `${(bytes / 1024 / 1024).toFixed(1)} MB`
        : `${Math.max(1, Math.round(bytes / 1024))} KB`;

    /* ------------------------------------------------------------------ */
    /* Notifications (Bootstrap toasts)                                    */
    /* ------------------------------------------------------------------ */

    function notify(message, type = 'success') {
        let container = $('#bhToasts');
        if (!container) {
            container = document.createElement('div');
            container.id = 'bhToasts';
            container.className = 'toast-container position-fixed bottom-0 end-0 p-3';
            container.setAttribute('aria-live', 'polite');
            document.body.appendChild(container);
        }
        const toast = document.createElement('div');
        toast.className = `toast align-items-center text-bg-${type === 'error' ? 'danger' : type} border-0`;
        toast.setAttribute('role', type === 'error' ? 'alert' : 'status');
        toast.innerHTML = '<div class="d-flex"><div class="toast-body"></div>'
            + '<button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Đóng"></button></div>';
        toast.querySelector('.toast-body').textContent = message;
        container.appendChild(toast);
        if (window.bootstrap) {
            const instance = bootstrap.Toast.getOrCreateInstance(toast, { delay: 5000 });
            toast.addEventListener('hidden.bs.toast', () => toast.remove());
            instance.show();
        } else {
            toast.classList.add('show');
            setTimeout(() => toast.remove(), 5000);
        }
    }

    /* ------------------------------------------------------------------ */
    /* fetch() wrapper: CSRF header, JSON, Vietnamese error messages       */
    /* ------------------------------------------------------------------ */

    async function fetchJson(url, options = {}) {
        const headers = new Headers(options.headers || {});
        headers.set('Accept', 'application/json');
        const method = (options.method || 'GET').toUpperCase();
        if (method !== 'GET' && method !== 'HEAD') {
            const token = $('meta[name="_csrf"]')?.content;
            const header = $('meta[name="_csrf_header"]')?.content;
            if (token && header) headers.set(header, token);
        }
        let response;
        try {
            response = await fetch(url, { credentials: 'same-origin', ...options, headers });
        } catch (networkError) {
            throw new Error('Không kết nối được máy chủ. Kiểm tra mạng và thử lại.');
        }
        if (response.redirected && response.url.includes('/login')) {
            throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
        }
        if (response.status === 401 || response.status === 403) {
            throw new Error('Bạn không có quyền thực hiện thao tác này.');
        }
        if (!response.ok) {
            throw new Error(`Máy chủ trả về lỗi ${response.status}. Vui lòng thử lại sau.`);
        }
        const type = response.headers.get('Content-Type') || '';
        if (!type.includes('application/json')) {
            throw new Error('Dữ liệu trả về không đúng định dạng.');
        }
        return response.json();
    }

    /* ------------------------------------------------------------------ */
    /* Confirmation dialog                                                 */
    /* ------------------------------------------------------------------ */

    function confirmModal() {
        let modal = $('#bhConfirmModal');
        if (modal) return modal;
        modal = document.createElement('div');
        modal.id = 'bhConfirmModal';
        modal.className = 'modal fade';
        modal.tabIndex = -1;
        modal.setAttribute('aria-labelledby', 'bhConfirmTitle');
        modal.setAttribute('aria-hidden', 'true');
        modal.innerHTML = `
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header">
                        <h2 class="modal-title fs-5" id="bhConfirmTitle">Xác nhận thao tác</h2>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                    </div>
                    <div class="modal-body" data-confirm-message></div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Huỷ bỏ</button>
                        <button type="button" class="btn btn-warning fw-semibold" data-confirm-ok>Đồng ý</button>
                    </div>
                </div>
            </div>`;
        document.body.appendChild(modal);
        return modal;
    }

    function confirmDialog(message) {
        if (!window.bootstrap) return Promise.resolve(window.confirm(message));
        return new Promise((resolve) => {
            const element = confirmModal();
            const modal = bootstrap.Modal.getOrCreateInstance(element);
            const ok = $('[data-confirm-ok]', element);
            let accepted = false;
            $('[data-confirm-message]', element).textContent = message;
            const onOk = () => { accepted = true; modal.hide(); };
            ok.addEventListener('click', onOk);
            element.addEventListener('shown.bs.modal', () => ok.focus(), { once: true });
            element.addEventListener('hidden.bs.modal', () => {
                ok.removeEventListener('click', onOk);
                resolve(accepted);
            }, { once: true });
            modal.show();
        });
    }

    function needsConfirmation(form) {
        if (!form.dataset.confirm) return false;
        const only = form.dataset.confirmWhenValue;
        if (!only) return true;
        const status = form.elements.namedItem('status');
        return Boolean(status) && only.split(',').includes(status.value);
    }

    /* ------------------------------------------------------------------ */
    /* Form validation with Vietnamese messages                            */
    /* ------------------------------------------------------------------ */

    function validationMessage(field) {
        const v = field.validity;
        if (v.customError) return field.validationMessage;
        if (v.valueMissing) {
            if (field.type === 'file') return 'Vui lòng chọn file.';
            if (field.tagName === 'SELECT') return 'Vui lòng chọn một mục.';
            return 'Vui lòng nhập thông tin này.';
        }
        if (v.typeMismatch) {
            if (field.type === 'email') return 'Email không hợp lệ (ví dụ: ten@hcmiu.edu.vn).';
            if (field.type === 'url') return 'Đường dẫn phải bắt đầu bằng http:// hoặc https://';
            return 'Giá trị không hợp lệ.';
        }
        if (v.tooShort) return `Cần ít nhất ${field.minLength} ký tự (hiện có ${field.value.length}).`;
        if (v.tooLong) return `Tối đa ${field.maxLength} ký tự.`;
        if (v.rangeUnderflow) return `Giá trị nhỏ nhất là ${field.min}.`;
        if (v.rangeOverflow) return `Giá trị lớn nhất là ${field.max}.`;
        if (v.badInput || v.stepMismatch) return 'Vui lòng nhập một số hợp lệ.';
        if (v.patternMismatch) return field.dataset.patternMessage || 'Sai định dạng.';
        return '';
    }

    function feedbackFor(field) {
        const holder = field.parentElement;
        let feedback = holder.querySelector('.invalid-feedback');
        if (!feedback) {
            feedback = document.createElement('div');
            feedback.className = 'invalid-feedback';
            holder.appendChild(feedback);
        }
        return feedback;
    }

    function checkMatch(field) {
        if (!field.dataset.match) return;
        const other = document.getElementById(field.dataset.match);
        const mismatch = other && field.value && field.value !== other.value;
        field.setCustomValidity(mismatch ? 'Mật khẩu nhập lại không khớp.' : '');
    }

    function validateField(field) {
        if (!field.willValidate) return true;
        checkMatch(field);
        const valid = field.checkValidity();
        field.classList.toggle('is-invalid', !valid);
        if (!valid) {
            feedbackFor(field).textContent = validationMessage(field);
            field.setAttribute('aria-invalid', 'true');
        } else {
            field.removeAttribute('aria-invalid');
        }
        return valid;
    }

    function validateForm(form) {
        const fields = $$('input, select, textarea', form).filter((f) => f.willValidate);
        const invalid = fields.filter((f) => !validateField(f));
        if (invalid.length) {
            invalid[0].focus();
            return false;
        }
        return true;
    }

    function initValidation() {
        $$('form').forEach((form) => {
            const constrained = $$('[required], [minlength], [maxlength], [min], [max], [pattern], [type=email], [type=url], [data-match]', form);
            if (!constrained.length) return;
            form.noValidate = true; // our messages replace the browser's (often English) bubbles
            form.addEventListener('input', (event) => {
                const field = event.target;
                // Re-check fields that are already marked, and the confirmation field when its partner changes.
                if (field.classList.contains('is-invalid') || form.dataset.checked) validateField(field);
                $$(`[data-match="${field.id}"]`, form).forEach((confirm) => {
                    if (confirm.value) validateField(confirm);
                });
            });
            form.addEventListener('change', (event) => {
                if (form.dataset.checked) validateField(event.target);
            });
        });
    }

    /* ------------------------------------------------------------------ */
    /* Submit handling: validation -> confirmation -> loading state        */
    /* ------------------------------------------------------------------ */

    function setLoading(form, submitter) {
        const button = submitter || $('button[type=submit], button:not([type])', form);
        if (!button || button.dataset.loading) return;
        button.dataset.loading = 'true';
        button.dataset.originalHtml = button.innerHTML;
        button.disabled = true;
        button.innerHTML = '<span class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>Đang xử lý...';
        form.setAttribute('aria-busy', 'true');
    }

    function resetLoading() {
        $$('button[data-loading]').forEach((button) => {
            button.disabled = false;
            button.innerHTML = button.dataset.originalHtml;
            delete button.dataset.loading;
        });
        $$('form[aria-busy]').forEach((form) => form.removeAttribute('aria-busy'));
    }

    function initSubmitHandling() {
        document.addEventListener('submit', async (event) => {
            const form = event.target;
            if (!(form instanceof HTMLFormElement)) return;

            if (form.noValidate && !validateForm(form)) {
                form.dataset.checked = 'true';
                event.preventDefault();
                return;
            }

            if (needsConfirmation(form) && form.dataset.confirmed !== 'true') {
                event.preventDefault();
                const submitter = event.submitter;
                if (await confirmDialog(form.dataset.confirm)) {
                    form.dataset.confirmed = 'true';
                    if (form.requestSubmit) form.requestSubmit(submitter && submitter.form === form ? submitter : undefined);
                    else { setLoading(form, submitter); form.submit(); }
                }
                return;
            }
            delete form.dataset.confirmed;
            // GET filter forms are cheap; POST forms get a spinner and cannot be sent twice.
            if ((form.method || 'get').toLowerCase() === 'post') setLoading(form, event.submitter);
        });

        // Pages restored from the back/forward cache must not keep disabled buttons.
        window.addEventListener('pageshow', (event) => {
            if (event.persisted) resetLoading();
        });
    }

    /* ------------------------------------------------------------------ */
    /* Lists: auto-submitting filters and client-side table filters        */
    /* ------------------------------------------------------------------ */

    function initAutoSubmit() {
        $$('select[data-autosubmit]').forEach((select) => {
            select.addEventListener('change', () => {
                const form = select.form;
                if (!form) return;
                if (form.requestSubmit) form.requestSubmit(); else form.submit();
            });
        });
    }

    function applyTableFilter(tableSelector) {
        const table = $(tableSelector);
        if (!table || !table.tBodies.length) return;
        const text = fold($(`[data-table-filter="${tableSelector}"]`)?.value.trim());
        const status = $(`[data-row-filter="${tableSelector}"]`)?.value || '';
        let visible = 0;
        Array.from(table.tBodies[0].rows).forEach((row) => {
            const show = (!text || fold(row.textContent).includes(text))
                && (!status || row.dataset.status === status);
            row.hidden = !show;
            if (show) visible += 1;
        });
        $(`[data-filter-empty="${tableSelector}"]`)?.classList.toggle('d-none', visible > 0);
    }

    function initTableFilters() {
        $$('[data-table-filter]').forEach((input) => {
            const apply = debounce(() => applyTableFilter(input.dataset.tableFilter), 150);
            input.addEventListener('input', apply);
        });
        $$('[data-row-filter]').forEach((select) => {
            select.addEventListener('change', () => applyTableFilter(select.dataset.rowFilter));
        });
    }

    /* ------------------------------------------------------------------ */
    /* Book form: cover preview                                            */
    /* ------------------------------------------------------------------ */

    function initCoverPreview() {
        $$('input[data-cover-preview]').forEach((input) => {
            const img = $(input.dataset.coverPreview);
            if (!img) return;
            const hint = document.createElement('div');
            hint.className = 'small text-danger mt-2 d-none';
            hint.textContent = 'Không tải được ảnh từ link này.';
            img.after(hint);

            img.addEventListener('load', () => {
                img.classList.remove('d-none');
                hint.classList.add('d-none');
            });
            img.addEventListener('error', () => {
                if (!img.getAttribute('src')) return;
                img.classList.add('d-none');
                hint.classList.remove('d-none');
            });

            input.addEventListener('input', debounce(() => {
                const url = input.value.trim();
                hint.classList.add('d-none');
                if (/^https?:\/\/\S+$/i.test(url)) {
                    img.src = url;
                } else {
                    img.removeAttribute('src');
                    img.classList.add('d-none');
                }
            }, 400));
        });
    }

    /* ------------------------------------------------------------------ */
    /* Category form: slug from a Vietnamese name                          */
    /* ------------------------------------------------------------------ */

    const slugify = (text) => fold(text)
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '')
        .slice(0, 120);

    function initSlug() {
        $$('input[data-slug-from]').forEach((slug) => {
            const source = $(slug.dataset.slugFrom);
            if (!source) return;
            slug.dataset.patternMessage = 'Chỉ dùng chữ thường không dấu, số và dấu gạch ngang (vd: lap-trinh-web).';
            // Follow the name until the librarian types a slug of their own.
            let auto = !slug.value || slug.value === slugify(source.value);
            slug.addEventListener('input', () => { auto = slug.value === '' || slug.value === slugify(source.value); });
            source.addEventListener('input', () => {
                if (!auto) return;
                slug.value = slugify(source.value);
                if (slug.classList.contains('is-invalid')) validateField(slug);
            });
        });
    }

    /* ------------------------------------------------------------------ */
    /* CSV import: check the file before uploading it                      */
    /* ------------------------------------------------------------------ */

    const REQUIRED_CSV_COLUMNS = ['isbn', 'title', 'publisher', 'publication_year', 'language'];
    const MAX_CSV_ROWS = 1000;

    /** Blob.text() with a FileReader fallback for older browsers. */
    const readText = (file) => (typeof file.text === 'function'
        ? file.text()
        : new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(String(reader.result));
            reader.onerror = () => reject(reader.error);
            reader.readAsText(file, 'UTF-8');
        }));

    function renderCsvPreview(container, lines) {
        if (!container) return;
        container.replaceChildren();
        if (lines.length < 2) return;
        const table = document.createElement('table');
        table.className = 'table table-sm table-bordered small mb-0 mt-2';
        lines.slice(0, 4).forEach((line, index) => {
            const row = table.insertRow();
            line.split(',').forEach((value) => {
                const cell = document.createElement(index === 0 ? 'th' : 'td');
                cell.textContent = value.trim().replace(/^"|"$/g, '');
                row.appendChild(cell);
            });
        });
        const caption = document.createElement('div');
        caption.className = 'small text-muted mt-2';
        caption.textContent = 'Xem trước 3 dòng đầu:';
        const wrapper = document.createElement('div');
        wrapper.className = 'table-responsive';
        wrapper.appendChild(table);
        container.append(caption, wrapper);
    }

    function initCsvCheck() {
        $$('input[type=file][data-max-bytes]').forEach((input) => {
            const max = Number(input.dataset.maxBytes);
            const info = input.parentElement.querySelector('[data-file-info]');
            const preview = input.parentElement.querySelector('[data-csv-preview]');
            input.addEventListener('change', async () => {
                const file = input.files[0];
                input.setCustomValidity('');
                if (preview) preview.replaceChildren();
                if (!file) {
                    if (info) info.textContent = 'Chưa chọn file.';
                    validateField(input);
                    return;
                }
                if (info) info.textContent = `${file.name} – ${formatBytes(file.size)}`;

                let problem = '';
                if (!/\.csv$/i.test(file.name)) problem = 'Chỉ chấp nhận file có đuôi .csv.';
                else if (file.size === 0) problem = 'File đang rỗng.';
                else if (file.size > max) problem = `File quá lớn (${formatBytes(file.size)}), tối đa ${formatBytes(max)}.`;

                if (!problem) {
                    const text = (await readText(file)).replace(/^\uFEFF/, '');
                    const lines = text.split(/\r?\n/).filter((line) => line.trim() !== '');
                    const header = (lines[0] || '').split(',').map((h) => h.trim().replace(/^"|"$/g, '').toLowerCase());
                    const missing = REQUIRED_CSV_COLUMNS.filter((column) => !header.includes(column));
                    const rows = Math.max(0, lines.length - 1);
                    if (missing.length) problem = `Thiếu cột: ${missing.join(', ')}.`;
                    else if (rows === 0) problem = 'File chỉ có dòng tiêu đề, chưa có sách nào.';
                    else if (rows > MAX_CSV_ROWS) problem = `File có ${rows} dòng, tối đa ${MAX_CSV_ROWS} dòng.`;
                    else {
                        if (info) info.textContent = `${file.name} – ${formatBytes(file.size)} – khoảng ${rows} đầu sách, đủ cột bắt buộc.`;
                        if (preview) renderCsvPreview(preview, lines);
                    }
                }
                input.setCustomValidity(problem);
                validateField(input);
            });
        });
    }

    /* ------------------------------------------------------------------ */
    /* Book detail: keep the stock counters up to date                     */
    /* ------------------------------------------------------------------ */

    const AVAILABILITY_INTERVAL_MS = 30000;

    function initAvailability() {
        const box = $('[data-availability-url]');
        if (!box) return;
        const notice = $('[data-availability-changed]');
        let available = Number(box.dataset.available);
        let timer;

        const refresh = async () => {
            try {
                const data = await fetchJson(box.dataset.availabilityUrl);
                $$('[data-count]', box).forEach((element) => {
                    const value = data[element.dataset.count];
                    if (typeof value === 'number' && element.textContent !== String(value)) {
                        element.textContent = value;
                        element.classList.add('bh-count-changed');
                        setTimeout(() => element.classList.remove('bh-count-changed'), 1500);
                    }
                });
                // Borrow/reserve buttons depend on stock, so crossing zero needs a fresh page.
                if ((available === 0) !== (data.availableCopies === 0) && notice) {
                    $('[data-availability-message]', notice).textContent = data.availableCopies === 0
                        ? 'Cuốn cuối cùng vừa được mượn – sách đã hết. Tải lại trang để đặt trước.'
                        : 'Sách vừa có lại trong kho. Tải lại trang để mượn.';
                    notice.classList.remove('d-none');
                }
                available = data.availableCopies;
            } catch (error) {
                // Background refresh only: keep the numbers already on screen, try again later.
            }
        };

        const schedule = () => {
            clearInterval(timer);
            if (document.visibilityState === 'visible') timer = setInterval(refresh, AVAILABILITY_INTERVAL_MS);
        };
        document.addEventListener('visibilitychange', () => {
            if (document.visibilityState === 'visible') refresh();
            schedule();
        });
        $('[data-reload]', notice || document)?.addEventListener('click', () => window.location.reload());
        schedule();
    }

    /* ------------------------------------------------------------------ */
    /* Small things                                                        */
    /* ------------------------------------------------------------------ */

    function initAlerts() {
        $$('[data-autohide]').forEach((alert) => {
            setTimeout(() => {
                if (window.bootstrap && alert.classList.contains('alert')) bootstrap.Alert.getOrCreateInstance(alert).close();
                else alert.remove();
            }, 6000);
        });
        // Registration celebration on the login page: let it shine, then fade away.
        const sparkles = $('#successSparkleOverlay');
        if (sparkles) {
            setTimeout(() => sparkles.classList.add('bh-fade-out'), 3500);
            sparkles.addEventListener('transitionend', () => sparkles.remove());
        }
    }

    function initHistoryBack() {
        $$('[data-history-back]').forEach((button) => {
            button.addEventListener('click', () => {
                if (window.history.length > 1) window.history.back();
                else window.location.href = '/';
            });
        });
    }

    window.BookHive = { fetchJson, confirmDialog, notify, fold, debounce };

    const start = () => {
        initValidation();
        initSubmitHandling();
        initAutoSubmit();
        initTableFilters();
        initCoverPreview();
        initSlug();
        initCsvCheck();
        initAvailability();
        initAlerts();
        initHistoryBack();
    };
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start);
    else start();
})();
