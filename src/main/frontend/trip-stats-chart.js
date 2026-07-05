import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

class TripStatsChart extends HTMLElement {
    constructor() {
        super();
        this.attachShadow({ mode: 'open' });
        this._chart = null;
        this._data = null;
        this._ready = false;
    }

    connectedCallback() {
        this._ready = false;
        this.shadowRoot.innerHTML = `
            <style>
                :host {
                    display: block;
                    width: 100%;
                    position: relative;
                }
                .chart-wrapper {
                    position: relative;
                    width: 100%;
                    background: var(--chart-bg, #1e2a22);
                    border-radius: 12px;
                    padding: 16px;
                    box-sizing: border-box;
                }
                canvas {
                    width: 100% !important;
                }
                .chart-empty {
                    color: var(--chart-empty-color, #6b8a74);
                    font-size: 0.85rem;
                    text-align: center;
                    padding: 32px 0;
                    display: none;
                }
            </style>
            <div class="chart-wrapper">
                <canvas></canvas>
                <div class="chart-empty">No data available</div>
            </div>
        `;

        if (this._data) {
            this._ready = true;
            this._renderChart();
        } else {
            this._ready = true;
        }
    }

    /**
     * Set chart data from Java via element.data = { labels, datasets }
     * Expected format:
     * {
     *   labels: ["06:00","06:30",...],
     *   datasets: [
     *     { label: "Times Cancelled", data: [2, 5, 0, ...], color: "#e84b4b" },
     *     { label: "Claims Requested", data: [0, 1, 0, ...], color: "#4caf7d" },
     *     { label: "Total Reimbursable Trips", data: [2, 6, 0, ...], color: "#2196f3" }
     *   ]
     * }
     */
    set chartData(value) {
        this._data = value;
        if (this._ready) {
            this._renderChart();
        }
    }

    _renderChart() {
        const canvas = this.shadowRoot.querySelector('canvas');
        const emptyEl = this.shadowRoot.querySelector('.chart-empty');

        if (!this._data || !this._data.labels || this._data.labels.length === 0) {
            canvas.style.display = 'none';
            emptyEl.style.display = 'block';
            if (this._chart) {
                this._chart.destroy();
                this._chart = null;
            }
            return;
        }

        canvas.style.display = 'block';
        emptyEl.style.display = 'none';

        const datasets = (this._data.datasets || []).map(ds => ({
            label: ds.label,
            data: ds.data,
            backgroundColor: ds.color + '33',
            borderColor: ds.color,
            borderWidth: 2,
            borderRadius: 4,
            tension: 0.3,
            fill: ds.fill !== undefined ? ds.fill : false,
            pointRadius: 3,
            pointHoverRadius: 5,
            pointBackgroundColor: ds.color,
        }));

        const config = {
            type: this._data.type || 'bar',
            data: {
                labels: this._data.labels,
                datasets,
            },
            options: {
                responsive: true,
                interaction: {
                    mode: 'index',
                    intersect: false,
                },
                plugins: {
                    legend: {
                        display: true,
                        position: 'top',
                        labels: {
                            color: '#a8d5b5',
                            font: { size: 12, weight: '500' },
                            padding: 16,
                            usePointStyle: true,
                            pointStyle: 'rect',
                            boxWidth: 12,
                            boxHeight: 12,
                        },
                    },
                    tooltip: {
                        backgroundColor: '#162119',
                        titleColor: '#e2ede6',
                        bodyColor: '#a8d5b5',
                        borderColor: '#2e4235',
                        borderWidth: 1,
                    },
                },
                scales: {
                    x: {
                        ticks: {
                            color: '#7aab8a',
                            font: { size: 11 },
                            maxRotation: 90,
                            minRotation: 0,
                            autoSkip: true,
                            autoSkipPadding: 8,
                            maxTicksLimit: 12,
                        },
                        grid: {
                            color: '#2a3d30',
                        },
                    },
                    y: {
                        beginAtZero: true,
                        ticks: {
                            color: '#7aab8a',
                            font: { size: 11 },
                            stepSize: 1,
                        },
                        grid: {
                            color: '#2a3d30',
                        },
                    },
                },
            },
        };

        if (this._chart) {
            this._chart.destroy();
        }
        this._chart = new Chart(canvas, config);
    }
}

customElements.define('trip-stats-chart', TripStatsChart);
