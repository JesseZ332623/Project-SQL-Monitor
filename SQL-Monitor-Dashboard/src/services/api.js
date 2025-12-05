// services/api.js
const API_BASE_URL = '/api/sql-monitor'

const fetchServerTime = async () => {
    try 
    {
        console.log(`${API_BASE_URL}/running-time`);
        const response = await fetch(`${API_BASE_URL}/running-time`)

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
        }

        const data = await response.json()
        
        return data
    }
    catch (error)
    {
        console.error('Error fetching server time:', error)
        throw new Error('Failed to fetch server time. Please check if the server is running.')
    }
}

const fetchBaseAddress = async () => {
    try 
    {
        const response = await fetch(`${API_BASE_URL}/base-address`)
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
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
        throw new Error('Failed to fetch base address. Please check if the server is running.')
    }
}

const fetchQPSData = async () => {
    try 
    {
        const response = await fetch(`${API_BASE_URL}/qps`)

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
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
        throw new Error('Failed to fetch QPS data. Please check if the server is running.')
    }
}

const fetchConnectionsUsage = async () => {
    try 
    {
        const response 
            = await fetch(`${API_BASE_URL}/connection-usage`)

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
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
        throw new Error('Failed to fetch connection usage data. Please check if the server is running.')
    }
}

const fetchNetworkTraffic = async (sizeUnit = 'KB') => {
    try 
    {
        const response = await fetch(`${API_BASE_URL}/network-traffic?sizeUnit=${sizeUnit}`)

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
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
        throw new Error('Failed to fetch network traffic data. Please check if the server is running.')
    }
}

const fetchInnodbBufferCacheHitRate = async () => {
    try 
    {
        const response 
            = await fetch(`${API_BASE_URL}/cache-hit-rate`)
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
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
        console.error('Error fetching network traffic data:', error)
        throw new Error('Failed to fetch network traffic data. Please check if the server is running.')
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