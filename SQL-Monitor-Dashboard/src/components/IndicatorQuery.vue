<template>
  <div class="indicator-query">
    <div class="card">
      <div class="card-header">
        <h2 class="card-title">Indicator Query</h2>
      </div>
      <div class="card-content">
        <div class="query-form">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Indicator Type</label>
              <select v-model="selectedType" class="form-control">
                <option value="">All Types</option>
                <option value="QPSResult">QPS</option>
                <option value="ConnectionUsage">Connection Usage</option>
                <option value="NetWorkTraffic">Network Traffic</option>
                <option value="InnodbBufferCacheHitRate">InnoDB Buffer Cache Hit Rate</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Server IP</label>
              <input v-model="serverIp" type="text" class="form-control readonly-input" readonly />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Start Time</label>
              <input v-model="fromDate" type="datetime-local" class="form-control" />
            </div>
            <div class="form-group">
              <label class="form-label">End Time</label>
              <input v-model="toDate" type="datetime-local" class="form-control" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Sort Order</label>
              <select v-model="orderBy" class="form-control">
                <option value="ASC">Ascending</option>
                <option value="DESC">Descending</option>
              </select>
            </div>
                        <div class="form-group">
                          <label class="form-label">Page</label>
                          <input 
                            v-model="page" 
                            type="number" 
                            class="form-control" 
                            min="1" 
                            :disabled="loading"
                          />
                        </div>
          </div>
          
          <!-- QPS Statistics Configuration -->
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">QPS Statistics Type</label>
              <select v-model="qpsStatisticsType" class="form-control" v-if="selectedType === 'QPSResult'">
                <option value="">None (Don't fetch statistics)</option>
                <option value="STANDARD_DEVIATION">Standard Deviation</option>
                <option value="AVERAGE">Average</option>
                <option value="MEDIAN_VALUE">Median Value</option>
                <option value="EXTREME_VALUE">Extreme Value (Min/Max)</option>
              </select>
              <div v-else class="form-control readonly-input" style="opacity: 0.5; cursor: not-allowed;">Select 'QPS' to enable</div>
            </div>
            <div class="form-group" v-if="selectedType === 'QPSResult' && qpsStatisticsType">
              <label class="form-label">Refresh Statistics</label>
              <button class="btn btn-primary" @click="fetchQPSStats" :disabled="loading || serverIpLoading">Fetch</button>
            </div>
          </div>
          <div class="form-actions">
            <button class="btn btn-primary" @click="fetchData" :disabled="loading">
              <svg class="btn-icon" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                <path fill-rule="evenodd"
                  d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z">
                </path>
              </svg>
              {{ loading ? 'Loading...' : 'Query' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="card" v-if="queryResult">
      <div class="card-header">
        <h2 class="card-title">Query Results</h2>
        <div class="card-actions">
          <span class="result-count">Total: {{ queryResult.metadata?.pagination?.totalItem || 0 }}</span>
        </div>
      </div>
      <div class="card-content">
        <div class="result-table-container" v-if="!loading && queryResult.data && queryResult.data.length > 0">
          <table class="result-table">
            <thead>
              <tr>
                <th>Datetime</th>
                <th v-if="selectedType === 'ConnectionUsage'">Max Connections</th>
                <th v-if="selectedType === 'ConnectionUsage'">Current Connections</th>
                <th v-if="selectedType === 'ConnectionUsage'">Usage %</th>
                <th v-if="selectedType === 'QPSResult'">QPS</th>
                <th v-if="selectedType === 'QPSResult'">Current Queries</th>
                <th v-if="selectedType === 'QPSResult'">Query Diff</th>
                <th v-if="selectedType === 'NetWorkTraffic'">Total Bytes Sent</th>
                <th v-if="selectedType === 'NetWorkTraffic'">Total Bytes Received</th>
                <th v-if="selectedType === 'NetWorkTraffic'">Sent Per Sec</th>
                <th v-if="selectedType === 'NetWorkTraffic'">Received Per Sec</th>
                <th v-if="selectedType === 'InnodbBufferCacheHitRate'">Cache Hit Rate</th>
                <th v-if="selectedType === 'InnodbBufferCacheHitRate'">Query Diff</th>
                <th>Valid</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in queryResult.data" :key="index">
                <td>{{ item.datetime }}</td>
                <td v-if="selectedType === 'ConnectionUsage'">{{ item.indicator.maxConnections }}</td>
                <td v-if="selectedType === 'ConnectionUsage'">{{ item.indicator.currentConnections }}</td>
                <td v-if="selectedType === 'ConnectionUsage'">{{ item.indicator.connectUsagePercent }}%</td>
                <td v-if="selectedType === 'QPSResult'">{{ item.indicator.qps?.toFixed(2) }}</td>
                <td v-if="selectedType === 'QPSResult'">{{ item.indicator.currentQueries }}</td>
                <td v-if="selectedType === 'QPSResult'">{{ item.indicator.queryDiff }}</td>
                <td v-if="selectedType === 'NetWorkTraffic'">{{ item.indicator.totalBytesSent }}</td>
                <td v-if="selectedType === 'NetWorkTraffic'">{{ item.indicator.totalBytesReceive }}</td>
                <td v-if="selectedType === 'NetWorkTraffic'">{{ item.indicator.sentPerSec }}/s</td>
                <td v-if="selectedType === 'NetWorkTraffic'">{{ item.indicator.receivePerSec }}/s</td>
                <td v-if="selectedType === 'InnodbBufferCacheHitRate'">{{ (item.indicator.cacheHitRate * 100).toFixed(4) }}%
                </td>
                <td v-if="selectedType === 'InnodbBufferCacheHitRate'">{{ item.indicator.queryDiff }}</td>
                <td>{{ item.indicator.valid ? 'Yes' : 'No' }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="no-data" v-else-if="!loading && (!queryResult.data || queryResult.data.length === 0)">
          No data found for the selected criteria.
        </div>

        <div class="loading-overlay" v-if="loading">
          <div class="loading-spinner"></div>
          <p>Loading data...</p>
        </div>
      </div>

              <!-- 分页控件 -->
            <div class="pagination"
              v-if="queryResult.metadata?.pagination && queryResult.data && queryResult.data.length > 0">
              <button class="btn btn-sm btn-secondary" @click="goToPage(1)" :disabled="page <= 1 || totalPages <= 1">
                First
              </button>
              <button class="btn btn-sm btn-secondary" @click="goToPage(page - 1)" :disabled="page <= 1 || totalPages <= 1">
                Previous
              </button>
              <span class="pagination-info">
                Page {{ page }} of {{ totalPages }}
              </span>
              <button class="btn btn-sm btn-secondary" @click="goToPage(page + 1)"
                :disabled="page >= totalPages || totalPages <= 1">
                Next
              </button>
              <button class="btn btn-sm btn-secondary"
                @click="goToPage(totalPages)"
                :disabled="page >= totalPages || totalPages <= 1">
                Last
              </button>
            </div>    </div>

    <!-- QPS Statistics Section -->
    <div class="card" v-if="qpsStats && qpsStatisticsType">
      <div class="card-header">
        <h2 class="card-title">QPS Statistics ({{ qpsStatisticsTypeLabel }})</h2>
      </div>
      <div class="card-content">
        <div class="stats-grid" v-if="qpsStatisticsType === 'STANDARD_DEVIATION'">
          <div class="metric-card">
            <div class="metric-label">Standard Deviation</div>
            <div class="metric-value">{{ qpsStats.stddev?.toFixed(4) }}</div>
            <div class="metric-description">Standard deviation of QPS values</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">Load Stability</div>
            <div class="metric-value">{{ (qpsStats.loadStability * 100).toFixed(2) }}%</div>
            <div class="metric-description">Stability of the load</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">Data Points</div>
            <div class="metric-value">{{ qpsStats.dataPoints }}</div>
            <div class="metric-description">Number of measurements</div>
          </div>
        </div>
        
        <div class="stats-grid" v-else-if="qpsStatisticsType === 'AVERAGE'">
          <div class="metric-card">
            <div class="metric-label">Average QPS</div>
            <div class="metric-value">{{ typeof qpsStats === 'number' ? qpsStats.toFixed(4) : (qpsStats?.toFixed ? qpsStats.toFixed(4) : 'N/A') }}</div>
            <div class="metric-description">Average QPS value</div>
          </div>
          <div class="metric-card" v-if="qpsStats.dataPoints !== undefined">
            <div class="metric-label">Data Points</div>
            <div class="metric-value">{{ qpsStats.dataPoints }}</div>
            <div class="metric-description">Number of measurements</div>
          </div>
        </div>
        
        <div class="stats-grid" v-else-if="qpsStatisticsType === 'MEDIAN_VALUE'">
          <div class="metric-card">
            <div class="metric-label">Median QPS</div>
            <div class="metric-value">{{ typeof qpsStats === 'number' ? qpsStats.toFixed(4) : (qpsStats?.toFixed ? qpsStats.toFixed(4) : 'N/A') }}</div>
            <div class="metric-description">Median QPS value</div>
          </div>
          <div class="metric-card" v-if="qpsStats.dataPoints !== undefined">
            <div class="metric-label">Data Points</div>
            <div class="metric-value">{{ qpsStats.dataPoints }}</div>
            <div class="metric-description">Number of measurements</div>
          </div>
        </div>
        
        <div class="stats-grid" v-else-if="qpsStatisticsType === 'EXTREME_VALUE'">
          <div class="metric-card">
            <div class="metric-label">Maximum QPS</div>
            <div class="metric-value">{{ qpsStats.max?.toFixed(4) }}</div>
            <div class="metric-description">Maximum QPS value</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">Minimum QPS</div>
            <div class="metric-value">{{ qpsStats.min?.toFixed(4) }}</div>
            <div class="metric-description">Minimum QPS value</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">Data Points</div>
            <div class="metric-value">{{ qpsStats.dataPoints }}</div>
            <div class="metric-description">Number of measurements</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed, watch, onUnmounted } from 'vue'
import { fetchIndicatorLog, fetchQPSStatistics } from '../services/indicator-query-api.js'
import { formatUtils } from '../utils/dataProcessor'
import { fetchBaseAddress } from '../services/monitor-api.js'

// 缓存相关的常量和函数
const CACHE_KEY = 'indicatorQueryCache';
const CACHE_EXPIRY_TIME = 24 * 60 * 60 * 1000; // 24小时过期时间

// 保存缓存数据
const saveToCache = (data) => {
  try {
    const cacheData = {
      ...data,
      timestamp: Date.now(),
    };
    localStorage.setItem(CACHE_KEY, JSON.stringify(cacheData));
  } catch (error) {
    console.warn('Failed to save to cache:', error);
  }
};

// 从缓存中读取数据
const loadFromCache = () => {
  try {
    const cachedData = localStorage.getItem(CACHE_KEY);
    if (cachedData) {
      const parsedData = JSON.parse(cachedData);
      // 检查缓存是否过期
      if (Date.now() - parsedData.timestamp < CACHE_EXPIRY_TIME) {
        return parsedData;
      } else {
        // 缓存过期，清除它
        localStorage.removeItem(CACHE_KEY);
      }
    }
  } catch (error) {
    console.warn('Failed to load from cache:', error);
  }
  return null;
};

// 清除缓存
const clearCache = () => {
  localStorage.removeItem(CACHE_KEY);
};

export default {
  name: 'IndicatorQuery',
  props: {},
  setup() {
    // 从缓存中恢复数据，如果存在且未过期
    const cachedData = loadFromCache();
    
    // 表单数据 - 优先使用缓存数据
    const selectedType = ref(cachedData?.selectedType || '')
    const serverIp = ref(cachedData?.serverIp || '')
    const serverIpLoading = ref(false) // 新增：服务器IP加载状态
    const fromDate = ref(cachedData?.fromDate || '')
    const toDate = ref(cachedData?.toDate || '')
    const orderBy = ref(cachedData?.orderBy || 'DESC')
    const page = ref(cachedData?.page || 1)
    const qpsStatisticsType = ref(cachedData?.qpsStatisticsType || 'STANDARD_DEVIATION') // 默认为标准差

    // 结果数据 - 优先使用缓存数据
    const queryResult = ref(cachedData?.queryResult || null)
    const qpsStats = ref(cachedData?.qpsStats || null)
    const loading = ref(false)

    // 保存状态到缓存的函数
    const saveStateToCache = () => {
      const stateData = {
        selectedType: selectedType.value,
        serverIp: serverIp.value,
        fromDate: fromDate.value,
        toDate: toDate.value,
        orderBy: orderBy.value,
        page: page.value,
        qpsStatisticsType: qpsStatisticsType.value,
        queryResult: queryResult.value,
        qpsStats: qpsStats.value,
      };
      saveToCache(stateData);
    };

    // 监听指标类型变化，清除QPS统计信息
    watch(selectedType, (newType) => {
      if (newType !== 'QPSResult') {
        // 如果不是QPSResult类型，清除QPS统计信息
        qpsStats.value = null;
        // 同时清除QPS统计类型选择
        qpsStatisticsType.value = '';
      }
      // 保存状态到缓存
      saveStateToCache();
    });

    // 监听QPS统计类型变化，如果指标类型不是QPSResult，清除统计信息
    watch(qpsStatisticsType, (newType) => {
      if (newType && selectedType.value !== 'QPSResult') {
        // 如果选择了QPS统计类型但指标类型不是QPSResult，清除统计信息
        qpsStats.value = null;
      }
      // 保存状态到缓存
      saveStateToCache();
    });

    // 监听其他响应式变量变化，保存到缓存
    watch([serverIp, fromDate, toDate, orderBy, page], () => {
      saveStateToCache();
    });

    // 初始化服务器IP
    const initServerIp = async () => {
      serverIpLoading.value = true;
      try {
        const response = await fetchBaseAddress();
        if (response && response.data) {
          // 从base address中提取IP地址，去掉端口号
          const address = response.data;
          // 提取IP地址部分（去掉端口号）
          if (address.includes(':')) {
            serverIp.value = address.split(':')[0]; // 取冒号前的部分，即IP地址
          } else {
            serverIp.value = address; // 如果没有端口号，则直接使用
          }
        }
      } catch (err) {
        console.error('Failed to fetch server IP:', err);
        serverIp.value = ''; // 设置为空，这样查询时会提示IP是必需的
      } finally {
        serverIpLoading.value = false;
      }
    }

    // 重置表单
    const resetForm = () => {
      selectedType.value = ''
      serverIp.value = ''
      fromDate.value = ''
      toDate.value = ''
      orderBy.value = 'DESC'
      page.value = 1
      queryResult.value = null
      qpsStats.value = null
      qpsStatisticsType.value = 'STANDARD_DEVIATION' // 重置为默认值
      
      // 清除缓存
      clearCache();
    }

    // 初始化组件时获取服务器IP
    onMounted(() => {
      // 如果缓存中有数据，不需要重新获取服务器IP
      if (!cachedData || !cachedData.serverIp) {
        initServerIp();
      }
    })

    // 在组件卸载时保存状态到缓存
    onUnmounted(() => {
      saveStateToCache();
    })

    // 获取数据
    const fetchData = async () => {
      // 防止重复提交
      if (loading.value) return;
      
      loading.value = true

      try {
        // 参数验证
        const type = selectedType.value || 'ALL'
        const ip = serverIp.value.trim()
        const from = fromDate.value
        const to = toDate.value
        const order = orderBy.value
        const pageNum = page.value

        // 验证IP地址格式
        if (!ip) {
          // 简单的日志记录，不显示错误
          console.error('Server IP is required. Please make sure the server is running and the IP is available.')
          return;
        }

        // 验证日期范围 (如果提供了日期)
        if (from && to && new Date(from) > new Date(to)) {
          console.error('Start time cannot be later than end time.')
          return;
        }

        // 验证页码
        if (pageNum < 1) {
          console.error('Page number must be at least 1.')
          return;
        }

        // 执行查询
        const result = await fetchIndicatorLog(type, ip, from, to, order, pageNum)
        queryResult.value = result

        // 根据配置获取QPS统计信息，但仅当指标类型为QPSResult时
        if (qpsStatisticsType.value && selectedType.value === 'QPSResult') {
          try {
            const statsResult = await fetchQPSStatistics(qpsStatisticsType.value, ip, from, to)
            qpsStats.value = statsResult.data
          } catch (statsError) {
            console.warn('Failed to fetch QPS statistics:', statsError.message)
            // 即使统计API失败，也保持之前的数据或设为null
            if (!qpsStats.value) {
              qpsStats.value = null
            }
          }
        } else {
          // 当指标类型不是QPS或没有选择统计类型时，清除统计信息
          qpsStats.value = null
        }
      } catch (err) {
        console.error('Error fetching indicator data:', err);
        // 不显示错误消息
      } finally {
        loading.value = false
        // 保存状态到缓存
        saveStateToCache();
      }
    }

    // 跳转到指定页
    const goToPage = (pageNum) => {
      if (pageNum < 1) return
      page.value = pageNum
      // 保存状态到缓存
      saveStateToCache();
      fetchData()
    }

    // 获取QPS统计信息
    const fetchQPSStats = async () => {
      if (!qpsStatisticsType.value) {
        qpsStats.value = null;
        // 保存状态到缓存
        saveStateToCache();
        return;
      }

      // 只有当指标类型为QPSResult时才允许获取QPS统计信息
      if (selectedType.value !== 'QPSResult') {
        console.error('QPS statistics can only be fetched for QPSResult indicator type.');
        qpsStats.value = null;
        // 保存状态到缓存
        saveStateToCache();
        return;
      }

      if (!serverIp.value) {
        console.error('Server IP is required for QPS statistics.');
        return;
      }

      // 防止重复提交统计查询
      if (loading.value) return;

      try {
        const statsResult = await fetchQPSStatistics(qpsStatisticsType.value, serverIp.value, fromDate.value, toDate.value);
        qpsStats.value = statsResult.data;
        // 保存状态到缓存
        saveStateToCache();
      } catch (statsError) {
        console.error('Failed to fetch QPS statistics:', statsError);
        qpsStats.value = null;
        // 保存状态到缓存
        saveStateToCache();
      }
    };
    // 计算总页数
    const totalPages = computed(() => {
      if (!queryResult.value?.metadata?.pagination?.totalItem) {
        return 1;
      }
      const totalItems = queryResult.value.metadata.pagination.totalItem;
      return Math.ceil(totalItems / 15);
    });
    
    // 获取统计类型标签
    const qpsStatisticsTypeLabel = computed(() => {
      switch (qpsStatisticsType.value) {
        case 'STANDARD_DEVIATION':
          return 'Standard Deviation';
        case 'AVERAGE':
          return 'Average';
        case 'MEDIAN_VALUE':
          return 'Median Value';
        case 'EXTREME_VALUE':
          return 'Extreme Value';
        default:
          return 'Statistics';
      }
    });

    return {
      selectedType,
      serverIp,
      serverIpLoading,
      fromDate,
      toDate,
      orderBy,
      page,
      qpsStatisticsType,
      qpsStatisticsTypeLabel,
      queryResult,
      qpsStats,
      loading,
      totalPages,
      fetchData,
      fetchQPSStats,
      resetForm,
      goToPage,
      formatBytes: formatUtils.bytes,
    }
  }
}
</script>

<style scoped>
.indicator-query {
  width: 100%;
}

.query-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 只读输入框样式 */
.form-control.readonly-input {
  background-color: var(--bg-tertiary);
  color: var(--text-tertiary);
  border: 1px solid var(--border-secondary);
  cursor: not-allowed;
  opacity: 0.7;
}

.form-control.readonly-input:focus {
  outline: none;
  box-shadow: none;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.form-control {
  padding: 8px 12px;
  border: 1px solid var(--border-primary);
  border-radius: 6px;
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
  font-size: 14px;
}

.form-control:focus {
  outline: none;
  border-color: var(--accent-secondary);
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.result-table-container {
  overflow-x: auto;
}

.result-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 800px;
}

.result-table th,
.result-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid var(--border-primary);
  color: var(--text-primary); /* 添加文字颜色 */
}

.result-table th {
  background-color: var(--bg-tertiary);
  font-weight: 600;
  color: var(--text-primary);
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.result-table tr:hover {
  background-color: var(--bg-tertiary);
}

.result-count {
  font-size: 14px;
  color: var(--text-secondary);
}

.no-data {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-secondary);
  font-style: italic;
}

.loading-overlay {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  position: relative;
}

.loading-overlay p {
  margin-top: 16px;
  color: var(--text-secondary);
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border-primary);
  border-top: 3px solid var(--accent-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }

  100% {
    transform: rotate(360deg);
  }
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-top: 1px solid var(--border-primary);
}

.pagination-info {
  color: var(--text-secondary);
  font-size: 14px;
  min-width: 150px;
  text-align: center;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
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

.metric-description {
  font-size: 12px;
  color: var(--text-tertiary);
}

@media (max-width: 768px) {
  .form-row {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .pagination {
    flex-direction: column;
    gap: 8px;
  }

  .pagination-info {
    min-width: auto;
  }
}
</style>