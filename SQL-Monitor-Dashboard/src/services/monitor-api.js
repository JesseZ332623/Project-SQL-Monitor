// services/monitor-api.js
const API_BASE_URL = '/api/sql-monitor'
const DEFAULT_TIMEOUT = 10000; // 10秒超时

// 创建带超时功能的fetch包装器
const fetchWithTimeout = async (url, options = {}) => {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), DEFAULT_TIMEOUT);
    
    try 
    {
        const response = await fetch(url, {
            ...options,
            signal: controller.signal
        });
        clearTimeout(timeoutId);

        return response;
    }
    catch (error) 
    {
        clearTimeout(timeoutId);
        if (error.name === 'AbortError') {
            throw new Error('Request timeout. Please check your network connection and server status.');
        }
        throw error;
    }
};

const fetchServerTime = async () => {
    try 
    {
        console.log(`${API_BASE_URL}/running-time`);
        const response = await fetchWithTimeout(`${API_BASE_URL}/running-time`)

        if (!response.ok) {
            let errorMessage = `HTTP error! status: ${response.status}`;
            try {
                const errorResponse = await response.json();
                if (errorResponse && errorResponse.message) {
                    errorMessage = errorResponse.message;
                }
            } catch (e) {
                // If we can't parse the JSON response, create a more helpful error message
                errorMessage = `Request failed with status ${response.status}. ${response.statusText || 'Unknown error'}`;
            }
            throw new Error(errorMessage);
        }

        const data = await response.json();
        
        // Validate the response data structure
        if (!data || typeof data !== 'object') {
            throw new Error('Invalid response format received from server');
        }
        
        return data;
    }
    catch (error)
    {
        console.error('Error fetching server time:', error);
        throw error.message.includes('timeout') 
            ? error 
            : new Error(`Failed to fetch server time: ${error.message}`);
    }
}

const fetchBaseAddress = async () => {
    try 
    {
        const response = await fetchWithTimeout(`${API_BASE_URL}/base-address`)
        if (!response.ok) {
            let errorMessage = `HTTP error! status: ${response.status}`;
            try {
                const errorResponse = await response.json();
                if (errorResponse && errorResponse.message) {
                    errorMessage = errorResponse.message;
                }
            } catch (e) {
                // 如果无法解析JSON响应，则使用默认错误消息
                console.warn('Could not parse error response as JSON:', e);
            }
            throw new Error(errorMessage);
        }
        const data = await response.json()

        // 验证数据结构
        if (!data || !data.data || typeof data.data !== 'string') {
            throw new Error('Invalid data format received from server')
        }  

        return data;
    }
    catch (error)
    {
        console.error('Error fetching base address:', error)
        throw error.message.includes('timeout') 
            ? error 
            : new Error('Failed to fetch base address. Please check if the server is running.')
    }
}

const fetchQPSData = async () => {
    try 
    {
        const response = await fetchWithTimeout(`${API_BASE_URL}/qps`)

        if (!response.ok) {
            let errorMessage = `HTTP error! status: ${response.status}`;
            try {
                const errorResponse = await response.json();
                if (errorResponse && errorResponse.message) {
                    errorMessage = errorResponse.message;
                }
            } catch (e) {
                // 如果无法解析JSON响应，则使用默认错误消息
                console.warn('Could not parse error response as JSON:', e);
            }
            throw new Error(errorMessage);
        }

        const data = await response.json()

        // 验证数据结构
        if (!data || !data.data || typeof data.data.qps !== 'number') {
            throw new Error('Invalid data format received from server')
        }

        return data
    } 
    catch (error) 
    {
        console.error('Error fetching QPS data:', error)
        throw error.message.includes('timeout') 
            ? error 
            : new Error('Failed to fetch QPS data. Please check if the server is running.')
    }
}

const fetchConnectionsUsage = async () => {
    try 
    {
        const response 
            = await fetchWithTimeout(`${API_BASE_URL}/connection-usage`)

        if (!response.ok) {
            let errorMessage = `HTTP error! status: ${response.status}`;
            try {
                const errorResponse = await response.json();
                if (errorResponse && errorResponse.message) {
                    errorMessage = errorResponse.message;
                }
            } catch (e) {
                // 如果无法解析JSON响应，则使用默认错误消息
                console.warn('Could not parse error response as JSON:', e);
            }
            throw new Error(errorMessage);
        }

        const data = await response.json()

        // 验证数据结构
        if (!data || !data.data || 
            typeof data.data.maxConnections !== 'number' || 
            typeof data.data.currentConnections !== 'number' ||
            typeof data.data.connectUsagePercent !== 'number'
        ) 
        {
            throw new Error('Invalid data format received from server')
        }

        return data
    }
    catch (error) 
    {
        console.error('Error fetching connection usage data:', error)
        throw error.message.includes('timeout') 
            ? error 
            : new Error('Failed to fetch connection usage data. Please check if the server is running.')
    }
}

const fetchNetworkTraffic = async (sizeUnit = 'KB') => {
    try 
    {
        const response = await fetchWithTimeout(`${API_BASE_URL}/network-traffic?sizeUnit=${sizeUnit}`)

        if (!response.ok) {
            let errorMessage = `HTTP error! status: ${response.status}`;
            try {
                const errorResponse = await response.json();
                if (errorResponse && errorResponse.message) {
                    errorMessage = errorResponse.message;
                }
            } catch (e) {
                // 如果无法解析JSON响应，则使用默认错误消息
                console.warn('Could not parse error response as JSON:', e);
            }
            throw new Error(errorMessage);
        }

        const data = await response.json()

        // 验证数据结构
        if (!data || !data.data || 
            typeof data.data.totalBytesSent !== 'number' ||
            typeof data.data.totalBytesReceive !== 'number' ||
            typeof data.data.receivePerSec !== 'number' ||
            typeof data.data.sentPerSec !== 'number'
        ) {
            throw new Error('Invalid data format received from server')
        }

        return data
    }
    catch (error)
    {
        console.error('Error fetching network traffic data:', error)
        throw error.message.includes('timeout') 
            ? error 
            : new Error('Failed to fetch network traffic data. Please check if the server is running.')
    }
}

const fetchInnodbBufferCacheHitRate = async () => {
    try 
    {
        const response 
            = await fetchWithTimeout(`${API_BASE_URL}/cache-hit-rate`)
        
        if (!response.ok) {
            let errorMessage = `HTTP error! status: ${response.status}`;
            try {
                const errorResponse = await response.json();
                if (errorResponse && errorResponse.message) {
                    errorMessage = errorResponse.message;
                }
            } catch (e) {
                // 如果无法解析JSON响应，则使用默认错误消息
                console.warn('Could not parse error response as JSON:', e);
            }
            throw new Error(errorMessage);
        }

        const data = await response.json()

        if (!data || !data.data || 
            typeof data.data.cacheHitRate !== 'number' ||
            typeof data.data.queryDiff !== 'number'
        ) { 
            throw new Error('Invalid data format received from server')
        }

        return data;
    }
    catch (error)
    {
        console.error('Error fetching InnoDB buffer cache hit rate data:', error)
        throw error.message.includes('timeout') 
            ? error 
            : new Error('Failed to fetch InnoDB buffer cache hit rate data. Please check if the server is running.')
    }
}

export {
    fetchServerTime,
    fetchBaseAddress,
    fetchQPSData,
    fetchConnectionsUsage,
    fetchNetworkTraffic,
    fetchInnodbBufferCacheHitRate
}