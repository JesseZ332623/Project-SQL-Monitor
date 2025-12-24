package com.jesse.sqlmonitor.scheduled_tasks;

import com.jesse.sqlmonitor.indicator_record.repository.MonitorLogRepository;
import com.jesse.sqlmonitor.properties.HistoricalIndicatorCleanerProps;
import com.jesse.sqlmonitor.properties.R2dbcMasterProperties;
import com.jesse.sqlmonitor.scheduled_tasks.constants.TaskExecuter;
import com.jesse.sqlmonitor.scheduled_tasks.dto.CleanUpResult;
import com.jesse.sqlmonitor.scheduled_tasks.exception.ScheduledTasksException;
import com.jesse.sqlmonitor.utils.DatetimeFormatter;
import io.github.jessez332623.reactive_email_sender.ReactiveEmailSender;
import io.github.jessez332623.reactive_email_sender.dto.EmailContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;

/** 定时清历史指标数据的清理器。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class HistoricalIndicatorCleaner
{
    /**
     * 删除操作起始时间点。
     *（MySQL 解析不了 LocalDateTime.MIN，这里构造一个 UNIX 纪元时间来替代）
     */
    private final static
    LocalDateTime DELETE_FROM
        = LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0);

    /** 历史指标数据删除任务相关属性类。 */
    private final HistoricalIndicatorCleanerProps cleanerProps;

    /** 响应式邮件发送器接口。*/
    private final ReactiveEmailSender emailSender;

    /** 运维人员的邮箱号（大嘘）。*/
    @Value("${app.operation-staff.email}")
    private String operationsStaffEmail;

    /** 被检测数据库属性类。*/
    private final R2dbcMasterProperties masterProperties;

    /** 监控日志实体仓储类。*/
    private final MonitorLogRepository monitorLogRepository;

    /** 本定时任务是否正在运行中的标志位。*/
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * 使用 Corn 表达式，
     * 每周日凌晨 0 点整执行清理操作。
     */
    @Scheduled(cron = "0 0 0 ? * SUN")
    public void startTask()
    {
        this.cleanIndicatorUntilLastWeek(TaskExecuter.AUTO_TASK)
            .subscribe();
    }

    /**
     * 清理上一个星期之前的本监视数据库下的所有历史指标数据，
     * 确保数据库中只保留一个星期之内的数据库，维持数据的规模。
     *
     * @param taskExecuter 任务的调用者是？
     *
     * @return 清理任务的执行结果
     */
    public @NotNull Mono<CleanUpResult>
    cleanIndicatorUntilLastWeek(@NonNull TaskExecuter taskExecuter)
    {
        final String executerName = taskExecuter.getExecuter();

        return
        Mono.defer(() -> {
            // 检查本定时任务是否正在被执行，避免并行的调用。
            if (!this.isRunning.compareAndSet(false, true))
            {
                final String concurrencyMessage
                    = format(
                        "%s The task cleanIndicatorUntilLastWeek() already executing, skip...",
                        executerName
                    );

                log.warn(concurrencyMessage);

                // 如果是自动执行的话，可以吞掉异常，只保留日志即可
                // 反之如果是 Http 请求手动调用，必须要往上传递异常
                return (taskExecuter.equals(TaskExecuter.AUTO_TASK))
                        ? Mono.empty()
                        : Mono.error(new ScheduledTasksException(concurrencyMessage));
            }

            final String        serverIp      = this.masterProperties.getHost();
            final LocalDateTime lastWeekPoint = LocalDateTime.now().minusDays(7L);

            // 初始化一个批量删除结果
            CleanUpResult cleanUpResult = CleanUpResult.init(serverIp, lastWeekPoint);

            return
            this.monitorLogRepository
                .deleteOneBatchIndicator(
                    serverIp, DELETE_FROM, lastWeekPoint,
                    this.cleanerProps.getBatchSize())
                .expand((deleted) -> {
                    if (deleted > 0)
                    {
                        cleanUpResult.batchIncrement();

                        return
                        this.monitorLogRepository
                            .deleteOneBatchIndicator(
                                serverIp,
                                DELETE_FROM, lastWeekPoint,
                                this.cleanerProps.getBatchSize())
                            .delayElement(Duration.ofMillis(50L));
                    }
                    else
                    {
                        log.info(
                            "{} There is no historical indicator data that needs to be cleaned up. " +
                            "(IP: {}, Deadline: {})",
                            executerName, serverIp, lastWeekPoint
                        );

                        return Mono.empty();
                    }
                })
                .doOnNext(cleanUpResult::addDeleted)
                .ignoreElements().thenReturn(cleanUpResult)
                .timeout(this.cleanerProps.getBatchDeleteTimeout()) // 要是删了很久都没删完，也是有问题的
                .doOnSuccess((ignore) -> {
                    log.info(
                        "{} Clean up {} rows historical indicator. (IP: {}, Deadline: {})",
                        executerName,
                        cleanUpResult.getTotalDeleted().get(),
                        serverIp, lastWeekPoint
                    );

                    // 如果总删除数超过阈值，可以考虑发送一条邮件给运维
                    if (cleanUpResult.getTotalDeleted().get() > this.cleanerProps.getTotalDeleteLimit()) {
                        this.sendBulkDeletionReport(cleanUpResult);
                    }
                })
                .onErrorResume(
                    TimeoutException.class,
                    (timeout) -> {
                        this.sendDeletionTimeoutReport(cleanUpResult);

                        return
                        Mono.error(
                            new ScheduledTasksException(
                                format(
                                    "%s Clean up historical indicators timeout!" +
                                    "Delete %d rows so far. (IP: %s, Deadline: %s)",
                                    executerName,
                                    cleanUpResult.getTotalDeleted().get(),
                                    serverIp, lastWeekPoint
                                )
                            )
                        );
                    })
                    .onErrorResume((error) -> {
                        final String errorMessage
                            = format(
                                "%s Clean up historical indicators failed, (IP: %s, Caused by: %s)",
                                executerName, serverIp, error.getMessage()
                            );

                        final ScheduledTasksException exception
                            = new ScheduledTasksException(errorMessage);

                        this.sendCleanUpFailedReport(lastWeekPoint, exception);

                        return Mono.error(exception);
                    })
                    .doFinally((signal) -> {
                        this.isRunning.set(false);
                        log.info(
                            "{} Task cleanIndicatorUtilLastWeek() execute complete! " +
                            "signal type: {}.",
                            executerName, signal
                        );
                    });
            });
    }

    /** 向运维发送大批量删除历史指标数据的报告。*/
    private void
    sendBulkDeletionReport(@NotNull CleanUpResult cleanUpResult)
    {
        EmailContent.fromJustText(
           operationsStaffEmail,
           "【数据库指标监视器】大批量删除历史指标数据的报告",
           """
                清理历史指标数据 %s 条，
                被检测的数据库服务 IP: [%s]，
                删除时间点：[%s] 之前的所有数据，
                执行时间：[%s]
                """.formatted(
                    cleanUpResult.getTotalDeleted().get(),
                    cleanUpResult.getServerIp(),
                    cleanUpResult.getOneWeekAgo(),
                    DatetimeFormatter.NOW()))
            .flatMap(this.emailSender::sendEmail)
            .subscribe(
                null,   // 没有结果需要消费
                (exception) ->
                    log.error(
                        "Failed to send bulk deletion report email, Caused by: {}",
                        exception.getMessage(), exception
                    ),
                () -> log.info("Send batch delete report successfully!")
            );
    }

    /** 向运维发送批量删除超时的报告。*/
    private void
    sendDeletionTimeoutReport(@NotNull CleanUpResult cleanUpResult)
    {
        EmailContent.fromJustText(
            operationsStaffEmail,
            "【数据库指标监视器】批量删除历史指标数据超时的报告",
            """
                批量删除历史指标数据超过 2 分钟限制！
                被检测的数据库服务 IP: [%s]，
                已经删除时间点：[%s] 之前的 [%d] 条数据，
                执行时间：[%s]
                """.formatted(
                cleanUpResult.getServerIp(),
                cleanUpResult.getOneWeekAgo(),
                cleanUpResult.getTotalDeleted().get(),
                DatetimeFormatter.NOW()))
        .flatMap(this.emailSender::sendEmail)
        .subscribe(
            null,
            (exception) ->
                log.error(
                    "Failed to send bulk deletion timeout report email, Caused by: {}",
                    exception.getMessage(), exception
                ),
            () -> log.info("Send bulk deletion timeout report email successfully!")
        );
    }

    /**
     * 自动清理任务执行失败的时候也要向运维发送邮件。
     *
     * @param timePoint 删除时间点
     * @param cause     造成删除失败的异常
     */
    private void
    sendCleanUpFailedReport(LocalDateTime timePoint, Throwable cause)
    {
        EmailContent.fromJustText(
                operationsStaffEmail,
                "【数据库指标监视器】删除历史指标数据失败的报告",
                """
                     被检测的数据库服务 IP: [%s]，
                     删除时间点：[%s] 之前的所有数据，
                     错误原因：%s，
                     执行时间：[%s]，
                     请检查手动执行清理操作并检查应用状态。
                     """.formatted(
                         this.masterProperties.getHost(),
                         timePoint,
                         (Objects.isNull(cause)) ? "UNKNOW ERROR" : cause.getMessage(),
                         DatetimeFormatter.NOW()
                    )
            )
            .flatMap(this.emailSender::sendEmail)
            .subscribe(
                null,
                (exception) ->
                    log.error(
                        "Failed to send bulk deletion failed report email, Caused by: {}",
                        exception.getMessage(), exception
                    ),
                () -> log.info("Send bulk deletion failed report email successfully!")
            );
    }
}