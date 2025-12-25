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
- **前端**: Vue 3, Chart.js, Vite, Web Workers
- **构建工具**: Maven (后端), npm (前端)
- **其他**: Lua 脚本 (用于 Redis 缓存操作), Swagger UI (API 文档), Micrometer (监控指标), Hutool (工具库), Reactive Email Sender (邮件发送)

## 项目架构

### 核心模块

1. **sql-monitor 模块**:
   - 主动监控 MySQL 指标（如 QPS、网络流量、连接数、InnoDB 缓存命中率、数据库大小等）
   - 使用 Redis 缓存监控结果，减少重复查询
   - 通过 RabbitMQ 发送监控指标到接收器
   - 包含定时任务来定期执行监控操作
   - 提供详细的 API 端点用于指标查询和统计
   - 实现邮件通知功能（配置在 test 环境中）
   - 实现历史数据自动清理功能
   - 实现定期指标报告发送功能

2. **indicator_receiver 模块**:
   - 通过 RabbitMQ 接收来自监控器的指标数据
   - 将接收到的指标数据存储到 MySQL 数据库
   - 提供响应式 API 接口供前端调用
   - 支持指标数据的查询和统计分析
   - 实现批量处理和缓冲区机制以提高性能

3. **SQL-Monitor-Dashboard 模块**:
   - 前端 Vue 3 应用
   - 提供图表化界面展示监控指标
   - 包含 QPS 图表和网络流量图表
   - 提供指标查询功能，支持按类型、时间范围、IP地址等条件查询
   - 实现自动刷新功能，使用 Web Workers 管理定时任务
   - 支持 QPS 统计分析（标准差、平均值、中位数、极值等）
   - 实现本地缓存功能，保存查询参数

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
mvn spring-boot:run -Ptest  # 使用测试配置
mvn spring-boot:run -Pprod  # 使用生产配置

# 运行 indicator_receiver 模块
cd indicator_receiver
mvn spring-boot:run -Ptest  # 使用测试配置
mvn spring-boot:run -Pprod  # 使用生产配置
```

### 前端模块 (npm)

```bash
cd SQL-Monitor-Dashboard
npm install
npm run dev  # 开发模式，自动代理到后端API
npm run build  # 构建生产版本
npm run preview  # 预览构建结果
```

## 开发约定

- 使用响应式编程 (Spring WebFlux) 实现高并发和低延迟
- 使用 Redis 缓存减少重复计算和查询
- 使用 RabbitMQ 实现异步指标数据传输
- 使用 Lua 脚本优化 Redis 操作（如缓存指标数据）
- 代码覆盖率要求：sql-monitor 模块不低于 80%，indicator_receiver 模块不低于 75%
- 使用分布式锁确保缓存操作的原子性
- 实现自动单元测试和集成测试
- 使用 Spring @Scheduled 实现定时任务

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
- **前端增强**: 包含指标查询页面，支持按条件查询历史数据
- **统计分析**: 支持 QPS 统计分析功能（标准差、平均值、中位数、极值等）
- **自动刷新**: 使用 Web Workers 实现前端定时刷新功能
- **单位自适应**: 自动调整网络流量单位（B/KB/MB/GB）
- **定时清理**: 自动清理超过一周的历史指标数据
- **定期报告**: 每日发送数据库指标报告邮件
- **本地缓存**: 前端支持查询参数本地缓存

## API 端点

### sql-monitor 模块
- `/api/sql-monitor/qps` - 获取 QPS 指标
- `/api/sql-monitor/connection-usage` - 获取连接使用情况
- `/api/sql-monitor/network-traffic` - 获取网络流量指标
- `/api/sql-monitor/cache-hit-rate` - 获取 InnoDB 缓存命中率
- `/api/sql-monitor/running-time` - 获取服务器运行时间
- `/api/sql-monitor/base-address` - 获取服务器基础地址

### indicator_receiver 模块
- `/api/indicator/log` - 查询指标日志（支持分页）
- `/api/indicator/qps-statistics` - 获取 QPS 统计数据

## 项目结构

```
Project-SQL-Monitor/
├── sql-monitor/           # 指标监控模块
│   ├── src/main/java/com/jesse/sqlmonitor/
│   │   ├── config/        # 配置类
│   │   ├── constants/     # 常量定义
│   │   ├── indicator_record/ # 指标记录相关
│   │   ├── luascript_reader/ # Lua脚本读取器
│   │   ├── monitor/       # 监控服务实现
│   │   ├── properties/    # 配置属性
│   │   ├── response_body/ # 响应体定义
│   │   ├── route/         # 路由配置
│   │   ├── scheduled_tasks/ # 定时任务
│   │   └── utils/         # 工具类
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-test.yml
│       ├── application-prod.yml
│       ├── lua-script/    # Lua脚本
│       └── sql/           # SQL脚本
├── indicator_receiver/    # 指标接收器模块
│   ├── src/main/java/com/jesse/indicator_receiver/
│   │   ├── config/        # 配置类
│   │   ├── contants/      # 常量定义
│   │   ├── entity/        # 实体类
│   │   ├── properties/    # 配置属性
│   │   ├── repository/    # 数据访问层
│   │   ├── response_body/ # 响应体定义
│   │   ├── route/         # 路由配置
│   │   ├── service/       # 业务逻辑层
│   │   └── utils/         # 工具类
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-test.yml
│       └── application-prod.yml
├── SQL-Monitor-Dashboard/ # 前端仪表盘
│   ├── src/
│   │   ├── components/    # Vue组件
│   │   │   ├── IndicatorQuery.vue # 指标查询组件
│   │   │   ├── NetworkTrafficChart.vue
│   │   │   └── QPSChart.vue
│   │   ├── services/      # API服务
│   │   │   ├── indicator-query-api.js
│   │   │   └── monitor-api.js
│   │   ├── utils/         # 工具函数
│   │   │   └── dataProcessor.js
│   │   └── workers/       # Web Worker
│   │       └── refreshWorker.js
│   ├── package.json
│   └── vite.config.js
├── ssl/                  # SSL 证书
├── documents/            # 项目文档和图片
└── sql_monitor_logs/     # SQL 监控日志
```

## 新增功能说明

### 指标查询功能
前端新增了指标查询页面，支持以下功能：
- 按指标类型（QPS、连接使用率、网络流量、InnoDB缓存命中率）查询
- 按服务器IP地址过滤
- 按时间范围查询（起始时间-结束时间）
- 按时间排序（升序/降序）
- 分页显示查询结果
- 支持15条记录每页的分页机制
- 本地缓存查询参数，避免重复输入

### QPS 统计功能
提供QPS统计分析功能，包括：
- 标准差计算（评估负载稳定性）
- 平均值计算
- 中位数计算
- 极值计算（最小值/最大值）
- 数据点数量统计

### 自动刷新机制
- 使用Web Worker实现后台定时器，避免主线程阻塞
- 支持可配置的刷新间隔（3s、5s、15s、30s、60s）
- 支持手动和自动刷新模式切换

### 网络流量单位自适应
- 自动计算最佳单位（B/KB/MB/GB）
- 支持手动选择单位
- 实时显示网络接收和发送速率

### 定时任务功能
项目包含两个重要的定时任务：

1. **历史指标数据清理器**：
   - 每周日凌晨0点自动清理一周前的历史指标数据
   - 防止数据库数据过度增长
   - 当删除数据量超过阈值时自动发送邮件通知运维人员
   - 包含超时检测和错误处理机制

2. **定期指标报告发送器**：
   - 每天9点和18点自动发送数据库指标报告邮件
   - 报告内容包括：数据增长量、QPS统计、网络流量、连接使用率等
   - 包含错误处理和邮件发送状态监控

### 邮件通知功能
- 自动发送历史数据清理报告邮件
- 自动发送定期指标报告邮件
- 支持批量删除超时和失败邮件通知
- 使用响应式邮件发送器实现

## 依赖管理

### 后端依赖更新
- Spring Boot 3.5.6
- Redisson 3.52.0 (用于分布式锁)
- Reactive Email Sender 1.1.5 (邮件发送功能)
- Micrometer 1.15.5 (监控指标)
- Hutool 5.8.41 (工具库)
- Reactor RabbitMQ 1.5.6 (异步消息处理)
- SpringDoc OpenAPI 2.8.13 (API文档)

### 前端依赖更新
- Vue 3.5.22
- Chart.js 4.5.0
- Vite (使用 rolldown-vite@7.1.14)
- Terser 5.44.0 (生产构建优化)

## 测试配置

- sql-monitor 模块代码覆盖率要求不低于 80%
- indicator_receiver 模块代码覆盖率要求不低于 75%
- 使用 JaCoCo 进行覆盖率分析
- 包含单元测试和集成测试
- 包含WebTestClient集成测试，覆盖指标查询和统计功能