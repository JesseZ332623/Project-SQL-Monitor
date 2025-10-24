<template>
    <div class="chart-container">
        <div v-if="loading && !chartData.labels.length" class="chart-loading">
            <div class="loading-spinner"></div>
            <p>Loading network traffic data...</p>
        </div>
        <canvas v-else ref="chartCanvas"></canvas>
        
        <!-- <div class="chart-unit-indicator" v-if="currentUnit">
            Unit: {{ currentUnit }}/s
        </div> -->
    </div>
</template>

<script>
import { ref, onMounted, watch, onUnmounted, nextTick } from 'vue'
import Chart from 'chart.js/auto'

export default {
    name: 'NetworkTrafficChart',
    props: {
        chartData: {
            type: Object,
            required: true
        },
        loading: {
            type: Boolean,
            default: false
        },
        currentUnit: {
            type: String,
            default: 'KB'
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
                                        const label = context.dataset.label || ''
                                        const value = context.parsed.y.toFixed(2)
                                        const unit = props.currentUnit || 'KB'
                                        return `${label}: ${value} ${unit}/s`
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
                                    color: '#8b949e',
                                    callback: function(value) {
                                        return value.toFixed(2) + ` ${props.currentUnit || 'KB'}/s`
                                    }
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
                console.error('Error creating network traffic chart:', error)
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
                    
                    // 更新Y轴标签以反映当前单位
                    if (chartInstance.options.scales.y.ticks) {
                        chartInstance.options.scales.y.ticks.callback = function(value) {
                            return value.toFixed(2) + ` ${props.currentUnit || 'KB'}/s`
                        }
                    }
                    
                    chartInstance.update()
                }
                catch (error) {
                    console.error('Error updating network traffic chart:', error)
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

        // 监听单位变化
        watch(() => props.currentUnit, (newUnit) => {
            if (chartInstance) {
                updateChart()
            }
        })

        return {
            chartCanvas
        }
    }
}
</script>

<style scoped>
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

@keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
}
</style>