import { RSocketClient } from 'rsocket-core';
import { WebsocketClientTransport } from 'rsocket-websocket-client';
import { encodeAndSendWellKnownTypes, decodeWellKnownMimeType } from 'rsocket-core';

/** RSocket 服务器。*/
class SQLMonitorRSocket {
    constructor(baseURL) {
        this.baseURL = baseURL;             // 后端 RSocket 服务器地址
        this.client = null;                 // RSocket 客户端实例
        this.subscriptions = new Map();     // 订阅映射
        this.reconnectAttempts = 0;         // 重连尝试次数
        this.maxReconnectAttempts = 5;      // 最大重连尝试次数
        this.reconnectInterval = 3000;      // 重连间隔时间（毫秒）
        this.isConnected = false;           // 连接状态
    }

    /** 与后端服务器建立 RSocket 连接。*/
    connect() {
        return new Promise((resolve, reject) => {
            try {
                // 创建 RSocket 传输层
                const transport = new WebsocketClientTransport({
                    url: this.baseURL
                });

                // 创建 RSocket 客户端
                this.client = new RSocketClient({
                    setup: {
                        keepAlive: 60000,
                        lifetime: 180000,
                        dataMimeType: 'application/json',
                        metadataMimeType: 'message/x.rsocket.routing.v0',
                    },
                    transport,
                });

                // 连接服务器
                this.client.connect().subscribe({
                    onComplete: (socket) => {
                        console.log(`Established RSocket connection with server: ${this.baseURL}`);
                        this.socket = socket;
                        this.isConnected = true;
                        this.reconnectAttempts = 0;
                        resolve(socket);
                    },
                    onError: (error) => {
                        console.error('RSocket connection error:', error);
                        this.isConnected = false;
                        reject(error);
                    },
                    onSubscribe: (cancel) => {
                        // 保存取消函数以便后续使用
                        this.connectionCancel = cancel;
                    }
                });

            } catch (error) {
                console.error(`Failed to establish RSocket connection with server: ${this.baseURL}`, error);
                reject(error);
            }
        });
    }

    /** 处理重连逻辑。*/
    handleReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`Reconnection attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts}...`);
            setTimeout(() => {
                this.connect();
            }, this.reconnectInterval);
        } else {
            console.error('Max reconnection attempts reached. Giving up.');
        }
    }

    /** 发送路由请求并建立流式订阅（request-stream 模式）*/
    sendRoute(route, onData, onError, onComplete) {
        if (!this.socket || !this.isConnected) {
            console.error('RSocket is not connected. Cannot send message.');
            return null;
        }

        const subscriptionId = this.generateSubscriptionId();

        // 创建路由元数据
        const metadata = this.encodeRoute(route);

        try {
            // 使用 request-stream 模式请求数据流
            const requestStream = this.socket.requestStream({
                data: null,
                metadata: metadata
            });

            const subscription = requestStream.subscribe({
                onNext: (payload) => {
                    try {
                        const data = payload.data ? JSON.parse(payload.data) : null;
                        onData(data);
                    } catch (error) {
                        console.error('Error parsing payload data:', error);
                        onError?.(error);
                    }
                },
                onError: (error) => {
                    console.error('Stream error:', error);
                    onError?.(error);
                    this.subscriptions.delete(subscriptionId);
                },
                onComplete: () => {
                    console.log(`Stream completed for subscription: ${subscriptionId}`);
                    onComplete?.();
                    this.subscriptions.delete(subscriptionId);
                },
                onSubscribe: (subscription) => {
                    // 保存订阅以便后续取消
                    this.subscriptions.set(subscriptionId, subscription);
                    // 请求数据
                    subscription.request(2147483647); // 请求最大数量的数据
                }
            });

            return subscriptionId;
        } catch (error) {
            console.error('Error creating request stream:', error);
            onError?.(error);
            return null;
        }
    }

    /** 编码路由信息到元数据 */
    encodeRoute(route) {
        // RSocket 路由元数据格式: 路由长度(1字节) + 路由内容
        const routeBuffer = Buffer.from(route, 'utf-8');
        const metadataBuffer = Buffer.alloc(1 + routeBuffer.length);
        metadataBuffer.writeUInt8(routeBuffer.length, 0);
        routeBuffer.copy(metadataBuffer, 1);
        return metadataBuffer;
    }

    /** 生成唯一的订阅 ID */
    generateSubscriptionId() {
        return `sub_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
    }

    /** 取消订阅 */
    unsubscribe(subscriptionId) {
        if (this.subscriptions.has(subscriptionId)) {
            const subscription = this.subscriptions.get(subscriptionId);
            subscription.cancel();
            this.subscriptions.delete(subscriptionId);
            console.log(`Unsubscribed from subscription ID: ${subscriptionId}`);
        }
    }

    /** 断开连接 */
    disconnect() {
        if (this.connectionCancel) {
            this.connectionCancel();
        }

        if (this.socket) {
            this.socket.close();
            this.socket = null;
        }

        // 取消所有活跃的订阅
        this.subscriptions.forEach((subscription, subscriptionId) => {
            subscription.cancel();
        });
        this.subscriptions.clear();

        this.isConnected = false;
        console.log('RSocket connection closed manually.');
    }

    /** 获取连接状态 */
    getConnectionStatus() {
        return this.isConnected;
    }
}

export { SQLMonitorRSocket };