-- 表 monitor_log 的建表语句
--（使用虚拟列 ver. 5.7+ 和虚拟列索引应对高数据增长下的查询速度问题）
-- 为 indicator 字段添加虚拟列
--  插入时：自动从 JSON 中提取并计算值
--  存储时：只存原始 JSON，不存计算结果
--  索引时：将计算结果物理存入索引结构
--  查询时：实时计算或使用索引快速查询
CREATE TABLE `monitor_log` (
  `log_id` bigint NOT NULL,
  `datetime` datetime NOT NULL,
  `indicator` varchar(255) NOT NULL,
  `server_ip` int unsigned NOT NULL,
  `indicator_type` enum('ConnectionUsage','DatabaseSize','InnodbBufferCacheHitRate','NetWorkTraffic','QPSResult') NOT NULL,
  `qps_value` decimal(15,8) GENERATED ALWAYS AS ((case when (json_unquote(json_extract(`indicator`,_utf8mb4'$.type')) = _utf8mb4'qps') then cast(json_unquote(json_extract(`indicator`,_utf8mb4'$.qps')) as decimal(15,8)) end)) VIRTUAL,
  `current_connections` int GENERATED ALWAYS AS ((case when (json_unquote(json_extract(`indicator`,_utf8mb4'$.type')) = _utf8mb4'connectionUsage') then cast(json_unquote(json_extract(`indicator`,_utf8mb4'$.currentConnections')) as unsigned) end)) VIRTUAL,
  `cacheHitRate` decimal(15,8) GENERATED ALWAYS AS ((case when (json_unquote(json_extract(`indicator`,_utf8mb4'$.type')) = _utf8mb4'innodbBufferCacheHitRate') then cast(json_unquote(json_extract(`indicator`,_utf8mb4'$.cacheHitRate')) as decimal(15,8)) end)) VIRTUAL,
  `receivePerSec` decimal(15,8) GENERATED ALWAYS AS ((case when (json_unquote(json_extract(`indicator`,_utf8mb4'$.type')) = _utf8mb4'networkTraffic') then cast(json_unquote(json_extract(`indicator`,_utf8mb4'$.receivePerSec')) as decimal(15,8)) end)) VIRTUAL,
  `sentPerSec` decimal(15,8) GENERATED ALWAYS AS ((case when (json_unquote(json_extract(`indicator`,_utf8mb4'$.type')) = _utf8mb4'networkTraffic') then cast(json_unquote(json_extract(`indicator`,_utf8mb4'$.sentPerSec')) as decimal(15,8)) end)) VIRTUAL,
  PRIMARY KEY (`log_id`),
  KEY `idx_ip_type_datetime` (`server_ip`,`indicator_type`,`datetime`),
  KEY `datetime_idx` (`datetime`),
  KEY `idx_qps` (`qps_value`),
  KEY `idx_current_connections` (`current_connections`),
  KEY `idx_cache_hitrate` (`cacheHitRate`),
  KEY `idx_network_traffic` (`receivePerSec`,`sentPerSec`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;