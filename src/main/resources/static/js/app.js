/*
 * BookHive – shared JavaScript (vanilla ES6, no build step). Member 3.
 *
 * Works with any page markup; include it once per page:
 *   <script th:src="@{/js/app.js}" defer></script>
 *
 * - Vietnamese messages for the browser's form validation (required, email, min/max...)
 * - Confirmation before destructive POST actions, recognised by the form's URL
 *   (archive, discard, cancel, return, toggle, resolve) or a data-confirm="..." attribute
 * - Submit buttons are disabled while a POST form is being sent (no double submit)
 * - Search forms keep the chosen sort / page size / filters and re-run when sort or size changes
 * - window.BookHive.fetchJson(): fetch() with the CSRF token and Vietnamese error messages
 */
(() => {
    'use strict';

    /* ---------- Form validation messages ---------- */

    function messageFor(field) {
        const v = field.validity;
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
        if (v.tooShort) return `Cần ít nhất ${field.minLength} ký tự.`;
        if (v.tooLong) return `Tối đa ${field.maxLength} ký tự.`;
        if (v.rangeUnderflow) return `Giá trị nhỏ nhất là ${field.min}.`;
        if (v.rangeOverflow) return `Giá trị lớn nhất là ${field.max}.`;
        if (v.badInput || v.stepMismatch) return 'Vui lòng nhập một số hợp lệ.';
        if (v.patternMismatch) return field.dataset.patternMessage || 'Sai định dạng.';
        return '';
    }

    /** data-match="otherId": e.g. password confirmation. */
    function checkMatch(field) {
        if (!field.dataset || !field.dataset.match) return;
        const other = document.getElementById(field.dataset.match);
        const mismatch = other && field.value && field.value !== other.value;
        field.setCustomValidity(mismatch ? 'Mật khẩu nhập lại không khớp.' : '');
    }

    // "invalid" does not bubble, so listen in the capture phase.
    document.addEventListener('invalid', (event) => {
        const field = event.target;
        if (!field.validity.customError) field.setCustomValidity(messageFor(field));
        field.classList.add('is-invalid');
    }, true);

    const clearError = (event) => {
        const field = event.target;
        if (!(field instanceof HTMLInputElement || field instanceof HTMLSelectElement
            || field instanceof HTMLTextAreaElement)) return;
        field.setCustomValidity('');
        checkMatch(field);
        document.querySelectorAll(`[data-match="${field.id}"]`).forEach(checkMatch);
        if (field.checkValidity()) field.classList.remove('is-invalid');
    };
    document.addEventListener('input', clearError);
    document.addEventListener('change', clearError);

    /* ---------- Confirmation before destructive actions ---------- */

    const CONFIRM_BY_URL = [
        [/\/archive$/, 'Ẩn mục này khỏi danh sách (xoá mềm)?'],
        [/\/discard$/, 'Thanh lý toàn bộ lô sách này? Không thể hoàn tác.'],
        [/\/cancel$/, 'Huỷ lượt đặt trước này? Bạn sẽ mất vị trí trong hàng đợi.'],
        [/\/return$/, 'Xác nhận đã nhận trả sách? Nếu trả trễ, tiền phạt được tính tự động.'],
        [/\/toggle$/, 'Đổi trạng thái khoá / mở khoá của tài khoản này?'],
        [/\/resolve$/, 'Cập nhật khoản phạt này? Không thể hoàn tác.'],
    ];

    function confirmMessage(form) {
        if (form.dataset.confirm) return form.dataset.confirm;
        if ((form.method || '').toLowerCase() !== 'post') return null;
        const path = new URL(form.action, window.location.href).pathname;
        // Copy status: only losing or discarding a copy needs a second thought.
        if (/\/copies\/\d+\/status$/.test(path)) {
            const status = form.elements.namedItem('status');
            return status && ['LOST', 'DISCARDED'].includes(status.value)
                ? 'Đánh dấu cuốn sách này là bị mất / đã thanh lý?' : null;
        }
        const rule = CONFIRM_BY_URL.find(([pattern]) => pattern.test(path));
        return rule ? rule[1] : null;
    }

    /* ---------- Submit: confirm, then lock the buttons ---------- */

    function lockButtons(form) {
        form.querySelectorAll('button[type=submit], button:not([type]), input[type=submit]').forEach((button) => {
            button.dataset.originalText = button.textContent;
            // Disable after the browser has collected the form data (a disabled button is not submitted).
            setTimeout(() => {
                button.disabled = true;
                if (button.tagName === 'BUTTON') button.textContent = 'Đang xử lý...';
            }, 0);
        });
    }

    document.addEventListener('submit', (event) => {
        const form = event.target;
        if (!(form instanceof HTMLFormElement)) return;
        const message = confirmMessage(form);
        if (message && !window.confirm(message)) {
            event.preventDefault();
            return;
        }
        if ((form.method || '').toLowerCase() === 'post') lockButtons(form);
    });

    // Coming back with the browser's Back button must not leave buttons disabled.
    window.addEventListener('pageshow', (event) => {
        if (!event.persisted) return;
        document.querySelectorAll('button[data-original-text]').forEach((button) => {
            button.disabled = false;
            button.textContent = button.dataset.originalText;
        });
    });

    /* ---------- fetch() helper for pages that call the JSON API ---------- */

    function csrf() {
        const token = document.querySelector('meta[name="_csrf"]')?.content
            || document.querySelector('input[name="_csrf"]')?.value;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
        return token ? { [header]: token } : {};
    }

    async function fetchJson(url, options = {}) {
        const method = (options.method || 'GET').toUpperCase();
        const headers = { Accept: 'application/json', ...(method === 'GET' ? {} : csrf()), ...(options.headers || {}) };
        let response;
        try {
            response = await fetch(url, { credentials: 'same-origin', ...options, headers });
        } catch (networkError) {
            throw new Error('Không kết nối được máy chủ. Kiểm tra mạng và thử lại.');
        }
        if (response.redirected && new URL(response.url).pathname === '/login') {
            throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
        }
        if (response.status === 401 || response.status === 403) {
            throw new Error('Bạn không có quyền xem dữ liệu này.');
        }
        if (!response.ok) throw new Error(`Máy chủ báo lỗi ${response.status}. Vui lòng thử lại sau.`);
        if (!(response.headers.get('Content-Type') || '').includes('application/json')) {
            throw new Error('Dữ liệu trả về không đúng định dạng.');
        }
        return response.json();
    }

    /* ---------- Search forms: keep the chosen options, apply sort / size at once ---------- */

    const onReady = (fn) => (document.readyState === 'loading'
        ? document.addEventListener('DOMContentLoaded', fn) : fn());

    // Changing one of these only changes how results are shown, so search again immediately.
    const DISPLAY_FIELDS = ['sort', 'direction', 'size'];

    function initSearchForms() {
        const params = new URLSearchParams(window.location.search);
        document.querySelectorAll('form').forEach((form) => {
            if ((form.getAttribute('method') || 'get').toLowerCase() !== 'get') return;
            form.querySelectorAll('select[name]').forEach((select) => {
                // The list pages render these selects without a selected option, so after a search
                // they jump back to the first value. Show the value that is actually in use.
                const value = params.get(select.name);
                if (value !== null && Array.from(select.options).some((o) => o.value === value)) {
                    select.value = value;
                }
                if (DISPLAY_FIELDS.includes(select.name)) {
                    select.addEventListener('change', () => (form.requestSubmit ? form.requestSubmit() : form.submit()));
                }
            });
        });
    }

    onReady(initSearchForms);

    window.BookHive = { fetchJson };
})();
