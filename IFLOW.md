# IFLOW.md - Project-SQL-Monitor 项目上下文

## 项目概述

这是一个基于响应式架构的 MySQL 数据库指标监控系统，采用多模块设计。项目主要由三个核心模块组成：

1. **sql-monitor**：数据库指标监控模块，负责主动监控 MySQL 数据库的各种指标。
2. **indicator_receiver**：监控指标接收器模块，通过 RabbitMQ 接收指标数据并存储。
3. **SQL-Monitor-Dashboard**：前端监控仪表盘，使用 Vue.js 和 Chart.js 实现可视化展示。

整个系统使用 Spring WebFlux 响应式编程模型，结合 Redis 缓存、RabbitMQ 消息队列和 MySQL 数据库，实现了一个完整的监控解决方案。

## 技术栈

- **后端**: Java 21, Spring Boot 3.5.6, Spring WebFlux (响应式), R2DBC
- **数据库**: MySQL (R2DBC 驱动), Redis (缓存和分布式锁)
- **消息队列**: RabbitMQ
- **前端**: Vue 3, Chart.js, Vite
- **构建工具**: Maven (后端), npm (前端)
- **其他**: Lua 脚本 (用于 Redis 缓存操作), Swagger UI (API 文档)

## 项目架构

### 核心模块

1. **sql-monitor 模块**:
   - 主动监控 MySQL 指标（如 QPS、网络流量、连接数、InnoDB 缓存命中率、数据库大小等）
   - 使用 Redis 缓存监控结果，减少重复查询
   - 通过 RabbitMQ 发送监控指标到接收器
   - 包含定时任务来定期执行监控操作

2. **indicator_receiver 模块**:
   - 通过 RabbitMQ 接收来自监控器的指标数据
   - 将接收到的指标数据存储到 MySQL 数据库
   - 提供响应式 API 接口供前端调用

3. **SQL-Monitor-Dashboard 模块**:
   - 前端 Vue 3 应用
   - 提供图表化界面展示监控指标
   - 包含 QPS 图表和网络流量图表

## 配置文件

项目支持 test 和 prod 两种环境配置：

- **sql-monitor**:
  - `application-test.yml`: 测试环境配置，端口 19198，无 SSL
  - `application-prod.yml`: 生产环境配置，端口 11451，启用 SSL

- **indicator_receiver**:
  - `application-test.yml`: 测试环境配置，端口 65531
  - `application-prod.yml`: 生产环境配置（未在当前目录中显示）

## 构建和运行

### 后端模块 (Maven)

```bash
# 构建所有模块
mvn clean install

# 运行 sql-monitor 模块
cd sql-monitor
mvn spring-boot:run

# 运行 indicator_receiver 模块
cd indicator_receiver
mvn spring-boot:run
```

### 前端模块 (npm)

```bash
cd SQL-Monitor-Dashboard
npm install
npm run dev  # 开发模式
npm run build  # 构建生产版本
```

## 开发约定

- 使用响应式编程 (Spring WebFlux) 实现高并发和低延迟
- 使用 Redis 缓存减少重复计算和查询
- 使用 RabbitMQ 实现异步指标数据传输
- 使用 Lua 脚本优化 Redis 操作（如缓存指标数据）
- 代码覆盖率要求：sql-monitor 模块不低于 80%，indicator_receiver 模块不低于 75%
- 使用分布式锁确保缓存操作的原子性

## 特性

- **响应式架构**: 基于 Spring WebFlux 的全响应式设计
- **指标缓存**: 使用 Redis 缓存指标数据，提高查询效率
- **异步处理**: 通过 RabbitMQ 实现指标数据的异步传输
- **分布式锁**: 使用 Redis 分布式锁确保并发安全
- **定时监控**: 通过 Spring 的 @Scheduled 实现定期指标采集
- **API 文档**: 集成 Swagger UI 提供 API 文档
- **邮件通知**: 支持邮件发送功能（配置在 test 环境中）
- **SSL 支持**: 生产环境支持 SSL 连接
- **健康检查**: 包含 Redis 健康检查机制

## 项目结构

```
Project-SQL-Monitor/
├── sql-monitor/           # 指标监控模块
├── indicator_receiver/    # 指标接收器模块
├── SQL-Monitor-Dashboard/ # 前端仪表盘
├── ssl/                  # SSL 证书
├── documents/            # 项目文档和图片
└── sql_monitor_logs/     # SQL 监控日志
```