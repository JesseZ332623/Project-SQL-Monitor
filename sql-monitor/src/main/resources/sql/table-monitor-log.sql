-- 表 monitor_log 相关的 SQL 语句

-- 表 monitor_log 的建表语句
--（使用虚拟列 ver. 5.7+ 和虚拟列索引应对高数据增长下的查询速度问题）
-- 为 indicator 字段添加虚拟列
--  插入时：自动从 JSON 中提取并计算值
--  存储时：只存原始 JSON，不存计算结果
--  索引时：将计算结果物理存入索引结构
--  查询时：实时计算或使用索引快速查询
CREATE TABLE `monitor_log` (
  `log_id` 		   BIGINT NOT NULL,
  `datetime` 	   DATETIME NOT NULL,
  `indicator` 	   VARCHAR(255) NOT NULL,
  `server_ip` 	   INT UNSIGNED NOT NULL,
  `indicator_type` ENUM('ConnectionUsage', 'DatabaseSize', 'InnodbBufferCacheHitRate', 'NetWorkTraffic', 'QPSResult') NOT NULL,
  `qps_value`      DECIMAL(15,8) GENERATED ALWAYS AS ((case when (json_unquote(json_extract(`indicator`,_utf8mb4'$.type')) = _utf8mb4'qps') then cast(json_unquote(json_extract(`indicator`,_utf8mb4'$.qps')) as decimal(15,8)) end)) VIRTUAL,
  `current_connections` INT GENERATED ALWAYS AS ((case when (json_unquote(json_extract(`indicator`,_utf8mb4'$.type')) = _utf8mb4'connectionUsage') then cast(json_unquote(json_extract(`indicator`,_utf8mb4'$.currentConnections')) as unsigned) end)) VIRTUAL,
  `cacheHitRate`        DECIMAL(15,8) GENERATED ALWAYS AS ((case when (json_unquote(json_extract(`indicator`,_utf8mb4'$.type')) = _utf8mb4'innodbBufferCacheHitRate') then cast(json_unquote(json_extract(`indicator`,_utf8mb4'$.cacheHitRate')) as decimal(15,8)) end)) VIRTUAL,
  `receivePerSec`       DECIMAL(15,8) GENERATED ALWAYS AS ((case when (json_unquote(json_extract(`indicator`,_utf8mb4'$.type')) = _utf8mb4'NetWorkTraffic') then cast(json_unquote(json_extract(`indicator`,_utf8mb4'$.receivePerSec')) as decimal(15,8)) end)) VIRTUAL,
  `sentPerSec`          DECIMAL(15,8) GENERATED ALWAYS AS ((case when (json_unquote(json_extract(`indicator`,_utf8mb4'$.type')) = _utf8mb4'NetWorkTraffic') then cast(json_unquote(json_extract(`indicator`,_utf8mb4'$.sentPerSec')) as decimal(15,8)) end)) VIRTUAL,
  PRIMARY KEY (`log_id`),
  UNIQUE KEY `logId_UNIQUE` (`log_id`),
  KEY `datetime_idx` (`datetime` DESC),
  KEY `server_ip_idx` (`server_ip` DESC),
  KEY `idx_ip_type_datetime` (`server_ip`,`indicator_type`,`datetime`),
  KEY `idx_type_ip_datetime` (`indicator_type`,`server_ip`,`datetime`),
  KEY `idx_qps` (`qps_value`),
  KEY `idx_current_connections` (`current_connections`),
  KEY `idx_cache_hitrate` (`cacheHitRate`),
  KEY `idx_network_traffic` (`receivePerSec`,`sentPerSec`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 计算某个数据库在某个时间点之前的 QPS 平均值
SELECT
    AVG(`qps_value`) AS average_qps
FROM
	sql_monitor.monitor_log
WHERE
	`indicator_type` = 'QPSResult'
    AND
    INET_NTOA(`server_ip`) = '172.16.100.200'
    AND
    `datetime` <= '2025-10-30 00:00:00';

-- 计算某个数据库某个时间段之前的最大和最小 QPS
SELECT
    MAX(`qps_value`) AS max_qps,
    MIN(`qps_value`) AS min_qps
FROM
	sql_monitor.monitor_log
WHERE
	`indicator_type` = 'QPSResult'
    AND
    INET_NTOA(`server_ip`) = '172.16.100.200'
    AND
    `datetime` <= '2025-10-30 00:00:00';

-- 计算某个数据库某个时间段之前的 QPS 中位数
SELECT
	AVG(qps) AS median_qps
FROM (
    SELECT
		`qps_value`							      AS qps,
        ROW_NUMBER() OVER (ORDER BY `qps_value`)  AS row_index,
        COUNT(*)     OVER () 					  AS total_rows
    FROM
		sql_monitor.monitor_log
    WHERE
		`indicator_type` = 'QPSResult'
        AND
        INET_NTOA(`server_ip`) = '172.16.100.200'
        AND
        `datetime` <= '2025-10-30 00:00:00'
) AS sorted
WHERE
	row_index IN (FLOOR((total_rows + 1) / 2), FLOOR((total_rows + 2) / 2));

-- 计算某个数据库某个时间段之前的标准差，平均值，总数据量
-- 此外，负载稳定性 = 标准差 / 平均值（这个可以放到应用层计算）
SELECT
    STDDEV_POP(`qps_value`) AS QPS_stddev,
    AVG(`qps_value`)        AS QPS_avg,
    COUNT(*)                AS data_points
FROM
	sql_monitor.monitor_log
WHERE
	`indicator_type` = 'QPSResult'
    AND
    INET_NTOA(`server_ip`) = '172.16.100.200'
    AND
    `datetime` <= '2025-10-30 00:00:00';