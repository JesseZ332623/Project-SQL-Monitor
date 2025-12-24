<template>
  <div class="scheduled-tasks">
    <div class="card">
      <div class="card-header">
        <h2 class="card-title">Scheduled Tasks</h2>
      </div>
      <div class="card-content">
        <div class="task-section">
          <div class="task-header">
            <h3>Send Indicator Report</h3>
            <p>Manually execute the routine indicator data report sending task</p>
          </div>
          <div class="task-actions">
            <button class="btn btn-primary" @click="executeSendIndicatorReport" :disabled="sendReportLoading">
              <svg class="btn-icon" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                <path fill-rule="evenodd"
                  d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z">
                </path>
              </svg>
              {{ sendReportLoading ? 'Sending...' : 'Send Indicator Report' }}
            </button>
          </div>
          <div class="task-result" v-if="sendReportResult">
            <div class="alert" :class="sendReportResult.success ? 'success-alert' : 'error-alert'">
              <div class="alert-content">
                <div class="alert-message">{{ sendReportResult.message }}</div>
                <div class="alert-timestamp" v-if="sendReportResult.timestamp">
                  {{ new Date(sendReportResult.timestamp).toLocaleString() }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="task-section">
          <div class="task-header">
            <h3>Clean Historical Indicators</h3>
            <p>Manually execute the historical indicator cleanup task (deletes data older than one week)</p>
          </div>
          <div class="task-actions">
            <button class="btn btn-danger" @click="executeCleanHistoricalIndicators" :disabled="cleanLoading">
              <svg class="btn-icon" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                <path fill-rule="evenodd"
                  d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z">
                </path>
              </svg>
              {{ cleanLoading ? 'Cleaning...' : 'Clean Historical Indicators' }}
            </button>
          </div>
          <div class="task-result" v-if="cleanResult">
            <div class="alert" :class="cleanResult.success ? 'success-alert' : 'error-alert'">
              <div class="alert-content">
                <div class="alert-message">{{ cleanResult.message }}</div>
                <div class="alert-details" v-if="cleanResult.data">
                  <p><strong>Server IP:</strong> {{ cleanResult.data.serverIp }}</p>
                  <p><strong>One Week Ago:</strong> {{ new Date(cleanResult.data.oneWeekAgo).toLocaleString() }}</p>
                  <p><strong>Total Deleted:</strong> {{ cleanResult.data.totalDeleted }}</p>
                  <p><strong>Batch Count:</strong> {{ cleanResult.data.batchCount }}</p>
                </div>
                <div class="alert-timestamp" v-if="cleanResult.timestamp">
                  {{ new Date(cleanResult.timestamp).toLocaleString() }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { 
  executeSendIntervalIndicatorReport, 
  executeCleanIndicatorUntilLastWeek 
} from '../services/scheduled-tasks-api.js'

export default {
  name: 'ScheduledTasks',
  setup() {
    // Loading states
    const sendReportLoading = ref(false)
    const cleanLoading = ref(false)

    // Result states
    const sendReportResult = ref(null)
    const cleanResult = ref(null)

    // Execute send indicator report
    const executeSendIndicatorReport = async () => {
      sendReportLoading.value = true
      sendReportResult.value = null

      try {
        const response = await executeSendIntervalIndicatorReport()
        sendReportResult.value = {
          success: true,
          message: response.message || 'Execute task sendIntervalIndicatorReportManually() complete!',
          timestamp: response.timestamp
        }
      } catch (error) {
        console.error('Failed to send indicator report:', error)
        sendReportResult.value = {
          success: false,
          message: error.message || 'Failed to send indicator report',
          timestamp: Date.now()
        }
      } finally {
        sendReportLoading.value = false
      }
    }

    // Execute clean historical indicators
    const executeCleanHistoricalIndicators = async () => {
      cleanLoading.value = true
      cleanResult.value = null

      try {
        const response = await executeCleanIndicatorUntilLastWeek()
        cleanResult.value = {
          success: true,
          message: response.message || 'Executing task CleanIndicatorUtilLastWeek() manually complete.',
          timestamp: response.timestamp,
          data: response.data
        }
      } catch (error) {
        console.error('Failed to clean historical indicators:', error)
        cleanResult.value = {
          success: false,
          message: error.message || 'Failed to clean historical indicators',
          timestamp: Date.now()
        }
      } finally {
        cleanLoading.value = false
      }
    }

    return {
      sendReportLoading,
      cleanLoading,
      sendReportResult,
      cleanResult,
      executeSendIndicatorReport,
      executeCleanHistoricalIndicators
    }
  }
}
</script>

<style scoped>
.scheduled-tasks {
  width: 100%;
}

.task-section {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-primary);
}

.task-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.task-header {
  margin-bottom: 16px;
}

.task-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.task-header p {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.task-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.task-result {
  margin-top: 16px;
}

.alert {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 16px;
  border: 1px solid;
  border-radius: 6px;
  margin: 16px 0;
}

.success-alert {
  border-color: rgba(63, 185, 80, 0.4);
  background-color: rgba(63, 185, 80, 0.1);
  color: var(--accent-primary);
}

.error-alert {
  border-color: rgba(248, 81, 73, 0.4);
  background-color: rgba(248, 81, 73, 0.1);
  color: var(--accent-danger);
}

.alert-content {
  flex: 1;
}

.alert-message {
  font-size: 14px;
  margin-bottom: 8px;
  word-break: break-word;
}

.alert-details {
  font-size: 13px;
  margin: 8px 0;
  padding: 8px;
  background-color: var(--bg-tertiary);
  border-radius: 4px;
}

.alert-details p {
  margin: 4px 0;
  color: var(--text-secondary);
}

.alert-details strong {
  color: var(--text-primary);
}

.alert-timestamp {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 8px;
}

.btn-danger {
  background-color: rgba(248, 81, 73, 0.1);
  border-color: rgba(248, 81, 73, 0.4);
  color: var(--accent-danger);
}

.btn-danger:hover:not(:disabled) {
  background-color: rgba(248, 81, 73, 0.2);
  border-color: rgba(248, 81, 73, 0.6);
}

.btn-icon {
  flex-shrink: 0;
}
</style>