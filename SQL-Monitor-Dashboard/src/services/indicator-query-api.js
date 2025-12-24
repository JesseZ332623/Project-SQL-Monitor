// services/indicator-query-api.js

const API_BASE_URL = '/api/indicator'

/**
 * 指标日志的条件查询 API 接口
 * 
 * @param {string} type  要查询的指标类型，由多选框给出
 * @param {string} ip    数据库服务器 IPv4 地址
 * @param {string} from  统计的起始范围（开始时间）
 * @param {string} to    统计的结束范围（结束时间）
 * @param {string} order 结果集的顺序（按时间排序），由多选框给出
 * @param {number} page  第几页？
 * 
 * @returns 服务器返回的响应 JSON
*/
const fetchIndicatorLog = async (type, ip, from, to, order, page) => {
    try 
    {
        // Validate inputs before making the request
        if (!ip || typeof ip !== 'string' || !ip.trim()) {
            throw new Error('Server IP is required and must be a valid string');
        }
        
        // Additional validation for IP format (basic check)
        const ipRegex = /^(\d{1,3}\.){3}\d{1,3}$/;
        if (!ipRegex.test(ip.trim())) {
            throw new Error(`Invalid IP address format: ${ip}. Expected format: xxx.xxx.xxx.xxx`);
        }

        const pageSize   = 15 // 一页的数据量，是一个定值
        const requestURL = new URL(`${API_BASE_URL}/log`, window.location.href)

        requestURL.searchParams.append('indicator-type', type)
        requestURL.searchParams.append('server-ip', ip)
        requestURL.searchParams.append('from', from)
        requestURL.searchParams.append('to', to)
        requestURL.searchParams.append('order', order)
        requestURL.searchParams.append('page-no', page)
        requestURL.searchParams.append('page-size', pageSize)

        const response = await fetch(requestURL.toString(), {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        })

        if (!response.ok)
        {
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

        return await response.json()
    }
    catch (error)
    {
        // Provide more context in the error message
        const errorWithContext = new Error(`Failed to fetch indicator log: ${error.message}`);
        errorWithContext.originalError = error;
        console.error('Error to fetching indicator log:', errorWithContext)  
        throw errorWithContext
    }
}

/**
 * QPS 统计数据查询。
 * 
 * @param {string} type  要统计的 QPS 指标类型，由多选框给出
 * @param {string} ip    数据库服务器 IPv4 地址
 * @param {string} from  统计的起始范围（开始时间）
 * @param {string} to    统计的结束范围（结束时间）
 * 
 * @returns 服务器返回的响应 JSON
*/
const fetchQPSStatistics = async (type, ip, from, to) => {
    try 
    {
        // Validate inputs before making the request
        if (!type || typeof type !== 'string' || !type.trim()) {
            throw new Error('Statistics type is required and must be a valid string');
        }
        
        if (!ip || typeof ip !== 'string' || !ip.trim()) {
            throw new Error('Server IP is required and must be a valid string');
        }
        
        // Additional validation for IP format (basic check)
        const ipRegex = /^(\d{1,3}\.){3}\d{1,3}$/;
        if (!ipRegex.test(ip.trim())) {
            throw new Error(`Invalid IP address format: ${ip}. Expected format: xxx.xxx.xxx.xxx`);
        }

        const requestURL = new URL(`${API_BASE_URL}/qps-statistics`, window.location.href)

        requestURL.searchParams.append('type', type)
        requestURL.searchParams.append('server-ip', ip)
        requestURL.searchParams.append('from', from)
        requestURL.searchParams.append('to', to)

        const response = await fetch(requestURL.toString(), {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        })

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

        return await response.json()
    }
    catch (error) 
    {
        // Provide more context in the error message
        const errorWithContext = new Error(`Failed to fetch QPS statistics: ${error.message}`);
        errorWithContext.originalError = error;
        console.error('Error to fetching qps statics:', errorWithContext)  
        throw errorWithContext
    }
}

export {
    fetchIndicatorLog,
    fetchQPSStatistics
}