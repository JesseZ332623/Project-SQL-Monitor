package com.jesse.sqlmonitor.scheduled_tasks;

import com.jesse.sqlmonitor.indicator_record.repository.MonitorLogRepository;
import com.jesse.sqlmonitor.indicator_record.repository.dto.AverageNetworkTraffic;
import com.jesse.sqlmonitor.indicator_record.repository.dto.IndicatorGrowth;
import com.jesse.sqlmonitor.monitor.MySQLIndicatorsRepository;
import com.jesse.sqlmonitor.properties.R2dbcMasterProperties;
import com.jesse.sqlmonitor.response_body.ConnectionUsage;
import com.jesse.sqlmonitor.response_body.qps_statistics.ExtremeQPS;
import com.jesse.sqlmonitor.response_body.qps_statistics.StandingDeviationQPS;
import com.jesse.sqlmonitor.scheduled_tasks.dto.IndicatorReport;
import com.jesse.sqlmonitor.scheduled_tasks.exception.ScheduledTasksException;
import io.github.jessez332623.reactive_email_sender.ReactiveEmailSender;
import io.github.jessez332623.reactive_email_sender.dto.EmailContent;
import io.github.jessez332623.reactive_email_sender.exception.EmailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/** 定时向运维人员发送指标报告发送器。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class IntervalIndicatorReporter
{
    /** 运维人员的邮箱号（大嘘）。*/
    @Value("${app.operation-staff.email}")
    private String operationsStaffEmail;

    /** 响应式邮件发送器接口。*/
    private final ReactiveEmailSender emailSender;

    /** 被检测数据库属性类。*/
    private final R2dbcMasterProperties masterProperties;

    /** 监控日志实体仓储类。*/
    private final MonitorLogRepository      monitorLogRepository;
    private final MySQLIndicatorsRepository indicatorsRepository;

    /** 本定时任务是否正在运行中的标志位。*/
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * 使用 Corn 表达式，
     * 在每天 9 点和 18 点执行指标报告发送任务。
     */
    @Scheduled(cron = "0 0 9,18 * * ?")
    public void startTask()
    {
        this.sendIntervalIndicatorReport()
            .subscribe();
    }

    /**
     * 定期以邮件形式向运维报告数据库服务器相关指标数据（定时任务自动调用）。
     * 条目如下：
     *
     * <ul>
     *     <li>距离上一次报告时数据增长的数量</li>
     *     <li>数据库平均 QPS</li>
     *     <li>数据库 QPS 极值</li>
     *     <li>数据库 QPS 中位数</li>
     *     <li>数据库 QPS 标准差</li>
     *     <li>平均网络流量值</li>
     *     <li>当前数据库连接使用率</li>
     * </ul>
     */
    private @NotNull Mono<Void>
    sendIntervalIndicatorReport()
    {
        return
        Mono.defer(() -> {
            // 检查本定时任务是否正在被执行，避免并行的调用。
            if (!this.isRunning.compareAndSet(false, true))
            {
                log.warn("(Auto-task) The task sendIntervalIndicatorReport() already executing, skip...");
                return Mono.empty();
            }

            return
            this.fetchIndicatorReport()
                .flatMap(this::makeIndicatorReportEmail)
                .flatMap(this.emailSender::sendEmail)
                .doOnSuccess((ignore) ->
                    log.info(
                        "Send interval indicator report email to operation staff {} complete.",
                        this.operationsStaffEmail
                    )
                )
                .doOnError(EmailException.class, (emailException) ->
                    log.error(
                        "Send interval indicator report email to operation staff {} failed." +
                            "Caused by: [{}] {}",
                        this.operationsStaffEmail,
                        emailException.getErrorType().name(),
                        emailException.getMessage(), emailException
                    )
                )
                .doOnError((exception) ->
                    log.error(
                        "Send interval indicator report email to operation staff {} failed." +
                            "Caused by: {}",
                        this.operationsStaffEmail,
                        exception.getMessage(), exception
                    )
                )
                .doFinally((signal) -> {
                    this.isRunning.set(false);
                    log.info("Task sendIntervalIndicatorReport() execute complete! signal type: {}.", signal);
                })
                .onErrorResume((error) -> Mono.empty());
        });
    }

    /**
     * 定期以邮件形式向运维报告数据库服务器相关指标数据。</br>
     * （Http 请求手动调用，需要向外传播异常）
     * 条目如下：
     *
     * <ul>
     *     <li>距离上一次报告时数据增长的数量</li>
     *     <li>数据库平均 QPS</li>
     *     <li>数据库 QPS 极值</li>
     *     <li>数据库 QPS 中位数</li>
     *     <li>数据库 QPS 标准差</li>
     *     <li>平均网络流量值</li>
     *     <li>当前数据库连接使用率</li>
     * </ul>
     */
    public @NotNull Mono<Void>
    sendIntervalIndicatorReportManually()
    {
        return
        Mono.defer(() -> {
            // 检查本定时任务是否正在被执行，避免并行的调用。
            if (!this.isRunning.compareAndSet(false, true))
            {
                return
                Mono.error(
                    new ScheduledTasksException(
                        "The task sendIntervalIndicatorReport() already executing, skip..."
                    )
                );
            }

            return
            this.fetchIndicatorReport()
                .flatMap(this::makeIndicatorReportEmail)
                .flatMap(this.emailSender::sendEmail)
                .onErrorResume(EmailException.class,
                    (emailException) ->
                        Mono.error(
                            new ScheduledTasksException(
                                String.format(
                                    "Send interval indicator report email to operation staff %s failed." +
                                    "Error Type: [%s]",
                                    this.operationsStaffEmail,
                                    emailException.getErrorType().name()
                                )
                            )
                        )
                )
                .onErrorResume((exception) ->
                    Mono.error(
                        new ScheduledTasksException(
                            String.format(
                                "Send interval indicator report email to operation staff %s failed. " +
                                "Reason: (%s)",
                                this.operationsStaffEmail, exception.getMessage()
                            )
                        )
                    )
                )
                .doFinally((signal) -> {
                    this.isRunning.set(false);
                    log.info(
                        "(Http-request) Task sendIntervalIndicatorReport() execute complete! signal type: {}.",
                        signal
                    );
                });
        });
    }

    /** 收集各种指标，构建一个指标报告。*/
    private @NotNull Mono<IndicatorReport>
    fetchIndicatorReport()
    {
        final String serverIp = this.masterProperties.getHost();

        return
        Mono.zip(
             this.monitorLogRepository
                 .getIndicatorIncrement(serverIp, LocalDate.now().atStartOfDay()),
             this.monitorLogRepository
                 .getAverageQPS(serverIp, LocalDateTime.now()),
             this.monitorLogRepository
                 .getMedianQPS(serverIp, LocalDateTime.now()),
             this.monitorLogRepository
                 .getExtremeQPS(serverIp, LocalDateTime.now()),
             this.monitorLogRepository
                 .getStandingDeviationQPS(serverIp, LocalDateTime.now()),
             this.monitorLogRepository
                 .getAverageNetworkTraffic(serverIp, LocalDateTime.now()),
             this.indicatorsRepository.getConnectionUsage())
        .map((indicators) -> {
             final IndicatorGrowth indicatorGrowth = indicators.getT1();
             final Double averageQPS               = indicators.getT2();
             final Double medianQPS                = indicators.getT3();
             final ExtremeQPS extremeQPS           = indicators.getT4();
             final StandingDeviationQPS standingDeviationQPS   = indicators.getT5();
             final AverageNetworkTraffic averageNetworkTraffic = indicators.getT6();
             final ConnectionUsage connectionUsage             = indicators.getT7();

             return
             IndicatorReport.builder()
                 .indicatorGrowth(indicatorGrowth)
                 .averageQPS(averageQPS)
                 .medianQPS(medianQPS)
                 .extremeQPS(extremeQPS)
                 .standingDeviationQPS(standingDeviationQPS)
                 .averageNetworkTraffic(averageNetworkTraffic)
                 .connectionUsage(connectionUsage)
                 .build();
        });
    }

    /** 根据指标报告，构造一份指标报告邮件。*/
    private @NotNull Mono<EmailContent>
    makeIndicatorReportEmail(@NotNull IndicatorReport report)
    {
        return
        EmailContent.fromJustText(
            this.operationsStaffEmail,
            "【数据库指标监视器】例行数据库指标报告",
            """
            截止 %s，数据库（IP 地址：%s，端口：%s）今日共增长 %d 条指标数据，
            当前 QPS 平均值 = %f
                 QPS 中位数 = %f
                 最大 QPS = %f，最小 QPS = %f，
                 QPS 标准差 = %f，负载均衡律 = %f
            当前 数据库服务器网络流量平均值为：
                接收：%f Kb/s
                发送：%f Kb/s
            当前 数据库连接使用率为：
                %d / %d（%f %%）
            """.formatted(
                report.getIndicatorGrowth().getCheckTime(),
                this.masterProperties.getHost(),
                this.masterProperties.getPort(),
                report.getIndicatorGrowth().getGrowthDataPoints(),
                report.getAverageQPS(),
                report.getMedianQPS(),
                report.getExtremeQPS().getMax(),
                report.getExtremeQPS().getMin(),
                report.getStandingDeviationQPS().getStddev(),
                report.getStandingDeviationQPS().getLoadStability(),
                report.getAverageNetworkTraffic().getAverageReceived(),
                report.getAverageNetworkTraffic().getAverageSent(),
                report.getConnectionUsage().getCurrentConnections(),
                report.getConnectionUsage().getMaxConnections(),
                report.getConnectionUsage().getConnectUsagePercent()
            )
        );
    }
}