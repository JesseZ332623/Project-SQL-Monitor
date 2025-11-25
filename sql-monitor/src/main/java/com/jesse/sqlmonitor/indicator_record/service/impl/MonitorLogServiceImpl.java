package com.jesse.sqlmonitor.indicator_record.service.impl;

import com.jesse.sqlmonitor.constants.QueryOrder;
import com.jesse.sqlmonitor.indicator_record.entity.IndicatorType;
import com.jesse.sqlmonitor.indicator_record.exception.QueryIndicatorFailed;
import com.jesse.sqlmonitor.indicator_record.repository.MonitorLogRepository;
import com.jesse.sqlmonitor.indicator_record.service.MonitorLogService;
import com.jesse.sqlmonitor.indicator_record.service.constants.QPSStatisticsType;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import io.github.jessez332623.reactive_response_builder.ReactiveResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.jesse.sqlmonitor.utils.DatetimeFormatter.parseDatetime;
import static io.github.jessez332623.reactive_response_builder.utils.URLParamPrase.praseRequestParam;

/** 监控日志数据统计服务实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorLogServiceImpl implements MonitorLogService
{
    /** 指标类型所在的包路径。*/
    private static final
    String INDICATOR_TYPE_ROOT = "com.jesse.sqlmonitor.response_body.";

    /** 类型令牌缓存表。*/
    private static final
    Map<String, Class<? extends ResponseBase<?>>>
    typeCache = new ConcurrentHashMap<>();

    private final MonitorLogRepository monitorLogRepository;

    @SuppressWarnings("unchecked")
    private static @NotNull Class<? extends ResponseBase<?>>
    getIndicatorType(String typeName)
    {
        return
        typeCache.computeIfAbsent(typeName, (key) -> {
            try
            {
                final String completeTypeName
                    = INDICATOR_TYPE_ROOT + IndicatorType.valueOf(typeName).name();

                Class<?> clazz = Class.forName(completeTypeName);

                /*
                 * 检查 ResponseBase 类（Class 对象）是否与另一个类相同，
                 * 或者是另一个类的超类或超接口。
                 */
                if (ResponseBase.class.isAssignableFrom(clazz))
                {
                    return
                    (Class<? extends ResponseBase<?>>) clazz;
                }
                else
                {
                    throw new
                    IllegalArgumentException(
                        String.format(
                            "Class: %s doesn't extended by ResponseBase.",
                            completeTypeName
                        )
                    );
                }
            }
            catch (ClassNotFoundException | IllegalArgumentException exception)
            {
                throw new
                QueryIndicatorFailed(
                    String.format("Failed to get indicator-type by: %s", typeName),
                    exception
                );
            }
        });
    }

    private @NotNull Mono<ServerResponse>
    errorHandle(@NotNull Throwable cause)
    {
        return
        switch (cause)
        {
            case IllegalArgumentException illegalArgument ->
                ReactiveResponseBuilder
                    .BAD_REQUEST(illegalArgument.getMessage(), illegalArgument);

            case QueryIndicatorFailed queryFailed ->
                ReactiveResponseBuilder
                    .BAD_REQUEST(queryFailed.getMessage(), queryFailed);

            default ->
                ReactiveResponseBuilder
                    .INTERNAL_SERVER_ERROR(cause.getMessage(), cause);
        };
    }

    @Override
    public Mono<ServerResponse>
    fetchIndicatorLog(ServerRequest request)
    {
        return
        Mono.zip(
            praseRequestParam(request, "indicator-type"),
            praseRequestParam(request, "server-ip"),
            praseRequestParam(request, "until"),
            praseRequestParam(request, "order"))
        .flatMap((params) -> {
            final Class<? extends ResponseBase<?>>
            type = getIndicatorType(params.getT1());
            final String serverIP     = params.getT2();
            final LocalDateTime until = parseDatetime(params.getT3());
            final QueryOrder order    = QueryOrder.valueOf(params.getT4().toUpperCase(Locale.ROOT));

            return
            this.monitorLogRepository
                .fetchIndicator(type, serverIP, until, order)
                .collectList()
                .flatMap((indicators) ->
                    ReactiveResponseBuilder.OK(
                        indicators,
                        String.format(
                            "Searched %d %s indicators from server %s until %s.",
                            indicators.size(), type.getSimpleName(), serverIP, until
                        )
                    )
                );
        })
        .onErrorResume(this::errorHandle);
    }

    @Override
    public Mono<ServerResponse>
    qpsStatistics(ServerRequest request)
    {
        return
        Mono.zip(
            praseRequestParam(request, "type"),
            praseRequestParam(request, "server-ip"),
            praseRequestParam(request, "until"))
        .flatMap((params) -> {
            final QPSStatisticsType statisticsType
                = QPSStatisticsType.valueOf(params.getT1());
            final String serverIP     = params.getT2();
            final LocalDateTime until = parseDatetime(params.getT3());

            String customerMessage
                = String.format(
                    "To Calculate type of %s statistics data from server %s until %s.",
                    statisticsType.name(), serverIP, until
            );

            return
            switch (statisticsType)
            {
                case AVERAGE ->
                    this.monitorLogRepository
                        .getAverageQPS(serverIP, until)
                        .map((average) ->
                            Tuples.of(average, customerMessage));
                case MEDIAN_VALUE ->
                    this.monitorLogRepository
                        .getMedianQPS(serverIP, until)
                        .map((medium) ->
                            Tuples.of(medium, customerMessage));
                case EXTREME_VALUE ->
                    this.monitorLogRepository
                        .getExtremeQPS(serverIP, until)
                        .map((extreme) ->
                            Tuples.of(extreme, customerMessage));
                case STANDARD_DEVIATION ->
                    this.monitorLogRepository
                        .getStandingDeviationQPS(serverIP, until)
                        .map((stddev) ->
                            Tuples.of(stddev, customerMessage));
            };
        })
        .flatMap((statisticsRes) ->
            ReactiveResponseBuilder.OK(statisticsRes.getT1(), statisticsRes.getT2()))
        .onErrorResume(this::errorHandle);
    }
}