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
				try {
					// 发送刷新信号到主线程
					self.postMessage({
						type: 'REFRESH',
						timestamp: Date.now()
					});
				} catch (error) {
					console.error('Error sending refresh message:', error);
				}
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
					try {
						self.postMessage({
							type: 'REFRESH',
							timestamp: Date.now()
						});
					} catch (error) {
						console.error('Error sending refresh message:', error);
					}
				}, currentInterval);
			}
			console.log('Worker: Interval updated to:', currentInterval, 'ms');
			break;
	}
};

// Worker 错误处理
self.addEventListener('error', (e) => {
    console.error('Worker error:', e.error);
    if (intervalId) {
        clearInterval(intervalId);
        intervalId = null;
    }
    // 发送错误信息到主线程
    self.postMessage({
        type: 'ERROR',
        message: e.error.message
    });
});

self.addEventListener('unhandledrejection', (e) => {
    console.error('Unhandled promise rejection in worker:', e.reason);
    if (intervalId) {
        clearInterval(intervalId);
        intervalId = null;
    }
    // 发送错误信息到主线程
    self.postMessage({
        type: 'ERROR',
        message: e.reason.message || 'Unhandled promise rejection'
    });
    e.preventDefault(); // 防止默认错误行为
});