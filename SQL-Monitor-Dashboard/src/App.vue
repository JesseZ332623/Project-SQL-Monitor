<template>
	<div id="app" class="github-dark">
		<div class="container">
			<header class="header">
				<div class="header-content">
					<h1 class="title">
						<svg class="icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor"
							stroke-width="2">
							<path
								d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z">
							</path>
							<polyline points="7.5 4.21 12 6.81 16.5 4.21"></polyline>
							<polyline points="7.5 19.79 7.5 14.6 3 12"></polyline>
							<polyline points="21 12 16.5 14.6 16.5 19.79"></polyline>
							<polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
							<line x1="12" y1="22.08" x2="12" y2="12"></line>
						</svg>
						Database Monitor - {{ baseAddress }}
					</h1>
					<p class="subtitle">Real-time database query performance monitoring</p>
				</div>
			</header>

			<div class="alert error-alert" v-if="error">
				<svg class="alert-icon" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
					<path fill-rule="evenodd"
						d="M8.22 1.754a.25.25 0 00-.44 0L1.698 13.132a.25.25 0 00.22.368h12.164a.25.25 0 00.22-.368L8.22 1.754zm-1.763-.707c.659-1.234 2.427-1.234 3.086 0l6.082 11.378A1.75 1.75 0 0114.082 15H1.918a1.75 1.75 0 01-1.543-2.575L6.457 1.047zM9 11a1 1 0 11-2 0 1 1 0 012 0zm-.25-5.25a.75.75 0 00-1.5 0v2.5a.75.75 0 001.5 0v-2.5z">
					</path>
				</svg>
				<span>Error fetching data: {{ error }}</span>
			</div>

			<div class="controls">
				<div class="control-group">
					<button class="btn btn-primary" @click="fetchData" :disabled="loading">
						<svg class="btn-icon" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
							<path fill-rule="evenodd"
								d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z">
							</path>
						</svg>
						Refresh
					</button>
					<button class="btn" :class="autoRefresh ? 'btn-danger' : 'btn-secondary'"
						@click="toggleAutoRefresh">
						<svg class="btn-icon" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
							<path fill-rule="evenodd"
								d="M8 2.5a5.487 5.487 0 00-4.131 1.869l1.204 1.204A.25.25 0 014.896 6H1.25A.25.25 0 011 5.75V2.104a.25.25 0 01.427-.177l1.38 1.38A7.001 7.001 0 0114.95 7.16a.75.75 0 11-1.49.178A5.501 5.501 0 008 2.5zM1.705 8.005a.75.75 0 01.834.656 5.501 5.501 0 009.592 2.97l-1.204-1.204a.25.25 0 01.177-.427h3.646a.25.25 0 01.25.25v3.646a.25.25 0 01-.427.177l-1.38-1.38A7.001 7.001 0 011.05 8.84a.75.75 0 01.656-.834z">
							</path>
						</svg>
						{{ autoRefresh ? 'Stop Auto-Refresh' : 'Start Auto-Refresh' }}
					</button>
				</div>
				<div class="status-indicator">
					<div class="status-dot" :class="{ active: !loading }"></div>
					<span v-if="autoRefresh">Auto-refresh every </span>
					<select v-if="autoRefresh" v-model.number="refreshInterval" class="btn">
						<option v-for="n in [3, 5, 15, 30, 60]" :key="n" :value="n">{{ n }}</option>
					</select>
					<span v-if="autoRefresh"> seconds</span>
					<span v-else>Manual refresh mode</span>
				</div>
			</div>

			<div class="dashboard">
				<!-- 图表区域改为两列布局 -->
				<div class="charts-grid">
					<div class="chart-section">
						<div class="card">
							<div class="card-header">
								<h2 class="card-title">QPS</h2>
								<div class="card-actions">
									<span class="timestamp">Last updated: {{ lastUpdate || 'Never' }}</span>
								</div>
							</div>
							<div class="card-content">
								<QPSChart :chart-data="qpsChartData" :loading="loading" />
							</div>
						</div>
					</div>

					<div class="chart-section">
						<div class="card">
							<div class="card-header">
								<h2 class="card-title">
									Network Traffic
									<span class="unit-badge" v-if="currentNetworkUnit">
										({{ currentNetworkUnit }}/s)
									</span>
								</h2>
								<div class="card-actions">
									<div class="unit-selector">
										<label>Unit:</label>
										<select v-model="selectedUnit" class="btn btn-sm" @change="onUnitChange">
											<option value="B">Bytes</option>
											<option value="KB">KB</option>
											<option value="MB">MB</option>
										</select>
									</div>
									<span class="timestamp">Last updated: {{ lastUpdate || 'Never' }}</span>
								</div>
							</div>
							<div class="card-content">
								<NetworkTrafficChart 
									:chart-data="networkChartData" 
									:loading="loading"
									:current-unit="currentNetworkUnit"
								/>
							</div>
						</div>
					</div>
				</div>

				<div class="stats-section">
					<div class="card">
						<div class="card-header">
							<h2 class="card-title">Current Metrics</h2>
						</div>
						<div class="card-content">
							<div class="metric-grid">
								<div class="metric-card">
									<div class="metric-label">Server Running Time</div>
									<div class="metric-value" :class="{ loading: loading && !serverRunningTime }">
										{{ serverRunningTime ? serverRunningTime : '--' }}
									</div>
									<div class="metric-description">Server running time begin it was start</div>
								</div>
								<div class="metric-card">
									<div class="metric-label">Current QPS</div>
									<div class="metric-value" :class="{ loading: loading && !qpsData }">
										{{ qpsData ? qpsData.qps.toFixed(2) : '--' }}
									</div>
									<div class="metric-description">Queries per second</div>
								</div>

								<div class="metric-card">
									<div class="metric-label">Total Queries</div>
									<div class="metric-value" :class="{ loading: loading && !qpsData }">
										{{ qpsData ? formatNumber(qpsData.currentQueries) : '--' }}
									</div>
									<div class="metric-description">Cumulative count</div>
								</div>

								<div class="metric-card">
									<div class="metric-label">Query Delta</div>
									<div class="metric-value" :class="{ loading: loading && !qpsData }">
										{{ qpsData ? qpsData.queryDiff : '--' }}
									</div>
									<div class="metric-description">Since last check</div>
								</div>

								<div class="metric-card">
									<div class="metric-label">Time Delta</div>
									<div class="metric-value" :class="{ loading: loading && !qpsData }">
										{{ qpsData ? qpsData.timeDiffMs + 'ms' : '--' }}
									</div>
									<div class="metric-description">Measurement interval</div>
								</div>

								<div class="metric-card">
									<div class="metric-label">Connections Usage</div>
									<div class="metric-value" :class="{ loading: loading && !connectionsData }">
										{{ 
											connectionsData 
												? connectionsData.currentConnections + ' / ' + connectionsData.maxConnections
												: '--' 
										}}
									</div>
									<div class="metric-description">
										{{ 'Usage: ' +  (connectionsData ? connectionsData.connectUsagePercent.toFixed(2) + '%' : '--') }}
									</div>
								</div>

								<div class="metric-card">
									<div class="metric-label">Network Receive</div>
									<div class="metric-value" :class="{ loading: loading && !netTrafficData }">
										{{ 
											netTrafficData 
												? formatNetworkRate(netTrafficData.receivePerSec, netTrafficData.sizeUnit)
												: '--' 
										}}
									</div>
									<div class="metric-description">Real-time receive rate</div>
								</div>

								<div class="metric-card">
									<div class="metric-label">Network Sent</div>
									<div class="metric-value" :class="{ loading: loading && !netTrafficData }">
										{{ 
											netTrafficData 
												? formatNetworkRate(netTrafficData.sentPerSec, netTrafficData.sizeUnit)
												: '--' 
										}}
									</div>
									<div class="metric-description">Real-time send rate</div>
								</div>

								<div class="metric-card">
									<div class="metric-label">Total Traffic</div>
									<div class="metric-value" :class="{ loading: loading && !netTrafficData }">
										{{ 
											netTrafficData 
												? formatBytes(netTrafficData.totalBytesReceive + netTrafficData.totalBytesSent)
												: '--' 
										}}
									</div>
									<div class="metric-description">Cumulative network usage</div>
								</div>

								<div class="metric-card">
									<div class="metric-label">InnoDB Buffer Cache Hit Rate</div>
									<div class="metric-value" :class="{ loading: loading && !innodbBufferCacheHitRate }">
										{{ 
											innodbBufferCacheHitRate
												? (innodbBufferCacheHitRate.cacheHitRate * 100.00).toFixed(4) + ' %'
												: '--' 
										}}
									</div>
									<div class="metric-description">
										Percentage of reads served from InnoDB buffer pool
									</div>
								</div>
							</div>
						</div>
					</div>

					<div class="card">
						<div class="card-header">
							<h2 class="card-title">Status Information</h2>
						</div>
						<div class="card-content">
							<div class="status-info">
								<div class="status-item">
									<span class="status-label">Reset Detected:</span>
									<span class="status-value"
										:class="qpsData && qpsData.resetDetected ? 'warning' : 'normal'">
										{{ qpsData ? (qpsData.resetDetected ? 'Yes' : 'No') : '--' }}
									</span>
								</div>
								<div class="status-item">
									<span class="status-label">Error State:</span>
									<span class="status-value"
										:class="qpsData && qpsData.error ? 'error' : 'normal'">
										{{ qpsData ? (qpsData.error ? 'Yes' : 'No') : '--' }}
									</span>
								</div>
								<div class="status-item">
									<span class="status-label">Auto Unit:</span>
									<span class="status-value"
										:class="autoUnitEnabled ? 'normal' : 'warning'">
										{{ autoUnitEnabled ? 'Enabled' : 'Disabled' }}
									</span>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>

			<footer class="footer">
				<p>Database Monitoring System &copy; 2025</p>
			</footer>
		</div>
	</div>
</template>

<script>
import { ref, onMounted, onUnmounted, reactive, watch, computed } from 'vue'
import QPSChart from './components/QPSChart.vue'
import NetworkTrafficChart from './components/NetworkTrafficChart.vue'
import { 
	fetchServerTime, fetchBaseAddress, 
	fetchQPSData, fetchConnectionsUsage, 
	fetchNetworkTraffic, fetchInnodbBufferCacheHitRate 
} from './services/api'

export default {
	name: 'App',
	components: {
		QPSChart,
		NetworkTrafficChart
	},
	setup() {
		const serverRunningTime = ref(null)
		const baseAddress     = ref(null)
		const qpsData         = ref(null)
		const connectionsData = ref(null)
		const netTrafficData  = ref(null)
		const loading         = ref(false)
		const error           = ref(null)
		const autoRefresh     = ref(true)
		const lastUpdate      = ref(null)
		const refreshInterval = ref(3)
		const selectedUnit    = ref('KB')  // 默认单位
		const autoUnitEnabled = ref(false) // 自动单位切换
		
		// InnoDB Buffer Cache Hit Rate
		const innodbBufferCacheHitRate = ref(null)
		
		let refreshTimer = null
		
		// 立刻初始化一次数据库地址 + 端口
	 	fetchBaseAddress()
			.then((response) => baseAddress.value = response.data)

		// 计算当前网络单位（支持自动切换）
		const currentNetworkUnit = computed(() => {
			if (!autoUnitEnabled.value) {
				return selectedUnit.value
			}

			if (!netTrafficData.value) {
				return selectedUnit.value
			}

			// 自动选择最合适的单位
			const maxRate = Math.max(
				netTrafficData.value.receivePerSec, 
				netTrafficData.value.sentPerSec
			)

			if (maxRate >= 1024) {
				return 'MB'
			} else if (maxRate >= 1) {
				return 'KB'
			} else {
				return 'B'
			}
		})

		// QPS 图表数据
		const qpsChartData = reactive({
			labels: [],
			datasets: [{
				label: 'QPS',
				data: [],
				borderColor: '#3fb950',
				backgroundColor: 'rgba(63, 185, 80, 0.1)',
				borderWidth: 2,
				fill: true,
				tension: 0.4
			}]
		})

		// 网络流量图表数据
		const networkChartData = reactive({
			labels: [],
			datasets: [
				{
					label: 'Receive',
					data: [],
					borderColor: '#1f6feb',
					backgroundColor: 'rgba(31, 111, 235, 0.1)',
					borderWidth: 2,
					fill: true,
					tension: 0.4
				},
				{
					label: 'Send',
					data: [],
					borderColor: '#da3633',
					backgroundColor: 'rgba(218, 54, 51, 0.1)',
					borderWidth: 2,
					fill: true,
					tension: 0.4
				}
			]
		})

		const formatNumber = (num) => {
			return new Intl.NumberFormat().format(num)
		}

		const formatBytes = (bytes) => {
			if (!bytes) return '0 B'
			const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
			const i = Math.floor(Math.log(bytes) / Math.log(1024))
			return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i]
		}

		const formatNetworkRate = (rate, unit) => {
			if (rate === undefined || rate === null) return '--'
			return `${rate.toFixed(2)} ${unit}/s`
		}

		const updateChartData = (chartState, values, timestamp, maxDataPoints = 30) => {
			const newLabels = [...chartState.labels, timestamp]
			const newDatasets = chartState.datasets.map((dataset, index) => ({
				...dataset,
				data: [...dataset.data, values[index] || 0]
			}))

			// 限制数据点数量
			if (newLabels.length > maxDataPoints) {
				newLabels.shift()
				newDatasets.forEach(dataset => dataset.data.shift())
			}

			chartState.labels = newLabels
			chartState.datasets = newDatasets
		}

		const onUnitChange = () => {
			autoUnitEnabled.value = false
			fetchData()
		}

		const fetchData = async () => {
			loading.value = true
			error.value = null

			try {
				const unitToUse = autoUnitEnabled.value ? currentNetworkUnit.value : selectedUnit.value
				
				const [
					qpsResponse, connResponse, 
					netTrafficResponse, serverRunningTimeResponse, 
					innodbBufferCacheHitRateResponse
				] = await Promise.all([
					fetchQPSData(),
					fetchConnectionsUsage(),
					fetchNetworkTraffic(unitToUse),
					fetchServerTime(),
					fetchInnodbBufferCacheHitRate()
				])

				qpsData.value                  = qpsResponse.data
				connectionsData.value          = connResponse.data
				netTrafficData.value           = netTrafficResponse.data
				innodbBufferCacheHitRate.value = innodbBufferCacheHitRateResponse.data

				const runtimeArray = serverRunningTimeResponse.data;
				serverRunningTime.value 
					= `${runtimeArray[0]} days ${runtimeArray[1]} hours ${runtimeArray[2]} minutes ${runtimeArray[3]} seconds`

				// 更新图表数据
				const now = new Date()
				const timeLabel = now.toLocaleTimeString()

				// 更新 QPS 图表
				updateChartData(qpsChartData, [qpsResponse.data.qps], timeLabel)
				
				// 更新网络流量图表
				updateChartData(
					networkChartData, 
					[
						netTrafficResponse.data.receivePerSec, 
						netTrafficResponse.data.sentPerSec
					], 
					timeLabel
				)

				lastUpdate.value = now.toLocaleString()

			} catch (err) {
				error.value = err.message
				console.error('Error fetching data:', err)
			} finally {
				loading.value = false
			}
		}

		const toggleAutoRefresh = () => {
			autoRefresh.value = !autoRefresh.value
			if (autoRefresh.value) {
				startAutoRefresh()
			} else {
				stopAutoRefresh()
			}
		}

		const startAutoRefresh = () => {
			stopAutoRefresh() // 先清除现有的定时器

			console.log('Starting auto-refresh with interval:', refreshInterval.value, 'seconds')
			
			// 立即执行一次数据获取
			// fetchData()
			
			// 设置定时器，无论页面是否可见都会执行
			refreshTimer = setInterval(() => {
				console.log('Auto-refresh triggered')
				fetchData()
			}, refreshInterval.value * 1000)
		}

		const stopAutoRefresh = () => {
			if (refreshTimer) {
				clearInterval(refreshTimer)
				refreshTimer = null
				console.log('Auto-refresh stopped')
			}
		}

		// 监听刷新间隔变化
		watch(refreshInterval, (newInterval) => {
			if (autoRefresh.value) {
				console.log('Refresh interval changed to', newInterval, 'seconds, restarting auto-refresh.')
				startAutoRefresh()
			}
		})

		// 监听单位变化
		watch(currentNetworkUnit, (newUnit) => {
			if (autoUnitEnabled.value) {
				console.log('Auto unit changed to:', newUnit)
			}
		})

		onMounted(() => {
			// 初始数据获取
			fetchData()

			// 如果启用了自动刷新，则启动定时器
			if (autoRefresh.value) {
				startAutoRefresh()
			}
		})

		onUnmounted(() => {
			// 组件卸载时清除定时器
			stopAutoRefresh()
		})

		return {
			innodbBufferCacheHitRate,
			serverRunningTime,
			baseAddress,
			qpsData,
			connectionsData,
			netTrafficData,
			qpsChartData,
			networkChartData,
			loading,
			error,
			autoRefresh,
			lastUpdate,
			refreshInterval,
			selectedUnit,
			currentNetworkUnit,
			autoUnitEnabled,
			fetchData,
			toggleAutoRefresh,
			onUnitChange,
			formatNumber,
			formatBytes,
			formatNetworkRate
		}
	}
}
</script>

<style>

/* 图表网格布局 */
.charts-grid {
	display: grid;
	grid-template-columns: 1fr 1fr;
	gap: 24px;
	margin-bottom: 24px;
}

@media (max-width: 1024px) {
	.charts-grid {
		grid-template-columns: 1fr;
	}
}

/* 单位选择器 */
.unit-selector {
	display: flex;
	align-items: center;
	gap: 8px;
}

.unit-selector label {
	font-size: 12px;
	color: var(--text-secondary);
}

.unit-badge {
	font-size: 12px;
	color: var(--text-secondary);
	font-weight: normal;
	margin-left: 8px;
}

/* 调整指标网格为4列 */
.metric-grid {
	display: grid;
	grid-template-columns: repeat(4, 1fr);
	gap: 16px;
}

@media (max-width: 1200px) {
	.metric-grid {
		grid-template-columns: repeat(2, 1fr);
	}
}

@media (max-width: 768px) {
	.metric-grid {
		grid-template-columns: 1fr;
	}
}

/* 原有的其他样式保持不变 */
* {
	margin: 0;
	padding: 0;
	box-sizing: border-box;
}

.github-dark {
	--bg-primary: #0d1117;
	--bg-secondary: #161b22;
	--bg-tertiary: #21262d;
	--border-primary: #30363d;
	--border-secondary: #3e444c;
	--text-primary: #c9d1d9;
	--text-secondary: #8b949e;
	--text-tertiary: #6e7681;
	--accent-primary: #3fb950;
	--accent-secondary: #1f6feb;
	--accent-danger: #f85149;
	--accent-warning: #d29922;
}

body {
	background-color: var(--bg-primary);
	color: var(--text-primary);
	font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Noto Sans', Helvetica, Arial, sans-serif;
	line-height: 1.5;
}

#app {
	min-height: 100vh;
	background-color: var(--bg-primary);
}

.container {
	max-width: 1400px;
	margin: 0 auto;
	padding: 0 24px;
}

.header {
	padding: 32px 0;
	border-bottom: 1px solid var(--border-primary);
}

.header-content {
	display: flex;
	flex-direction: column;
	gap: 8px;
}

.title {
	display: flex;
	align-items: center;
	gap: 12px;
	font-size: 24px;
	font-weight: 600;
	color: var(--text-primary);
}

.icon {
	color: var(--accent-primary);
}

.subtitle {
	color: var(--text-secondary);
	font-size: 16px;
}

.alert {
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 12px 16px;
	border: 1px solid;
	border-radius: 6px;
	margin: 16px 0;
}

.error-alert {
	border-color: rgba(248, 81, 73, 0.4);
	background-color: rgba(248, 81, 73, 0.1);
	color: var(--accent-danger);
}

.alert-icon {
	flex-shrink: 0;
}

.controls {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin: 24px 0;
	flex-wrap: wrap;
	gap: 16px;
}

.control-group {
	display: flex;
	gap: 12px;
}

.btn {
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 4px 14px;
	border: 1px solid var(--border-primary);
	border-radius: 6px;
	background-color: var(--bg-tertiary);
	color: var(--text-primary);
	font-size: 14px;
	font-weight: 500;
	cursor: pointer;
	transition: all 0.2s;
}

.btn-sm {
	padding: 4px 4px;
	font-size: 12px;
}

.btn:hover:not(:disabled) {
	background-color: var(--bg-secondary);
	border-color: var(--border-secondary);
}

.btn:disabled {
	opacity: 0.6;
	cursor: not-allowed;
}

.btn-primary {
	background-color: var(--accent-primary);
	border-color: var(--accent-primary);
	color: #000;
}

.btn-primary:hover:not(:disabled) {
	background-color: #2ea043;
	border-color: #2ea043;
}

.btn-secondary {
	background-color: var(--bg-tertiary);
	border-color: var(--border-primary);
}

.btn-danger {
	background-color: rgba(248, 81, 73, 0.1);
	border-color: rgba(248, 81, 73, 0.4);
	color: var(--accent-danger);
}

.btn-danger:hover:not(:disabled) {
	background-color: rgba(248, 81, 73, 0.2);
}

.btn-icon {
	flex-shrink: 0;
}

.status-indicator {
	display: flex;
	align-items: center;
	gap: 8px;
	color: var(--text-secondary);
	font-size: 14px;
}

.status-dot {
	width: 8px;
	height: 8px;
	border-radius: 50%;
	background-color: var(--text-tertiary);
	transition: background-color 0.3s;
}

.status-dot.active {
	background-color: var(--accent-primary);
	animation: pulse 2s infinite;
}

@keyframes pulse {
	0% {
		opacity: 1;
	}
	50% {
		opacity: 0.5;
	}
	100% {
		opacity: 1;
	}
}

.dashboard {
	display: flex;
	flex-direction: column;
	gap: 24px;
	margin-bottom: 32px;
}

.card {
	background-color: var(--bg-secondary);
	border: 1px solid var(--border-primary);
	border-radius: 6px;
	overflow: hidden;
}

.card-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 16px;
	border-bottom: 1px solid var(--border-primary);
}

.card-title {
	font-size: 16px;
	font-weight: 600;
	color: var(--text-primary);
}

.card-actions {
	display: flex;
	gap: 12px;
	align-items: center;
}

.timestamp {
	font-size: 12px;
	color: var(--text-tertiary);
}

.card-content {
	padding: 16px;
}

.chart-section {
	min-height: 300px;
}

.stats-section {
	display: flex;
	flex-direction: column;
	gap: 24px;
}

.metric-card {
	padding: 16px;
	border: 1px solid var(--border-primary);
	border-radius: 6px;
	background-color: var(--bg-tertiary);
}

.metric-label {
	font-size: 12px;
	color: var(--text-secondary);
	margin-bottom: 8px;
	text-transform: uppercase;
	letter-spacing: 0.5px;
}

.metric-value {
	font-size: 20px;
	font-weight: 600;
	color: var(--text-primary);
	margin-bottom: 4px;
	min-height: 28px;
}

.metric-value.loading {
	background: linear-gradient(90deg, var(--bg-secondary) 25%, var(--border-primary) 50%, var(--bg-secondary) 75%);
	background-size: 200% 100%;
	animation: loading 1.5s infinite;
	border-radius: 4px;
}

@keyframes loading {
	0% {
		background-position: 200% 0;
	}
	100% {
		background-position: -200% 0;
	}
}

.metric-description {
	font-size: 12px;
	color: var(--text-tertiary);
}

.status-info {
	display: flex;
	flex-direction: column;
	gap: 12px;
}

.status-item {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 8px 0;
}

.status-label {
	font-size: 14px;
	color: var(--text-secondary);
}

.status-value {
	font-size: 14px;
	font-weight: 500;
	padding: 4px 8px;
	border-radius: 12px;
	background-color: var(--bg-tertiary);
}

.status-value.normal {
	color: var(--accent-primary);
}

.status-value.warning {
	color: var(--accent-warning);
	background-color: rgba(210, 153, 34, 0.1);
}

.status-value.error {
	color: var(--accent-danger);
	background-color: rgba(248, 81, 73, 0.1);
}

.footer {
	padding: 24px 0;
	border-top: 1px solid var(--border-primary);
	text-align: center;
	color: var(--text-tertiary);
	font-size: 14px;
}
</style>