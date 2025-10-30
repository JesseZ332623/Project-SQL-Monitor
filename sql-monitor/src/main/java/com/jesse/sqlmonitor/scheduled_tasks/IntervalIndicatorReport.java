package com.jesse.sqlmonitor.scheduled_tasks;

import com.jesse.sqlmonitor.indicator_record.repository.MonitorLogRepository;
import com.jesse.sqlmonitor.properties.R2dbcMasterProperties;
import io.github.jessez332623.reactive_email_sender.ReactiveEmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

import java.util.concurrent.atomic.AtomicBoolean;

/** 定时向运维人员发送指标报告的装置（未完成）。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class IntervalIndicatorReport implements DisposableBean
{
    /** 运维人员的邮箱号（大嘘）。*/
    @Value("${app.operation-staff.email}")
    private String operationsStaffEmail;

    /** 响应式邮件发送器接口。*/
    private final ReactiveEmailSender emailSender;

    /** 被检测数据库属性类。*/
    private final R2dbcMasterProperties masterProperties;

    /** 监控日志实体仓储类。*/
    private final MonitorLogRepository monitorLogRepository;

    /** 本定时任务是否正在运行中的标志位。*/
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /** 本定时任务的订阅凭据。*/
    private Disposable intervalReportDisposable;

    @Override
    public void destroy() throws Exception {

    }
}