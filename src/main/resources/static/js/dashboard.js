/*
 * BookHive – statistics dashboard (advanced feature: Dashboard with Charts).
 * Loads GET /api/stats and GET /api/stats/charts, fills the headline numbers and
 * draws three single-series bar charts with Chart.js. Each chart also has a table view.
 */
(() => {
    'use strict';

    const root = document.querySelector('[data-dashboard]');
    if (!root || !window.BookHive) return;

    // Chart colours: one series hue, recessive grid and axes (light theme of the site).
    const INK = {
        series: '#2a78d6',
        seriesHover: '#256abf',
        grid: '#e1e0d9',
        axis: '#c3c2b7',
        muted: '#898781',
        text: '#52514e',
    };

    const STATUS_LABELS = {
        AVAILABLE: 'Có sẵn',
        BORROWED: 'Đang được mượn',
        RESERVED: 'Đang giữ cho bạn đọc',
        DAMAGED: 'Hư hỏng',
        LOST: 'Bị mất',
        DISCARDED: 'Đã thanh lý',
    };

    const number = new Intl.NumberFormat('vi-VN');
    const monthLabel = (yearMonth) => {
        const [year, month] = yearMonth.split('-');
        return `Th${Number(month)}/${year}`;
    };
    const shorten = (text, max = 30) => (text.length > max ? `${text.slice(0, max - 1)}…` : text);

    const charts = {};
    const $ = (selector) => root.querySelector(selector);
    const $$ = (selector) => Array.from(root.querySelectorAll(selector));

    if (window.Chart) {
        Chart.defaults.font.family = 'system-ui, -apple-system, "Segoe UI", Roboto, Arial, sans-serif';
        Chart.defaults.color = INK.text;
    }

    function setChartState(key, state, message = '') {
        const box = $(`[data-chart-box="${key}"]`);
        const overlay = box.querySelector('[data-chart-state]');
        if (state === 'ready') {
            overlay.classList.add('d-none');
            return;
        }
        overlay.classList.remove('d-none');
        overlay.innerHTML = state === 'loading'
            ? '<div class="spinner-border text-secondary" role="status"><span class="visually-hidden">Đang tải</span></div>'
            : `<p class="small text-muted mb-0 text-center px-3"></p>`;
        if (state !== 'loading') overlay.querySelector('p').textContent = message;
    }

    /** One-hue bar chart; horizontal bars for ranked categories, columns for months. */
    function drawBars(key, canvasId, labels, values, { horizontal, seriesName, tickLabel }) {
        const valueAxis = horizontal ? 'x' : 'y';
        const categoryAxis = horizontal ? 'y' : 'x';
        if (charts[key]) {
            charts[key].data.labels = labels;
            charts[key].data.datasets[0].data = values;
            charts[key].update();
            return;
        }
        charts[key] = new Chart(document.getElementById(canvasId), {
            type: 'bar',
            data: {
                labels,
                datasets: [{
                    label: seriesName,
                    data: values,
                    backgroundColor: INK.series,
                    hoverBackgroundColor: INK.seriesHover,
                    borderRadius: 4,
                    borderSkipped: 'start',
                    maxBarThickness: 28,
                }],
            },
            options: {
                indexAxis: categoryAxis,
                responsive: true,
                maintainAspectRatio: false,
                animation: { duration: 300 },
                // Hovering anywhere in a bar's row/column shows its tooltip (bigger target than the bar).
                interaction: { mode: 'index', axis: categoryAxis, intersect: false },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: (context) => ` ${seriesName}: ${number.format(context.parsed[valueAxis])}`,
                        },
                    },
                },
                scales: {
                    [valueAxis]: {
                        beginAtZero: true,
                        grace: '5%', // keep the longest bar and the last tick off the edge
                        ticks: { precision: 0, color: INK.muted },
                        grid: { color: INK.grid },
                        border: { display: false },
                    },
                    [categoryAxis]: {
                        ticks: {
                            color: INK.text,
                            autoSkip: false,
                            callback(value) { return tickLabel(this.getLabelForValue(value)); },
                        },
                        grid: { display: false },
                        border: { color: INK.axis },
                    },
                },
            },
        });
    }

    function fillTable(key, headers, rows) {
        const table = $(`[data-chart-table="${key}"]`);
        table.replaceChildren();
        const head = table.createTHead().insertRow();
        headers.forEach((text, i) => {
            const th = document.createElement('th');
            th.scope = 'col';
            th.textContent = text;
            if (i > 0) th.className = 'text-end';
            head.appendChild(th);
        });
        const body = table.createTBody();
        rows.forEach(([label, value, href]) => {
            const row = body.insertRow();
            const name = row.insertCell();
            if (href) {
                const link = document.createElement('a');
                link.href = href;
                link.textContent = label;
                name.appendChild(link);
            } else {
                name.textContent = label;
            }
            const cell = row.insertCell();
            cell.className = 'text-end';
            cell.textContent = number.format(value);
        });
    }

    function renderStats(stats) {
        $$('[data-stat]').forEach((element) => {
            const value = stats[element.dataset.stat];
            element.textContent = typeof value === 'number' ? number.format(value) : '—';
        });
        $('[data-overdue-hint]').classList.toggle('d-none', !(stats.overdueLoans > 0));
    }

    function renderCharts(data) {
        // Loans per month: change over time -> columns in calendar order.
        const months = data.loansPerMonth.map((m) => monthLabel(m.month));
        const monthValues = data.loansPerMonth.map((m) => m.count);
        drawBars('loansPerMonth', 'loansPerMonthChart', months, monthValues,
            { horizontal: false, seriesName: 'Lượt mượn', tickLabel: (l) => l });
        fillTable('loansPerMonth', ['Tháng', 'Lượt mượn'], months.map((m, i) => [m, monthValues[i]]));
        setChartState('loansPerMonth', 'ready');

        // Copy status: compare parts -> ranked horizontal bars, largest first.
        const statuses = [...data.copyStatus].sort((a, b) => b.count - a.count);
        const statusLabels = statuses.map((s) => STATUS_LABELS[s.status] || s.status);
        const statusValues = statuses.map((s) => s.count);
        const totalCopies = statusValues.reduce((a, b) => a + b, 0);
        fillTable('copyStatus', ['Trạng thái', 'Số cuốn'], statusLabels.map((l, i) => [l, statusValues[i]]));
        if (totalCopies === 0) {
            setChartState('copyStatus', 'empty', 'Kho chưa có cuốn sách nào. Hãy nhập lô sách đầu tiên.');
        } else {
            drawBars('copyStatus', 'copyStatusChart', statusLabels, statusValues,
                { horizontal: true, seriesName: 'Số cuốn', tickLabel: (l) => l });
            setChartState('copyStatus', 'ready');
        }

        // Most borrowed titles: ranked horizontal bars (titles can be long -> shortened ticks, full title in tooltip).
        const bookUrl = root.dataset.bookUrl;
        const titles = data.topBooks.map((b) => b.title);
        const loans = data.topBooks.map((b) => b.loans);
        fillTable('topBooks', ['Tên sách', 'Lượt mượn'],
            data.topBooks.map((b) => [b.title, b.loans, `${bookUrl}${b.bookId}`]));
        if (!titles.length) {
            setChartState('topBooks', 'empty', 'Chưa có lượt mượn nào.');
        } else {
            drawBars('topBooks', 'topBooksChart', titles, loans,
                { horizontal: true, seriesName: 'Lượt mượn', tickLabel: (l) => shorten(l) });
            setChartState('topBooks', 'ready');
        }

        document.getElementById('loansPerMonthChart').setAttribute('aria-label',
            `Lượt mượn theo tháng: ${months.map((m, i) => `${m} ${monthValues[i]}`).join(', ')}`);
        document.getElementById('copyStatusChart').setAttribute('aria-label',
            `Số cuốn theo trạng thái: ${statusLabels.map((l, i) => `${l} ${statusValues[i]}`).join(', ')}`);
        document.getElementById('topBooksChart').setAttribute('aria-label',
            `Sách được mượn nhiều nhất: ${titles.map((t, i) => `${t} ${loans[i]}`).join(', ')}`);
    }

    let loading = false;
    async function load() {
        if (loading) return;
        loading = true;
        const buttons = $$('[data-refresh]');
        buttons.forEach((b) => { b.disabled = true; });
        $('[data-dashboard-error]').classList.add('d-none');
        $('[data-updated-at]').textContent = 'Đang tải số liệu...';
        try {
            if (!window.Chart) throw new Error('Không tải được thư viện biểu đồ (Chart.js). Kiểm tra kết nối mạng.');
            const [stats, chartData] = await Promise.all([
                window.BookHive.fetchJson(root.dataset.statsUrl),
                window.BookHive.fetchJson(root.dataset.chartsUrl),
            ]);
            renderStats(stats);
            renderCharts(chartData);
            const now = new Date();
            $('[data-updated-at]').textContent =
                `Cập nhật lúc ${now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}`;
        } catch (error) {
            $('[data-error-message]').textContent = error.message;
            $('[data-dashboard-error]').classList.remove('d-none');
            $('[data-updated-at]').textContent = 'Chưa có số liệu';
            ['loansPerMonth', 'copyStatus', 'topBooks'].forEach((key) => {
                if (!charts[key]) setChartState(key, 'empty', 'Không có dữ liệu để hiển thị.');
            });
            $$('[data-stat]').forEach((element) => {
                if (element.querySelector('.placeholder')) element.textContent = '—';
            });
        } finally {
            buttons.forEach((b) => { b.disabled = false; });
            loading = false;
        }
    }

    $$('[data-refresh]').forEach((button) => button.addEventListener('click', load));
    load();
})();
