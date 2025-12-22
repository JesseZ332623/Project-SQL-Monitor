
-- 删除指定 IP 的下，两个时间点之间的指定批次的指标数据。
--（被定时任务：HistoricalIndicatorCleaner 调用）。
DELETE FROM
    monitor_log
WHERE
    server_ip = INET_ATON(:serverIP)
    AND
    datetime BETWEEN :from AND :to
    ORDER BY log_id DESC
LIMIT :batchSize

--  读取两个时间点之间指定 IP 下所有指定类型的监控日志记录（按时间排序）。
SELECT
    datetime,
    indicator,
    INET_NTOA(server_ip) AS server_ip_str,
    indicator_type
FROM
    monitor_log
WHERE
    indicator_type = :indicatorType
    AND
    server_ip = INET_ATON(:serverIP)
    AND
   datetime BETWEEN :from AND :to
   ORDER BY datetime -- DESC / ASSC
LIMIT :limit OFFSET :offset

-- 读取两个时间点之间指定 IP 下指定类型的指标数据量
SELECT
    COUNT(*) AS count
FROM
    monitor_log
WHERE
    indicator_type = :indicatorType
    AND
    server_ip = INET_ATON(:serverIP)
    AND
    datetime BETWEEN :from AND :to

-- 查询指定 IP 指定时间点起到现在的指标增长数。
SELECT
    COUNT(*)    AS growth_data_points,
    NOW()       AS check_time
FROM
    sql_monitor.monitor_log
WHERE
     `server_ip` = INET_ATON(:serverIP)
     AND
     `datetime` BETWEEN :startPoint AND NOW()

-- 查询并计算指定 IP 指定时间段内的 QPS 平均值。
SELECT
    AVG(`qps_value`) AS average_qps
FROM
    sql_monitor.monitor_log
WHERE
    `indicator_type` = 'QPSResult'
    AND
    `server_ip` = INET_ATON(:serverIP)
    AND
    `datetime` BETWEEN :from AND :to

-- 查询并计算指定 IP 指定时间段内的 QPS 中位数
SELECT
    AVG(qps) AS median_qps
FROM (
       SELECT
           `qps_value` 							 AS qps,
           ROW_NUMBER() OVER (ORDER BY `qps_value`) AS row_index,
           COUNT(*)     OVER () 					 AS total_rows
       FROM
            sql_monitor.monitor_log
       WHERE
            `indicator_type` = 'QPSResult'
            AND
            `server_ip` = INET_ATON(:serverIP)
            AND
            `datetime` BETWEEN :from AND :to
       ) AS sorted
       WHERE
            row_index IN (FLOOR((total_rows + 1) / 2), FLOOR((total_rows + 2) / 2))

-- 查询并计算指定 IP 指定时间段内的 QPS 极值
SELECT
      MAX(`qps_value`) AS max_qps,
      MIN(`qps_value`) AS min_qps
FROM
     sql_monitor.monitor_log
WHERE
      `indicator_type` = 'QPSResult'
      AND
      `server_ip` = INET_ATON(:serverIP)
      AND
      `datetime` BETWEEN :from AND :to

-- 查询并计算指定 IP 指定时间段内的 QPS 标准差。
SELECT
      STDDEV_POP(`qps_value`) AS QPS_stddev,
      AVG(`qps_value`)        AS QPS_avg,
      COUNT(*)                AS data_points
FROM
      sql_monitor.monitor_log
WHERE
     `indicator_type` = 'QPSResult'
     AND
     `server_ip` = INET_ATON(:serverIP)
     AND
     `datetime` BETWEEN :from AND :to


-- 查询并计算
-- 指定 IP 指定时间段内的 数据库网络流量 平均值（单位：Kb/s）。
SELECT
      AVG(`sentPerSec`)    AS average_sent,
      AVG(`receivePerSec`) AS average_receive
FROM
      sql_monitor.monitor_log
WHERE
     `indicator_type` = 'NetWorkTraffic'
     AND
     `server_ip` = INET_ATON(:serverIP)
     AND
     `datetime` BETWEEN :from AND :to