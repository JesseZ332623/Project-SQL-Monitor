package com.jesse.sqlmonitor.scheduled_tasks;

import com.jesse.sqlmonitor.indicator_record.repository.MonitorLogRepository;
import com.jesse.sqlmonitor.properties.R2dbcMasterProperties;
import com.jesse.sqlmonitor.utils.DatetimeFormatter;
import io.github.jessez332623.reactive_email_sender.ReactiveEmailSender;
import io.github.jessez332623.reactive_email_sender.dto.EmailContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** 定时清历史指标数据的清理器。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class HistoricalIndicatorCleaner implements DisposableBean
{
    /** 响应式邮件发送器接口。*/
    private final ReactiveEmailSender emailSender;

    /** 运维人员的邮箱号（大嘘）。*/
    private static final
    String operationsStaffEmail = "zhj3191955858@gmail.com";

    /** 被检测数据库属性类。*/
    private final R2dbcMasterProperties masterProperties;

    /** 监控日志实体仓储类。*/
    private final MonitorLogRepository monitorLogRepository;

    /** 本定时任务是否正在运行中的标志位。*/
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /** 本定时任务的订阅凭据。*/
    private Disposable cleanupDisposable;

    /**
     * 在应用完全启动时启用本任务，
     * 启动后过两个星期执行第一次，后面一星期执行一次。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startTask()
    {
        this.cleanupDisposable
            = Flux.interval(Duration.ofDays(14L), Duration.ofDays(7L))
                  .flatMap((ignore) -> this.cleanIndicatorUtilLastWeek())
                  .subscribeOn(Schedulers.boundedElastic())
                  .subscribe();
    }

    /** 应用关闭的时候取消订阅本自动任务。*/
    @Override
    public void destroy()
    {
        if (Objects.nonNull(cleanupDisposable) && !cleanupDisposable.isDisposed())
        {
            // 如果检查到正在运行，
            if (isRunning.get())
            {
                try {
                    Thread.sleep(Duration.ofSeconds(3L));
                }
                catch (InterruptedException exception)
                {
                    Thread.currentThread().interrupt();
                    log.warn("Shutdown wait interrupted.");
                }
            }

            cleanupDisposable.dispose();
            cleanupDisposable = null;
        }
    }

    /**
     * 清理上一个星期之前的本监视数据库下的所有历史指标数据，
     * 确保数据库中只保留一个星期之内的数据库，维持数据的规模。
     */
    private @NotNull Mono<CleanUpResult>
    cleanIndicatorUtilLastWeek()
    {
        // 检查本定时任务是否正在被执行，避免并行的调用。
        if (!this.isRunning.compareAndSet(false, true))
        {
            log.warn("The task cleanIndicatorUtilLastWeek() already executing, skip...");
            return Mono.empty();
        }

        return
        Mono.defer(() -> {
            // 计算当前时间往前推一个星期的时间点
            LocalDateTime lastWeekPoint = LocalDateTime.now().minusDays(7L);
            String serverIp             = this.masterProperties.getHost();

            log.info("Start to clean up historical indicators older than one week.");

            return
            this.monitorLogRepository
                .deleteIndicator(serverIp, lastWeekPoint)
                .map((effectedRows) -> {

                    if (effectedRows > 0L)
                    {
                        log.info(
                            "Clean up {} rows historical indicator. (IP: {}, Deadline: {})",
                            effectedRows, serverIp, lastWeekPoint
                        );
                    }
                    else
                    {
                        log.info(
                            "There is no historical indicator data that needs to be cleaned up. (IP: {}, Deadline: {})",
                            serverIp, lastWeekPoint
                        );
                    }

                    return
                    CleanUpResult.builder()
                        .serverIp(serverIp)
                        .effectedRows(effectedRows)
                        .oneWeekAgo(lastWeekPoint)
                        .build();
                })
                .doOnSuccess((cleanUpResult) -> {
                    if (cleanUpResult.getEffectedRows() > 1000L) {
                        this.sendBulkDeletionReport(cleanUpResult);
                    }
                })
                .doOnError((error) -> {
                    log.error(
                        "Clean up historical indicators failed, (IP: {}, Caused by: {})",
                        serverIp, error.getMessage(), error
                    );

                    this.sendCleanUpFailedReport(lastWeekPoint, error);
                })
                .doFinally((signal) -> {
                    this.isRunning.set(false);
                    log.info("Terminate task cleanIndicatorUtilLastWeek(), signal type: {}.", signal);
                })
                .onErrorResume((error) -> Mono.empty());
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
                    cleanUpResult.getEffectedRows(),
                    cleanUpResult.getServerIp(),
                    cleanUpResult.getOneWeekAgo(),
                    DatetimeFormatter.NOW()))
            .flatMap(this.emailSender::sendEmail)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe();
    }

    /** 自动清理任务执行失败的时候也要向运维发送邮件。*/
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
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe();
    }
}