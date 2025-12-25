// services/scheduled-tasks-api.js

const API_BASE_URL    = '/api/scheduled-task'
const SERVER_BASE_URL = window.location.href

/** 手动发送例行指标数据报告。*/
const executeSendIntervalIndicatorReport = async () => {
    try 
    {
        const response 
            = await fetch(
                new URL(`${API_BASE_URL}/indicator-report`, SERVER_BASE_URL), {
                    method: "POST",
                    'Content-Type': 'application/json'
                }
            )

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

        /*
            {
                "timestamp": 1766556867898,
                "status": "OK",
                "message": "Execute task sendIntervalIndicatorReportManually() complete!"
            }
        */
        return await response.json()
    }
    catch (error) 
    {
        // Provide more context in the error message
        const errorWithContext 
            = new Error(`Failed to send indicator report! log: ${error.message}`);
        errorWithContext.originalError = error;
        console.error('Failed to send indicator report! log:', errorWithContext)  
        throw errorWithContext
    }
}

/** 手动的执行历史指标清除操作。*/
const executeCleanIndicatorUntilLastWeek = async () => {
    try 
    {
        const response 
            = await fetch(
                new URL(`${API_BASE_URL}/historical-indicator`, SERVER_BASE_URL), {
                    method: "DELETE",
                    'Content-Type': 'application/json'
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

        /*
            {
                "timestamp": 1766557001764,
                "status": "OK",
                "message": "Executing task CleanIndicatorUtilLastWeek() manually complete.",
                "data": {
                    "serverIp": "172.16.100.200",
                    "oneWeekAgo": "2025-12-17T14:16:26.4249604",
                    "totalDeleted": 63405,
                    "batchCount": 14
                }
            }
        */
        return await response.json()
    }
    catch (error)
    {
        // Provide more context in the error message
        const errorWithContext 
            = new Error(`Failed to clean indicator until lastWeek! log: ${error.message}`);
        errorWithContext.originalError = error;
        console.error('Failed to clean indicator until lastWeek! log:', errorWithContext)  
        throw errorWithContext
    }
}

// 导出函数
export { 
    executeSendIntervalIndicatorReport, 
    executeCleanIndicatorUntilLastWeek 
};