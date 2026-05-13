package com.jesse.sqlmonitor.scheduled_tasks;

import com.jesse.sqlmonitor.constants.LuaScriptOperatorType;
import com.jesse.sqlmonitor.indicator_record.repository.MonitorLogRepository;
import com.jesse.sqlmonitor.indicator_record.repository.dto.AverageNetworkTraffic;
import com.jesse.sqlmonitor.indicator_record.repository.dto.IndicatorGrowth;
import com.jesse.sqlmonitor.monitor.MySQLIndicatorsRepository;
import com.jesse.sqlmonitor.properties.EmailTrafficLimitingProps;
import com.jesse.sqlmonitor.properties.R2dbcMasterProperties;
import com.jesse.sqlmonitor.response_body.ConnectionUsage;
import com.jesse.sqlmonitor.response_body.qps_statistics.ExtremeQPS;
import com.jesse.sqlmonitor.response_body.qps_statistics.StandingDeviationQPS;
import com.jesse.sqlmonitor.scheduled_tasks.constants.TaskExecutor;
import com.jesse.sqlmonitor.scheduled_tasks.dto.IndicatorReport;
import com.jesse.sqlmonitor.scheduled_tasks.exception.ScheduledTasksException;
import com.jesse.sqlmonitor.scheduled_tasks.exception.SendEmailContentFailed;
import com.jesse.sqlmonitor.scheduled_tasks.service.EmailContentSender;
import io.github.jessez332623.reactive_email_sender.ReactiveEmailSender;
import io.github.jessez332623.reactive_email_sender.dto.EmailContent;
import io.github.jessez332623.reactive_email_sender.exception.EmailException;
import io.github.jessez332623.reactive_luascript_reader.LuaScriptReader;
import io.github.jessez332623.reactive_luascript_reader.impl.LuaOperatorResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;

/** 定时向运维人员发送指标报告发送器。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class IntervalIndicatorReporter
{
    /**指标报告标题。*/
    private static final String
    REPORT_SUBJECT = "【数据库指标监视器】例行数据库指标报告";

    /** 运维人员的邮箱号（大嘘）。*/
    @Getter
    @Value("${app.operation-staff.email}")
    private String operationsStaffEmail;

    /** 响应式邮件发送器接口。*/
    private final ReactiveEmailSender emailSender;

    /** 邮件内容 {@link EmailContent} 发送器。*/
    private final EmailContentSender emailContentSender;

    /** Spring 响应式模板引擎（用于 Thymeleaf 框架的 HTML 渲染）。*/
    private final SpringWebFluxTemplateEngine templateEngine;

    /** Lua 脚本读取器。*/
    private final LuaScriptReader luaScriptReader;

    /** 专用于执行 Lua 脚本的 Redis 模板。*/
    private final
    ReactiveRedisTemplate<String, LuaOperatorResult> luaScriptTemplate;

    /** 被检测数据库属性类。*/
    private final
    R2dbcMasterProperties masterProperties;

    /** 邮件发送限流相关属性。*/
    private final
    EmailTrafficLimitingProps emailTrafficLimitingProps;

    /** 监控日志实体仓储类。*/
    private final MonitorLogRepository      monitorLogRepository;
    private final MySQLIndicatorsRepository indicatorsRepository;

    /** 本定时任务是否正在运行中的标志位。*/
    private final
    AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * 使用 Corn 表达式，
     * 在每天 9 点和 18 点执行指标报告发送任务。
     */
    @Scheduled(cron = "0 0 9,18 * * ?")
    public void startTask()
    {
        this.sendIntervalIndicatorReport(TaskExecutor.AUTO_TASK)
            .subscribe();
    }

    /**
     * 定期以邮件形式向运维报告数据库服务器相关指标数据。</br>
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
     *
     * @param taskExecutor 任务的调用者是？
     */
    public @NotNull Mono<Void>
    sendIntervalIndicatorReport(@NotNull TaskExecutor taskExecutor)
    {
        final String executorName = taskExecutor.getExecutor();

        return
        Mono.defer(() ->
            this.runningCheck(taskExecutor)
                .then(this.sendTrafficLimiting(taskExecutor))
                .then(this.fetchIndicatorReport())
                .flatMap(this::makeIndicatorReportEmail)
                .flatMap((emailContent) ->
                    this.emailContentSender
                        .sendEmailContent(emailContent)
                        .onErrorResume(
                            SendEmailContentFailed.class,
                            (exception) -> {
                                log.error("{}", exception.getMessage());

                                return
                                this.downGradeSend(executorName, emailContent);
                        })
                )
                .onErrorResume((exception) -> {
                    if (exception instanceof ScheduledTasksException) {
                        return Mono.error(exception);
                    }
                    else
                    {
                        final String errorMessage
                            = format(
                                "%s Send interval indicator report email to operation staff %s failed.",
                                executorName,
                                this.operationsStaffEmail
                            );

                        return Mono.error(
                            new ScheduledTasksException(errorMessage, exception)
                        );
                    }
                })
                .doFinally((signal) -> {
                    this.isRunning.set(false);
                    log.info(
                        "{} Task sendIntervalIndicatorReport() execute complete! signal type: {}.",
                        executorName, signal
                    );
                })
        );
    }

    /** 邮件发送限流键前缀。*/
    private String trafficLimitKeyPrefix() {
        return "sql-monitor-mail-rate:"+ this.masterProperties.getHost();
    }

    /** 检查本定时任务是否正在被执行，避免并行的调用。*/
    private Mono<Void>
    runningCheck(@NotNull TaskExecutor taskExecutor)
    {
        if (!this.isRunning.compareAndSet(false, true))
        {
            final String concurrencyMessage
                = format(
                "%s The task sendIntervalIndicatorReport() already executing, skip...",
                taskExecutor.getExecutor()
            );

            log.warn(concurrencyMessage);

            // 如果是自动执行的话，可以吞掉异常，只保留日志即可
            // 反之如果是 Http 请求手动调用，必须要往上传递异常
            return
            (taskExecutor.equals(TaskExecutor.AUTO_TASK))
                ? Mono.empty()
                : Mono.error(new ScheduledTasksException(concurrencyMessage));
        }

        return Mono.empty();
    }

    /**
     * 当无法将邮件内容发往消息队列时，就直接在这个请求发送邮件
     *（作为 {@link IntervalIndicatorReporter#sendIntervalIndicatorReport(TaskExecutor)} 的优雅降级策略存在）
     *
     * @param executorName 任务的执行者是？
     * @param emailContent 邮件内容实例
     *
     * @throws ScheduledTasksException 连兜底策略都失败了，直接向上传播本异常
     */
    private @NotNull Mono<Void>
    downGradeSend(String executorName, EmailContent emailContent)
    {
        return
        this.emailSender
            .sendEmail(emailContent)
            .onErrorResume(EmailException.class,
                (emailException) -> {
                final String errorMessage
                    = format(
                        "%s Send interval indicator report email to operation staff %s failed." +
                        "Error Type: [%s]",
                        executorName,
                        this.operationsStaffEmail,
                        emailException.getErrorType().name()
                    );

                return Mono.error(new ScheduledTasksException(errorMessage));
            });
    }

    /**
     * 采用令牌桶策略对邮件发送进行限流（限流参数通过配置给出）。
     *
     * @param executor 任务的执行者是？
     */
    private @NotNull Mono<Void>
    sendTrafficLimiting(@NotNull TaskExecutor executor)
    {
        // 定时任务不受限流的约束
        if (executor.equals(TaskExecutor.AUTO_TASK)) {
            return Mono.empty();
        }

        return
        this.luaScriptReader
            .read(LuaScriptOperatorType.EMAIL_SEND, "trafficLimiting.lua")
            .flatMap((script) -> {
                final String keyPrefix    = this.trafficLimitKeyPrefix();
                final int    fillTokens   = this.emailTrafficLimitingProps.getFillTokens();
                final long   fillDuration = this.emailTrafficLimitingProps.getFillDuration().toSeconds();
                final double rate         = (double) fillTokens / fillDuration;
                final int    burst        = this.emailTrafficLimitingProps.getBurst();

                return
                this.luaScriptTemplate
                    .execute(script, List.of(keyPrefix), rate, burst)
                    .next()
                    .flatMap((result) ->
                        switch (result.getStatus())
                        {
                            case "SEND_PASS" -> Mono.empty();

                            case "SEND_REJECT" ->
                                Mono.error(
                                    new ScheduledTasksException(
                                        "The number of attempts has exceeded the limit. Please try again later."
                                    )
                                );

                            case "UNKNOWN_ERROR" ->
                                Mono.error(new ScheduledTasksException(result.getMessage()));

                             // 不可到达的
                            default ->
                                Mono.error(
                                    new IllegalStateException(
                                        "Unexpected value: " + result.getStatus()
                                    )
                                );
                        });
            });
    }

    /** 收集各种指标，构建一个从今天开始到此刻时间段内的指标报告。*/
    private @NotNull Mono<IndicatorReport>
    fetchIndicatorReport()
    {
        final String serverIp          = this.masterProperties.getHost();
        final LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        final LocalDateTime to         = LocalDateTime.now();

        return
        Mono.zip(
             this.monitorLogRepository
                 .getIndicatorIncrement(serverIp, startOfDay),
             this.monitorLogRepository
                 .getAverageQPS(serverIp, startOfDay, to),
             this.monitorLogRepository
                 .getMedianQPS(serverIp, startOfDay, to),
             this.monitorLogRepository
                 .getExtremeQPS(serverIp, startOfDay, to),
             this.monitorLogRepository
                 .getStandingDeviationQPS(serverIp, startOfDay, to),
             this.monitorLogRepository
                 .getAverageNetworkTraffic(serverIp, startOfDay, to),
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

    private Mono<String>
    makeIndicatorContext(IndicatorReport report)
    {
        // 对于空指标，直接渲染空模板的 Context 即可
        if (Objects.isNull(report))
        {
            return Mono.fromCallable(() ->
                this.templateEngine.process(
                    "database-empty-indicator-report",
                    new Context(Locale.getDefault())
                )
            );
        }

        final Map<String, Object> indicatorMap = new HashMap<>();

        indicatorMap.put("checkTime",     report.getIndicatorGrowth().getCheckTime());
        indicatorMap.put("host",          this.masterProperties.getHost());
        indicatorMap.put("port",          this.masterProperties.getPort());
        indicatorMap.put("growthPoints",  report.getIndicatorGrowth().getGrowthDataPoints());
        indicatorMap.put("averageQps",    report.getAverageQPS());
        indicatorMap.put("medianQps",     report.getMedianQPS());
        indicatorMap.put("maxQps",        report.getExtremeQPS().getMax());
        indicatorMap.put("minQps",        report.getExtremeQPS().getMin());
        indicatorMap.put("stddev",        report.getStandingDeviationQPS().getStddev());
        indicatorMap.put("loadStability", report.getStandingDeviationQPS().getLoadStability());
        indicatorMap.put("avgReceived",   report.getAverageNetworkTraffic().getAverageReceived());
        indicatorMap.put("avgSent",       report.getAverageNetworkTraffic().getAverageSent());
        indicatorMap.put("currentConn",   report.getConnectionUsage().getCurrentConnections());
        indicatorMap.put("maxConn",       report.getConnectionUsage().getMaxConnections());
        indicatorMap.put("usagePercent",  report.getConnectionUsage().getConnectUsagePercent());

        return
        Mono.fromCallable(() ->
            this.templateEngine.process(
                "database-indicator-report",
                new Context(Locale.getDefault(), indicatorMap)
            )
        );
    }

    /** 根据指标报告，构造一份指标报告邮件。*/
    private @NotNull Mono<EmailContent>
    makeIndicatorReportEmail(@NotNull IndicatorReport report)
    {
        if (report.getIndicatorGrowth().getGrowthDataPoints() <= 0)
        {
            return
            this.makeIndicatorContext(null)
                .flatMap((html) ->
                    EmailContent.fromHtml(
                        this.operationsStaffEmail,
                        REPORT_SUBJECT,
                        html
                    )
                );
        }

        return
        this.makeIndicatorContext(report)
            .flatMap((html) ->
                EmailContent.fromHtml(
                    this.operationsStaffEmail,
                    REPORT_SUBJECT,
                    html
                )
            );
    }
}