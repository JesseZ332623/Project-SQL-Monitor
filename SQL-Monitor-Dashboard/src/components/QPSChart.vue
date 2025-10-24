<template>
    <div class="chart-container">
        <div v-if="loading && !chartData.labels.length" class="chart-loading">
            <div class="loading-spinner"></div>
            <p>Loading QPS data...</p>
        </div>
        <canvas v-else ref="chartCanvas"></canvas>

        <!-- <div class="chart-unit-indicator">
            Unit: Queries/s
        </div> -->
    </div>
</template>

<script>
import { ref, onMounted, watch, onUnmounted, nextTick } from 'vue'
import Chart from 'chart.js/auto'

export default {
    name: 'QPSChart',
    props: {
        chartData: {
            type: Object,
            required: true
        },
        loading: {
            type: Boolean,
            default: false
        }
    },
    setup(props) {
        const chartCanvas = ref(null)
        let chartInstance = null

        const initChart = () => {
            if (!chartCanvas.value) {
                console.error('Canvas element not found')
                return
            }

            try {
                const ctx = chartCanvas.value.getContext('2d')
                chartInstance = new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: props.chartData.labels || [],
                        datasets: (props.chartData.datasets || []).map(dataset => ({
                            ...dataset,
                            data: dataset.data || []
                        }))
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                display: true,
                                position: 'top',
                                labels: {
                                    color: '#8b949e',
                                    usePointStyle: true
                                }
                            },
                            tooltip: {
                                mode: 'index',
                                intersect: false,
                                backgroundColor: '#161b22',
                                titleColor: '#c9d1d9',
                                bodyColor: '#c9d1d9',
                                borderColor: '#30363d',
                                borderWidth: 1,
                                callbacks: {
                                    label: function (context) {
                                        return `QPS: ${context.parsed.y.toFixed(2)}`
                                    }
                                }
                            }
                        },
                        scales: {
                            x: {
                                grid: {
                                    color: 'rgba(48, 54, 61, 0.5)',
                                    drawBorder: false
                                },
                                ticks: {
                                    color: '#8b949e',
                                    maxRotation: 0,
                                    callback: function (value, index, values) {
                                        return index % Math.ceil(values.length / 6) === 0 ? this.getLabelForValue(value) : ''
                                    }
                                }
                            },
                            y: {
                                grid: {
                                    color: 'rgba(48, 54, 61, 0.5)',
                                    drawBorder: false
                                },
                                ticks: {
                                    color: '#8b949e'
                                },
                                beginAtZero: true
                            }
                        },
                        interaction: {
                            intersect: false,
                            mode: 'nearest'
                        },
                        animation: {
                            duration: 0
                        }
                    }
                })
            } catch (error) {
                console.error('Error creating QPS chart:', error)
            }
        }

        const updateChart = () => {
            if (chartInstance && props.chartData) {
                try {
                    chartInstance.data.labels = props.chartData.labels || []
                    chartInstance.data.datasets = (props.chartData.datasets || []).map(dataset => ({
                        ...dataset,
                        data: dataset.data || []
                    }))
                    chartInstance.update()
                }
                catch (error) {
                    console.error('Error updating QPS chart:', error)
                }
            }
        }

        onMounted(() => {
            nextTick(() => {
                initChart()
            })
        })

        onUnmounted(() => {
            if (chartInstance) {
                chartInstance.destroy()
            }
        })

        watch(() => props.chartData, (newData) => {
            if (chartInstance) {
                updateChart()
            } else {
                initChart()
            }
        }, { deep: true })

        return {
            chartCanvas
        }
    }
}
</script>

<style scoped>
.chart-unit-indicator {
    position: absolute;
    top: 10px;
    right: 10px;
    background: rgba(22, 27, 34, 0.8);
    padding: 4px 8px;
    border-radius: 4px;
    font-size: 12px;
    color: var(--text-secondary);
    border: 1px solid var(--border-primary);
}

.chart-container {
    position: relative;
    width: 100%;
    min-height: 300px;
}

.chart-container canvas {
    display: block;
    width: 100% !important;
    height: 100% !important;
}

.chart-loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 300px;
    color: var(--text-secondary);
}

.loading-spinner {
    width: 40px;
    height: 40px;
    border: 3px solid var(--border-primary);
    border-top: 3px solid var(--accent-primary);
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin-bottom: 16px;
}

@keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
}
</style>