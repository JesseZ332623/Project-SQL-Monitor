# MySQL 指标监控器（纯响应式架构）

<p>
    <a href="https://skillicons.dev">
        <img src="https://skillicons.dev/icons?i=mysql,redis,rabbitmq,spring,vue,vite,lua" alt="仪表盘页面预览">
    </a>
</p>

## 界面

![前端界面](/documents/application-preview.png)

## 架构图

![系统架构图](/documents/architecture_diagram/Project-SQL-Monitor%20架构图.png)

## 模块速览

- [基于 RabbitMQ 实现的监控指标接收器服务](https://github.com/JesseZ332623/Project-SQL-Monitor/tree/main/indicator_receiver/src/main/java/com/jesse/indicator_receiver)

- [数据库指标监视器服务](https://github.com/JesseZ332623/Project-SQL-Monitor/tree/main/sql-monitor/src/main/java/com/jesse/sqlmonitor)

- [邮件发送服务](https://github.com/JesseZ332623/Project-SQL-Monitor/tree/main/sql-monitor/src/main/java/com/jesse/email_receiver)

- [全局 ID 分配服务](https://github.com/JesseZ332623/Project-SQL-Monitor/tree/main/sql-monitor/src/main/java/com/jesse/id_allocator)

- [前端数据库指标仪表盘展示模块](https://github.com/JesseZ332623/Project-SQL-Monitor/tree/main/SQL-Monitor-Dashboard)

- [Gatling 负载测试模块](https://github.com/JesseZ332623/Project-SQL-Monitor/tree/main/gatling)

### [Apache License Version 2.0](https://github.com/JesseZ332623/Project-SQL-Monitor/blob/main/LICENSE)

*Last Update: 2026.03.05*