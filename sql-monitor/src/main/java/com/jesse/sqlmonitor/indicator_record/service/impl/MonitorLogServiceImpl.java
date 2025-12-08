package com.jesse.sqlmonitor.indicator_record.service.impl;

import com.jesse.sqlmonitor.constants.QueryOrder;
import com.jesse.sqlmonitor.indicator_record.entity.IndicatorType;
import com.jesse.sqlmonitor.indicator_record.exception.QueryIndicatorFailed;
import com.jesse.sqlmonitor.indicator_record.repository.MonitorLogRepository;
import com.jesse.sqlmonitor.indicator_record.service.MonitorLogService;
import com.jesse.sqlmonitor.indicator_record.service.constants.QPSStatisticsType;
import com.jesse.sqlmonitor.response_body.base.ResponseBase;
import io.github.jessez332623.reactive_response_builder.ReactiveResponseBuilder;
import io.github.jessez332623.reactive_response_builder.pojo.Pagination;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.jesse.sqlmonitor.indicator_record.service.impl.PaginationLinkBuilder.buildPaginationLinks;
import static com.jesse.sqlmonitor.route.endpoints_config.IndicatorQueryEndpoints.MONITOR_LOG_QUERY;
import static com.jesse.sqlmonitor.utils.DatetimeFormatter.parseDatetime;
import static io.github.jessez332623.reactive_response_builder.utils.URLParamPrase.praseRequestParam;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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

    /** 通过指标类型名尝试提取并缓存指定类型令牌。*/
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

    /** 本服务类通用错误处理逻辑。*/
    private @NotNull Mono<ServerResponse>
    errorHandle(@NotNull Throwable cause)
    {
        return
        switch (cause)
        {
            case IllegalArgumentException illegalArgument ->
                ReactiveResponseBuilder
                    .BAD_REQUEST(illegalArgument.getMessage(), null);

            case QueryIndicatorFailed queryFailed ->
                ReactiveResponseBuilder
                    .BAD_REQUEST(queryFailed.getMessage(), null);

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
            praseRequestParam(request, "from"),
            praseRequestParam(request, "to"),
            praseRequestParam(request, "order"),
            praseRequestParam(request, "pageNo"),
            praseRequestParam(request, "perPageLimit"))
        .flatMap((params) -> {
            final Class<? extends ResponseBase<?>>
                type = getIndicatorType(params.getT1());
            final String serverIP    = params.getT2();
            final LocalDateTime from = parseDatetime(params.getT3());
            final LocalDateTime to   = parseDatetime(params.getT4());
            final QueryOrder order   = QueryOrder.valueOf(params.getT5().toUpperCase(Locale.ROOT));
            final int perPageLim     = Integer.parseInt(params.getT7());    // 每页数据量
            final int pageNo         = Integer.parseInt(params.getT6());    // 第几页？

            final long offset = (pageNo - 1L) * perPageLim; // 计算偏移量

            return
            Mono.zip(
                this.monitorLogRepository
                    .fetchIndicator(type, serverIP, from, to, order, perPageLim, offset)
                    .collectList(),
                this.monitorLogRepository
                    .getIndicatorCount(type, serverIP, from, to))
                .flatMap((pageableRes) -> {
                    final List<? extends ResponseBase<?>>
                        indicators        = pageableRes.getT1();
                    final long totalRows  = pageableRes.getT2();
                    final long totalPages = (totalRows + perPageLim - 1) / perPageLim; // 计算出当前参数下的总页数
                    final Map<String, String> hateOasArgs
                        = new HashMap<>(
                            Map.of(
                                "indicator-type", params.getT1(),
                                "server-ip",      params.getT2(),
                                "from",           from.format(ISO_LOCAL_DATE_TIME),
                                "to",             to.format(ISO_LOCAL_DATE_TIME),
                                "order",          params.getT5()
                            )
                    );

                    return
                    ReactiveResponseBuilder.OK(
                        indicators,
                        String.format(
                            "Searched %s indicators from server %s between %s and %s",
                            type.getSimpleName(), serverIP, from, to
                        ),
                        null,
                        new Pagination(pageNo, indicators.size(), totalRows),
                        buildPaginationLinks(
                            MONITOR_LOG_QUERY,
                            hateOasArgs,
                            pageNo, totalPages, perPageLim
                        )
                    );
                });
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
            praseRequestParam(request, "from"),
            praseRequestParam(request, "to")
        )
        .flatMap((params) -> {
            final QPSStatisticsType statisticsType
                = QPSStatisticsType.valueOf(params.getT1());
            final String serverIP    = params.getT2();
            final LocalDateTime from = parseDatetime(params.getT3());
            final LocalDateTime to   = parseDatetime(params.getT4());

            String customerMessage
                = String.format(
                    "To Calculate type of %s statistics data from server %s between %s and %s.",
                    statisticsType.name(), serverIP, from, to
            );

            return
            switch (statisticsType)
            {
                case AVERAGE ->
                    this.monitorLogRepository
                        .getAverageQPS(serverIP, from, to)
                        .map((average) ->
                            Tuples.of(average, customerMessage));
                case MEDIAN_VALUE ->
                    this.monitorLogRepository
                        .getMedianQPS(serverIP, from, to)
                        .map((medium) ->
                            Tuples.of(medium, customerMessage));
                case EXTREME_VALUE ->
                    this.monitorLogRepository
                        .getExtremeQPS(serverIP, from, to)
                        .map((extreme) ->
                            Tuples.of(extreme, customerMessage));
                case STANDARD_DEVIATION ->
                    this.monitorLogRepository
                        .getStandingDeviationQPS(serverIP, from, to)
                        .map((stddev) ->
                            Tuples.of(stddev, customerMessage));
            };
        })
        .flatMap((statisticsRes) ->
            ReactiveResponseBuilder.OK(statisticsRes.getT1(), statisticsRes.getT2()))
        .onErrorResume(this::errorHandle);
    }
}