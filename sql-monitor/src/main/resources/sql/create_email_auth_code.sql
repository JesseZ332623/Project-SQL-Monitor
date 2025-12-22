-- email_auth_code 建表语句。
CREATE TABLE `email_auth_code` (
  `id` int NOT NULL AUTO_INCREMENT,
  `email` varchar(64) DEFAULT NULL,
  `email_auth_code` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_email_auth_table_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;