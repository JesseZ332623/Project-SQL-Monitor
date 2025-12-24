// dataProcessor.js - 数据处理和聚合工具函数

// 统一的数据获取函数
export async function fetchAllMetrics(unitToUse, api) {
	const results = await Promise.allSettled([
		api.fetchQPSData(),
		api.fetchConnectionsUsage(),
		api.fetchNetworkTraffic(unitToUse),
		api.fetchServerTime(),
		api.fetchInnodbBufferCacheHitRate()
	]);

	const [qpsResult, connResult, netTrafficResult, serverRunningTimeResult, innodbResult] = results;

	return {
		qpsData: qpsResult.status === 'fulfilled' ? qpsResult.value.data : null,
		connectionsData: connResult.status === 'fulfilled' ? connResult.value.data : null,
		netTrafficData: netTrafficResult.status === 'fulfilled' ? netTrafficResult.value.data : null,
		runtimeArray: serverRunningTimeResult.status === 'fulfilled' ? serverRunningTimeResult.value.data : [],
		innodbBufferCacheHitRate: innodbResult.status === 'fulfilled' ? innodbResult.value.data : null
	};
}

// 统一的图表数据更新函数
export function updateChartData(chartState, values, timestamp, maxDataPoints = 30) {
	const newLabels = [...chartState.labels, timestamp];
	const newDatasets = chartState.datasets.map((dataset, index) => ({
		...dataset,
		data: [...dataset.data, values[index] || 0]
	}));

	// 限制数据点数量
	if (newLabels.length > maxDataPoints) {
		newLabels.shift();
		newDatasets.forEach(dataset => dataset.data.shift());
	}

	chartState.labels = newLabels;
	chartState.datasets = newDatasets;
}

// 格式化工具函数
export const formatUtils = {
	number: (num) => num ? new Intl.NumberFormat().format(num) : '--',
	bytes: (bytes) => {
		if (bytes === 0) return '0 B';
		if (!bytes) return '0 B';
		const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
		const i = Math.floor(Math.log(Math.abs(bytes)) / Math.log(1024));
		return parseFloat((bytes / Math.pow(1024, i)).toFixed(2)) + ' ' + sizes[i];
	},
	networkRate: (rate, unit) => {
		return rate !== undefined && rate !== null ? `${rate.toFixed(2)} ${unit}/s` : '--';
	},
	runtime: (runtimeArray) => {
		if (!runtimeArray || runtimeArray.length < 4) return '--';
		return `${runtimeArray[0]} days ${runtimeArray[1]} hours ${runtimeArray[2]} min ${runtimeArray[3]} sec`;
	}
};

// 计算最佳网络单位
export function calculateBestNetworkUnit(receivePerSec, sentPerSec) {
	// 检查输入值是否为有效数字
	if (typeof receivePerSec !== 'number' || typeof sentPerSec !== 'number' || 
	    isNaN(receivePerSec) || isNaN(sentPerSec)) {
		return 'KB'; // 默认单位
	}
	
	const maxRate = Math.max(Math.abs(receivePerSec), Math.abs(sentPerSec));
	if (maxRate >= 1024 * 1024) return 'GB'; // 如果速率大于等于1024*1024，则使用GB
	if (maxRate >= 1024) return 'MB';
	if (maxRate >= 1) return 'KB';
	return 'B';
};