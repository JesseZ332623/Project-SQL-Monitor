import { SQLMonitorRSocket } from './SQLMonitorRSocket.js';

/** SQL 监控客户端，用于订阅和接收 SQL 服务器的各项性能指标（基于 RSocket）。*/
class SQLMonitorClient {
    constructor(baseURL) {
        this.rsocketClient = new SQLMonitorRSocket(baseURL);
        this.isConnected = false;
    }

    /** 初始化客户端，建立连接。*/
    async initialize() {
        try {
            await this.rsocketClient.connect();
            this.isConnected = true;
            console.log('SQLMonitorRSocketClient initialized and connected.');
            return true;
        } catch (error) {
            console.error('Failed to initialize SQLMonitorRSocketClient:', error);
            this.isConnected = false;
            return false;
        }
    }

    // 订阅 QPS 指标
    subscribeQPS(interval = '3', onData, onError, onComplete) {
        const route = `qps/${interval}`;

        return this.rsocketClient.sendRoute(
            route,
            (data) => {
                onData(data);
            },
            (error) => {
                onError?.(error);
            },
            () => {
                onComplete?.();
            }
        );
    }

    // 订阅网络流量指标
    subscribeNetworkTraffic(unit = 'KB', interval = '3', onData, onError, onComplete) {
        const route = `network-traffic/${unit}/${interval}`;

        return this.rsocketClient.sendRoute(
            route,
            (data) => {
                onData(data);
            },
            (error) => {
                onError?.(error);
            },
            () => {
                onComplete?.();
            }
        );
    }

    // 订阅连接使用率指标
    subscribeConnectionUsage(interval = '3', onData, onError, onComplete) {
        const route = `connection-usage/${interval}`;

        return this.rsocketClient.sendRoute(
            route,
            (data) => {
                onData(data);
            },
            (error) => {
                onError?.(error);
            },
            () => {
                onComplete?.();
            }
        );
    }

    // 订阅 InnoDB 缓存命中率指标
    subscribeCacheHitRate(interval = '3', onData, onError, onComplete) {
        const route = `cache-hit-rate/${interval}`;

        return this.rsocketClient.sendRoute(
            route,
            (data) => {
                onData(data);
            },
            (error) => {
                onError?.(error);
            },
            () => {
                onComplete?.();
            }
        );
    }

    // 取消订阅
    unsubscribe(subscriptionId) {
        this.rsocketClient.unsubscribe(subscriptionId);
    }

    // 断开连接
    disconnect() {
        this.rsocketClient.disconnect();
        this.isConnected = false;
    }

    // 获取连接状态
    getConnectionStatus() {
        return this.rsocketClient.getConnectionStatus();
    }
}

export { SQLMonitorClient };