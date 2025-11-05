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

			<div class="alert warning-alert" v-if="connectionStatus === 'connecting'">
				<svg class="alert-icon" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
					<path fill-rule="evenodd"
						d="M8.22 1.754a.25.25 0 00-.44 0L1.698 13.132a.25.25 0 00.22.368h12.164a.25.25 0 00.22-.368L8.22 1.754zm-1.763-.707c.659-1.234 2.427-1.234 3.086 0l6.082 11.378A1.75 1.75 0 0114.082 15H1.918a1.75 1.75 0 01-1.543-2.575L6.457 1.047zM9 11a1 1 0 11-2 0 1 1 0 012 0zm-.25-5.25a.75.75 0 00-1.5 0v2.5a.75.75 0 001.5 0v-2.5z">
					</path>
				</svg>
				<span>Connecting to RSocket server...</span>
			</div>

			<div class="controls">
				<div class="control-group">
					<button class="btn btn-primary" @click="reconnect" :disabled="connectionStatus === 'connecting'">
						<svg class="btn-icon" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
							<path fill-rule="evenodd"
								d="M8 2.5a5.487 5.487 0 00-4.131 1.869l1.204 1.204A.25.25 0 014.896 6H1.25A.25.25 0 011 5.75V2.104a.25.25 0 01.427-.177l1.38 1.38A7.001 7.001 0 0114.95 7.16a.75.75 0 11-1.49.178A5.501 5.501 0 008 2.5zM1.705 8.005a.75.75 0 01.834.656 5.501 5.501 0 009.592 2.97l-1.204-1.204a.25.25 0 01.177-.427h3.646a.25.25 0 01.25.25v3.646a.25.25 0 01-.427.177l-1.38-1.38A7.001 7.001 0 011.05 8.84a.75.75 0 01.656-.834z">
							</path>
						</svg>
						{{ connectionStatus === 'connected' ? 'Reconnect' : 'Connect' }}
					</button>
					<button class="btn" :class="connectionStatus === 'connected' ? 'btn-danger' : 'btn-secondary'"
						@click="toggleConnection">
						<svg class="btn-icon" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
							<path fill-rule="evenodd"
								d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z">
							</path>
						</svg>
						{{ connectionStatus === 'connected' ? 'Disconnect' : 'Connect' }}
					</button>
				</div>
				<div class="status-indicator">
					<div class="status-dot" :class="connectionStatus"></div>
					<span class="connection-status">{{ connectionStatusText }}</span>
					<span v-if="connectionStatus === 'connected'" class="subscription-count">
						({{ Object.values(subscriptionIds).filter(Boolean).length }} active subscriptions)
					</span>
				</div>
			</div>

			<div class="dashboard" v-if="connectionStatus === 'connected'">
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
								<NetworkTrafficChart :chart-data="networkChartData" :loading="loading"
									:current-unit="currentNetworkUnit" />
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
										{{ 'Usage: ' + (connectionsData ? connectionsData.connectUsagePercent.toFixed(2)
											+ '%' : '--') }}
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
									<div class="metric-value"
										:class="{ loading: loading && !innodbBufferCacheHitRate }">
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
									<span class="status-value" :class="qpsData && qpsData.error ? 'error' : 'normal'">
										{{ qpsData ? (qpsData.error ? 'Yes' : 'No') : '--' }}
									</span>
								</div>
								<div class="status-item">
									<span class="status-label">Auto Unit:</span>
									<span class="status-value" :class="autoUnitEnabled ? 'normal' : 'warning'">
										{{ autoUnitEnabled ? 'Enabled' : 'Disabled' }}
									</span>
								</div>
								<div class="status-item">
									<span class="status-label">Connection Status:</span>
									<span class="status-value" :class="connectionStatus === 'connected' ? 'normal' : 'error'">
										{{ connectionStatusText }}
									</span>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>

			<div class="no-connection" v-else-if="connectionStatus === 'disconnected'">
				<div class="no-connection-content">
					<svg class="no-connection-icon" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor">
						<path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
						<line x1="12" y1="9" x2="12" y2="13"></line>
						<line x1="12" y1="17" x2="12.01" y2="17"></line>
					</svg>
					<h3>Not Connected</h3>
					<p>Click the Connect button to start receiving real-time metrics.</p>
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
import { SQLMonitorClient } from './services/SQLMonitorClient'

export default {
	name: 'App',
	components: {
		QPSChart,
		NetworkTrafficChart
	},
	setup() {
		const serverRunningTime = ref(null)
		const baseAddress = ref('ws://localhost:19198/sql-indicator-stream') // 使用 RSocket 地址
		const qpsData = ref(null)
		const connectionsData = ref(null)
		const netTrafficData = ref(null)
		const loading = ref(false)
		const error = ref(null)
		const lastUpdate = ref(null)
		const refreshInterval = ref(3)
		const selectedUnit = ref('KB')
		const autoUnitEnabled = ref(false)

		// InnoDB Buffer Cache Hit Rate
		const innodbBufferCacheHitRate = ref(null)

		// RSocket 客户端实例
		const rsocketClient = ref(null)

		// 连接状态
		const connectionStatus = ref('disconnected') // 'disconnected', 'connecting', 'connected', 'error'

		// 订阅 ID 存储
		const subscriptionIds = reactive({
			qps: null,
			network: null,
			connection: null,
			cache: null
		})

		// 计算属性
		const connectionStatusText = computed(() => {
			const statusMap = {
				disconnected: 'Disconnected',
				connecting: 'Connecting...',
				connected: 'Connected',
				error: 'Connection Error'
			}
			return statusMap[connectionStatus.value] || 'Unknown'
		})

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
			// 重新订阅网络流量指标
			if (subscriptionIds.network) {
				rsocketClient.value.unsubscribe(subscriptionIds.network)
				subscribeNetworkTraffic()
			}
		}

		// 初始化 RSocket 连接
		const initializeRSocket = async () => {
			try {
				connectionStatus.value = 'connecting'
				loading.value = true
				error.value = null

				rsocketClient.value = new SQLMonitorClient(baseAddress.value)
				const success = await rsocketClient.value.initialize()

				if (success) {
					console.log('RSocket 连接成功')
					connectionStatus.value = 'connected'
					startSubscriptions()
					loading.value = false
					return true
				} else {
					throw new Error('RSocket 连接失败')
				}
			} catch (err) {
				console.error('RSocket 初始化失败:', err)
				error.value = err.message
				connectionStatus.value = 'error'
				loading.value = false
				return false
			}
		}

		// 开始所有订阅
		const startSubscriptions = () => {
			subscribeQPS()
			subscribeNetworkTraffic()
			subscribeConnectionUsage()
			subscribeCacheHitRate()
		}

		// 停止所有订阅
		const stopSubscriptions = () => {
			Object.values(subscriptionIds).forEach(id => {
				if (id) {
					rsocketClient.value.unsubscribe(id)
				}
			})

			// 重置订阅 ID
			Object.keys(subscriptionIds).forEach(key => {
				subscriptionIds[key] = null
			})
		}

		// 订阅 QPS 指标
		const subscribeQPS = () => {
			subscriptionIds.qps = rsocketClient.value.subscribeQPS(
				refreshInterval.value.toString(),
				(data) => {
					qpsData.value = data
					updateQPSChart(data)
					updateLastUpdate()
				},
				(err) => {
					console.error('QPS 订阅错误:', err)
					error.value = `QPS 数据错误: ${err.message}`
				},
				() => {
					console.log('QPS 流结束')
					subscriptionIds.qps = null
				}
			)
		}

		// 订阅网络流量指标
		const subscribeNetworkTraffic = () => {
			const unitToUse = autoUnitEnabled.value ? currentNetworkUnit.value : selectedUnit.value

			subscriptionIds.network = rsocketClient.value.subscribeNetworkTraffic(
				unitToUse,
				refreshInterval.value.toString(),
				(data) => {
					netTrafficData.value = data
					updateNetworkChart(data)
					updateLastUpdate()
				},
				(err) => {
					console.error('网络流量订阅错误:', err)
					error.value = `网络流量数据错误: ${err.message}`
				},
				() => {
					console.log('网络流量流结束')
					subscriptionIds.network = null
				}
			)
		}

		// 订阅连接使用率指标
		const subscribeConnectionUsage = () => {
			subscriptionIds.connection = rsocketClient.value.subscribeConnectionUsage(
				refreshInterval.value.toString(),
				(data) => {
					connectionsData.value = data
					updateLastUpdate()
				},
				(err) => {
					console.error('连接使用率订阅错误:', err)
					error.value = `连接使用率数据错误: ${err.message}`
				},
				() => {
					console.log('连接使用率流结束')
					subscriptionIds.connection = null
				}
			)
		}

		// 订阅缓存命中率指标
		const subscribeCacheHitRate = () => {
			subscriptionIds.cache = rsocketClient.value.subscribeCacheHitRate(
				refreshInterval.value.toString(),
				(data) => {
					innodbBufferCacheHitRate.value = data
					updateLastUpdate()
				},
				(err) => {
					console.error('缓存命中率订阅错误:', err)
					error.value = `缓存命中率数据错误: ${err.message}`
				},
				() => {
					console.log('缓存命中率流结束')
					subscriptionIds.cache = null
				}
			)
		}

		// 更新 QPS 图表
		const updateQPSChart = (data) => {
			const now = new Date()
			const timeLabel = now.toLocaleTimeString()
			updateChartData(qpsChartData, [data.qps], timeLabel)
		}

		// 更新网络流量图表
		const updateNetworkChart = (data) => {
			const now = new Date()
			const timeLabel = now.toLocaleTimeString()
			updateChartData(
				networkChartData,
				[data.receivePerSec, data.sentPerSec],
				timeLabel
			)
		}

		// 更新最后更新时间
		const updateLastUpdate = () => {
			lastUpdate.value = new Date().toLocaleString()
		}

		// 连接/断开连接
		const toggleConnection = () => {
			if (connectionStatus.value === 'connected') {
				disconnect()
			} else {
				connect()
			}
		}

		// 连接
		const connect = () => {
			initializeRSocket()
		}

		// 断开连接
		const disconnect = () => {
			if (rsocketClient.value) {
				stopSubscriptions()
				rsocketClient.value.disconnect()
			}
			connectionStatus.value = 'disconnected'
		}

		// 重新连接
		const reconnect = () => {
			disconnect()
			setTimeout(() => {
				connect()
			}, 1000)
		}

		// 监听刷新间隔变化
		watch(refreshInterval, (newInterval) => {
			if (rsocketClient.value && connectionStatus.value === 'connected') {
				// 间隔变化时重新订阅所有指标
				console.log('刷新间隔变化，重新订阅所有指标:', newInterval)
				stopSubscriptions()
				startSubscriptions()
			}
		})

		// 监听单位变化
		watch(currentNetworkUnit, (newUnit) => {
			if (autoUnitEnabled.value && rsocketClient.value && connectionStatus.value === 'connected') {
				console.log('自动单位变化，重新订阅网络流量:', newUnit)
				if (subscriptionIds.network) {
					rsocketClient.value.unsubscribe(subscriptionIds.network)
					subscribeNetworkTraffic()
				}
			}
		})

		// 监听自动单位切换
		watch(autoUnitEnabled, (newValue) => {
			if (newValue && rsocketClient.value && connectionStatus.value === 'connected') {
				// 启用自动单位时重新订阅网络流量
				if (subscriptionIds.network) {
					rsocketClient.value.unsubscribe(subscriptionIds.network)
					subscribeNetworkTraffic()
				}
			}
		})

		onMounted(() => {
			// 初始连接
			connect()
		})

		onUnmounted(() => {
			// 清理资源
			disconnect()
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
			lastUpdate,
			refreshInterval,
			selectedUnit,
			currentNetworkUnit,
			autoUnitEnabled,
			connectionStatus,
			connectionStatusText,
			subscriptionIds,
			toggleConnection,
			reconnect,
			onUnitChange,
			formatNumber,
			formatBytes,
			formatNetworkRate
		}
	}
}
</script>

<style>
/* 新增样式 */
.warning-alert {
	border-color: rgba(210, 153, 34, 0.4);
	background-color: rgba(210, 153, 34, 0.1);
	color: var(--accent-warning);
}

.connection-status {
	font-weight: 500;
}

.subscription-count {
	font-size: 12px;
	color: var(--text-tertiary);
}

.status-dot.connected {
	background-color: var(--accent-primary);
	animation: pulse 2s infinite;
}

.status-dot.connecting {
	background-color: var(--accent-warning);
	animation: pulse 1s infinite;
}

.status-dot.disconnected,
.status-dot.error {
	background-color: var(--accent-danger);
}

.no-connection {
	display: flex;
	justify-content: center;
	align-items: center;
	min-height: 400px;
	padding: 40px;
}

.no-connection-content {
	text-align: center;
	color: var(--text-secondary);
}

.no-connection-icon {
	color: var(--text-tertiary);
	margin-bottom: 16px;
}

.no-connection-content h3 {
	margin-bottom: 8px;
	color: var(--text-primary);
}

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