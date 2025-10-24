package com.jesse.sqlmonitor.monitor.constants;

import lombok.Getter;

/** 数据库全局状态名枚举。*/
public enum GlobalStatusName
{
    /** 所有全局状态 */
    ALL_STATUS("%"),

    /** 提交的事务数 */
    TRANSACTIONS_COMMITED("Com_commit"),

    /** 回滚的事务数 */
    TRANSACTIONS_ROLLBACK("Com_rollback"),

    /** InnoDB 引擎相关指标 */
    INNODB_STATUS("Innodb_%"),

    /** 当前连接数 */
    CURRENT_THREADS_CONNECTED("Threads_connected"),

    /** 慢查询数量 */
    SLOW_QUERY_COUNT("Slow_queries"),

    /** 查询语句数 */
    COM_SELECT("Com_select"),

    /** 插入语句数 */
    COM_INSERT("Com_insert"),

    /** 更新语句数 */
    COM_UPDATE("Com_update"),

    /** 删除语句数 */
    COM_DELETE("Com_delete"),

    /** 总连接数 */
    TOTAL_CONNECTIONS("Connections"),

    /** 异常断开连接数 */
    ABORTED_CLIENTS("Aborted_clients"),

    /** 运行线程数 */
    THREADS_RUNNING("Threads_running"),

    /** 创建的线程数 */
    THREADS_CREATED("Threads_created"),

    /** 接收字节数 */
    BYTES_RECEIVED("Bytes_received"),

    /** 发送字节数 */
    BYTES_SENT("Bytes_sent"),

    /** 总查询数 */
    QUERIES("Queries"),

    /** 问题数 */
    QUESTIONS("Questions"),

    /** 创建的临时表数 */
    CREATED_TMP_TABLES("Created_tmp_tables"),

    /** 创建的磁盘临时表数 */
    CREATED_TMP_DISK_TABLES("Created_tmp_disk_tables"),

    /** 排序扫描次数 */
    SORT_SCAN("Sort_scan"),

    /** 排序行数 */
    SORT_ROWS("Sort_rows"),

    /** 全表连接次数 */
    SELECT_FULL_JOIN("Select_full_join"),

    /** 全表扫描次数 */
    SELECT_SCAN("Select_scan"),

    /** InnoDB 缓冲池读取请求 */
    INNODB_BUFFER_POOL_READ_REQUESTS("Innodb_buffer_pool_read_requests"),

    /** InnoDB 缓冲池物理读取 */
    INNODB_BUFFER_POOL_READS("Innodb_buffer_pool_reads"),

    /** InnoDB 读取行数 */
    INNODB_ROWS_READ("Innodb_rows_read"),

    /** InnoDB 插入行数 */
    INNODB_ROWS_INSERTED("Innodb_rows_inserted"),

    /** InnoDB 更新行数 */
    INNODB_ROWS_UPDATED("Innodb_rows_updated"),

    /** InnoDB 删除行数 */
    INNODB_ROWS_DELETED("Innodb_rows_deleted"),

    /** InnoDB 行锁等待次数 */
    INNODB_ROW_LOCK_WAITS("Innodb_row_lock_waits"),

    /** InnoDB 行锁平均等待时间 */
    INNODB_ROW_LOCK_TIME_AVG("Innodb_row_lock_time_avg"),

    /** 打开的表数 */
    OPEN_TABLES("Open_tables"),

    /** 表缓存命中数 */
    TABLE_OPEN_CACHE_HITS("Table_open_cache_hits"),

    /** 表缓存未命中数 */
    TABLE_OPEN_CACHE_MISSES("Table_open_cache_misses"),

    /** 立即获得的表锁数 */
    TABLE_LOCKS_IMMEDIATE("Table_locks_immediate"),

    /** 等待的表锁数 */
    TABLE_LOCKS_WAITED("Table_locks_waited"),

    /** 查询缓存命中数 */
    QCACHE_HITS("Qcache_hits"),

    /** 二进制日志缓存使用次数 */
    BINLOG_CACHE_USE("Binlog_cache_use"),

    /** 二进制日志缓存磁盘使用次数 */
    BINLOG_CACHE_DISK_USE("Binlog_cache_disk_use"),

    /** 服务器运行时间 */
    UPTIME("Uptime");

    @Getter
    private final String statusName;

    GlobalStatusName(String name) {
        this.statusName = name;
    }
}