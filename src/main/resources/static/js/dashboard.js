/*
 * BookHive – statistics page (/librarian/dashboard.html). Member 3.
 * Reads GET /api/stats (Member 1) and shows four headline numbers plus one Chart.js bar chart.
 * Needs /js/app.js (BookHive.fetchJson) and Chart.js loaded first.
 */
(() => {
    'use strict';

    const STATS_URL = '/api/stats';
    // One series hue, recessive grid/axes.
    const COLOR = { bar: '#2a78d6', barHover: '#256abf', grid: '#e1e0d9', axis: '#c3c2b7', muted: '#898781', text: '#52514e' };
    const number = new Intl.NumberFormat('vi-VN');

    const updatedAt = document.getElementById('updatedAt');
    const errorBox = document.getElementById('errorBox');
    const refreshButton = document.getElementById('refreshButton');
    let chart;

    function showStats(stats) {
        document.querySelectorAll('[data-stat]').forEach((element) => {
            const value = stats[element.dataset.stat];
            element.textContent = typeof value === 'number' ? number.format(value) : '—';
        });
    }

    /** Copies in circulation split into three parts, kept in their natural order. */
    function circulationParts(stats) {
        const overdue = Math.min(stats.overdueLoans, stats.borrowedCopies);
        return [
            ['Có thể mượn', stats.availableCopies],
            ['Đang mượn – còn hạn', Math.max(0, stats.borrowedCopies - overdue)],
            ['Đang mượn – quá hạn', overdue],
        ];
    }

    function showTable(parts) {
        const table = document.getElementById('circulationTable');
        table.replaceChildren();
        const head = table.createTHead().insertRow();
        ['Nhóm', 'Số cuốn'].forEach((text, i) => {
            const th = document.createElement('th');
            th.scope = 'col';
            th.textContent = text;
            if (i === 1) th.className = 'text-end';
            head.appendChild(th);
        });
        const body = table.createTBody();
        parts.forEach(([label, value]) => {
            const row = body.insertRow();
            row.insertCell().textContent = label;
            const cell = row.insertCell();
            cell.className = 'text-end';
            cell.textContent = number.format(value);
        });
    }

    function showChart(parts) {
        const labels = parts.map(([label]) => label);
        const values = parts.map(([, value]) => value);
        document.getElementById('circulationChart').setAttribute('aria-label',
            `Tình hình các cuốn sách: ${parts.map(([l, v]) => `${l} ${v}`).join(', ')}`);
        if (chart) {
            chart.data.datasets[0].data = values;
            chart.update();
            return;
        }
        chart = new Chart(document.getElementById('circulationChart'), {
            type: 'bar',
            data: {
                labels,
                datasets: [{
                    label: 'Số cuốn',
                    data: values,
                    backgroundColor: COLOR.bar,
                    hoverBackgroundColor: COLOR.barHover,
                    borderRadius: 4,
                    borderSkipped: 'start',
                    maxBarThickness: 28,
                }],
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                interaction: { mode: 'index', axis: 'y', intersect: false },
                plugins: {
                    legend: { display: false },
                    tooltip: { callbacks: { label: (c) => ` Số cuốn: ${number.format(c.parsed.x)}` } },
                },
                scales: {
                    x: { beginAtZero: true, grace: '5%', ticks: { precision: 0, color: COLOR.muted },
                        grid: { color: COLOR.grid }, border: { display: false } },
                    y: { ticks: { color: COLOR.text }, grid: { display: false }, border: { color: COLOR.axis } },
                },
            },
        });
    }

    async function load() {
        refreshButton.disabled = true;
        errorBox.classList.add('d-none');
        updatedAt.textContent = 'Đang tải số liệu...';
        try {
            if (!window.Chart) throw new Error('Không tải được thư viện biểu đồ Chart.js. Kiểm tra kết nối mạng.');
            const stats = await window.BookHive.fetchJson(STATS_URL);
            showStats(stats);
            const parts = circulationParts(stats);
            showTable(parts);
            showChart(parts);
            updatedAt.textContent = `Cập nhật lúc ${new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}`;
        } catch (error) {
            errorBox.textContent = error.message;
            errorBox.classList.remove('d-none');
            updatedAt.textContent = 'Chưa có số liệu';
            document.querySelectorAll('[data-stat]').forEach((element) => {
                if (element.textContent === '…') element.textContent = '—';
            });
        } finally {
            refreshButton.disabled = false;
        }
    }

    refreshButton.addEventListener('click', load);
    load();
})();
