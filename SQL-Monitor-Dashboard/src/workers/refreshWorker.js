// refreshWorker.js - 用于处理定时刷新的 Web Worker

let intervalId = null;
let currentInterval = 3000; // 默认3秒

// 监听来自主线程的消息
self.onmessage = function (e) {
	const { type, interval } = e.data;

	switch (type)
	{
		case 'START':
			// 如果已有定时器，先清除
			if (intervalId) {
				clearInterval(intervalId);
			}

			// 设置新的定时器
			currentInterval = interval || currentInterval;
			intervalId = setInterval(() => {
				// 发送刷新信号到主线程
				self.postMessage({
					type: 'REFRESH',
					timestamp: Date.now()
				});
			}, currentInterval);

			console.log('Worker: Auto-refresh started with interval:', currentInterval, 'ms');
			break;

		case 'STOP':
			if (intervalId) 
			{
				clearInterval(intervalId);
				intervalId = null;
				console.log('Worker: Auto-refresh stopped');
			}
			break;

		case 'UPDATE_INTERVAL':
			currentInterval = interval;
			// 重启定时器以应用新间隔
			if (intervalId) {
				clearInterval(intervalId);
				intervalId = setInterval(() => {
					self.postMessage({
						type: 'REFRESH',
						timestamp: Date.now()
					});
				}, currentInterval);
			}
			console.log('Worker: Interval updated to:', currentInterval, 'ms');
			break;
	}
};

// 当 Worker 被终止时清理定时器
self.addEventListener('error', () => {
	if (intervalId) {
		clearInterval(intervalId);
	}
});

// 当 Worker 被终止时清理定时器
self.addEventListener('unhandledrejection', () => {
	if (intervalId) {
		clearInterval(intervalId);
	}
});