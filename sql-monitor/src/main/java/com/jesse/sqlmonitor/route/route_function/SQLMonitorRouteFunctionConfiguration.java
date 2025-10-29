package com.jesse.sqlmonitor.route.route_function;

import com.jesse.sqlmonitor.monitor.constants.GlobalStatusName;
import com.jesse.sqlmonitor.monitor.constants.SizeUnit;
import com.jesse.sqlmonitor.response_body.*;
import com.jesse.sqlmonitor.response_body.QPSResult;
import com.jesse.sqlmonitor.service.SQLMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.jetbrains.annotations.NotNull;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static com.jesse.sqlmonitor.route.endpoints_config.SQLMonitorEndPoints.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

/** SQL 指标监控程序路由函数配置。*/
@Configuration
public class SQLMonitorRouteFunctionConfiguration
{
    @Bean
    @RouterOperations(
        {
            @RouterOperation(
                path = BASE_ADDRESS_QUERY,
                operation = @Operation(
                    operationId = "getDatabaseAddress",
                    summary = "获取数据库服务器地址和端口号",
                    tags = {"数据库连接属性信息"},
                    responses = {
                        @ApiResponse(
                            responseCode = "200",
                            description = "成功",
                            content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                examples = {@ExampleObject(value = "172.16.100.200:3433")},
                                schema = @Schema(implementation = String.class)
                            )
                        ),
                        @ApiResponse(responseCode = "500", description = "数据库断连或其他未知错误")
                    }
                )
            ),
            @RouterOperation(
                path = QPS_QUERY,
                operation = @Operation(
                    operationId = "getQPS",
                    summary = "获取本数据库此刻的 QPS（每秒查询频率）",
                    description = "实时获取数据库连接的最大连接数、当前连接数和使用率百分比",
                    tags = {"数据库 QPS 监控"},
                    responses = {
                        @ApiResponse(
                            responseCode = "200",
                            description = "成功",
                            content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = QPSResult.class)
                            )
                        ),
                        @ApiResponse(responseCode = "500", description = "数据库断连或其他未知错误")
                    }
                )
            ),
            @RouterOperation(
                path = NETWORK_TRAFFIC_QUERY,
                operation = @Operation(
                    operationId = "getNetWorkTraffic",
                    summary = "获取本数据库网络流量相关数据",
                    tags = {"数据库网络流量监控"},
                    parameters = {
                        @Parameter(
                            name = "sizeUnit",
                            description = "计量单位，可选值：B, KB, MB, GB",
                            schema = @Schema(implementation = SizeUnit.class ),
                            required = true
                        )
                    },
                    responses = {
                        @ApiResponse(
                            responseCode = "200",
                            description = "成功",
                            content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = NetWorkTraffic.class)
                            )
                        ),
                        @ApiResponse(responseCode = "400", description = "计量单位参数非法"),
                        @ApiResponse(responseCode = "500", description = "数据库断连或其他未知错误")
                    }
                )
            ),
            @RouterOperation(
                path = GLOBAL_STATUS_QUERY,
                operation = @Operation(
                    operationId = "getGlobalStatus",
                    summary = "获取被数据库的全局状态",
                    tags = {"数据库的全局状态获取"},
                    parameters = {
                        @Parameter(
                            name = "statusName",
                            description = """
                                全局状态名全局状态名，
                                <a href='https://github.com/JesseZ332623/Project-SQL-Monitor/blob/main/sql-monitor/src/main/java/com/jesse/sqlmonitor/monitor/constants/GlobalStatusName.java'>
                                    查看完整 GlobalStatusName 列表
                                </a>
                                """,
                            required = true
                        )
                    },
                    responses = {
                        @ApiResponse(
                            responseCode = "200",
                            description = "成功",
                            content = @Content(mediaType = APPLICATION_JSON_VALUE)
                        ),
                        @ApiResponse(responseCode = "400", description = "全局状态名非法"),
                        @ApiResponse(responseCode = "500", description = "数据库断连或其他未知错误")
                    }
                )
            ),
            @RouterOperation(
                path = CONNECTION_USAGE_QUERY,
                operation = @Operation(
                    operationId = "getConnectionUsage",
                    summary = "获取数据库连接使用率相关数据",
                    tags = {"数据库连接使用率获取"},
                    responses = {
                        @ApiResponse(
                            responseCode = "200",
                            description = "成功",
                            content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ConnectionUsage.class)
                            )
                        ),
                        @ApiResponse(responseCode = "500", description = "数据库断连或其他未知错误")
                    }
                )
            ),
            @RouterOperation(
                path = DATABASE_SIZE_QUERY,
                operation = @Operation(
                    operationId = "getAllDatabaseSize",
                    summary = "获取本数据库所有库的大小",
                    tags = {"数据库所有库大小获取"},
                    responses = {
                        @ApiResponse(
                            responseCode = "200",
                            description = "成功",
                            content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = DatabaseSize.class)
                            )
                        ),
                        @ApiResponse(responseCode = "500", description = "数据库断连或其他未知错误")
                    }
                )
            ),
            @RouterOperation(
                path = INNODB_BUFFER_CACHE_HIT_RATE_QUERY,
                operation = @Operation(
                    operationId = "getInnodbBufferCacheHitRate",
                    summary = "查询 InnoDB 缓存命中率",
                    tags = {"InnoDB 缓存命中率获取"},
                    responses = {
                        @ApiResponse(
                            responseCode = "200",
                            description = "成功",
                            content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = InnodbBufferCacheHitRate.class)
                            )
                        ),
                        @ApiResponse(responseCode = "500", description = "数据库断连或其他未知错误")
                    }
                )
            ),
            @RouterOperation(
                path = SERVER_UPTIME_QUERY,
                operation = @Operation(
                    operationId = "getServerUpTime",
                    summary = "查询数据库服务器从启动至今所经过的秒数",
                    tags = {"数据库服务器从启动至今所经过的秒数获取"},
                    responses = {
                        @ApiResponse(
                            responseCode = "200",
                            description = "成功",
                            content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = String.class)
                            )
                        ),
                        @ApiResponse(responseCode = "500", description = "数据库断连或其他未知错误")
                    }
                )
            )
        }
    )
    public RouterFunction<ServerResponse>
    sqlMonitorRouteFunction(@NotNull SQLMonitorService sqlMonitorService)
    {
        return
        RouterFunctions.route()
            .GET(BASE_ADDRESS_QUERY,      accept(APPLICATION_JSON), sqlMonitorService::getDatabaseAddress)
            .GET(QPS_QUERY,               accept(APPLICATION_JSON), sqlMonitorService::getQPS)
            .GET(NETWORK_TRAFFIC_QUERY,   accept(APPLICATION_JSON), sqlMonitorService::getNetWorkTraffic)
            .GET(GLOBAL_STATUS_QUERY,     accept(APPLICATION_JSON), sqlMonitorService::getGlobalStatus)
            .GET(CONNECTION_USAGE_QUERY,  accept(APPLICATION_JSON), sqlMonitorService::getConnectionUsage)
            .GET(DATABASE_SIZE_QUERY,     accept(APPLICATION_JSON), sqlMonitorService::getDatabaseSize)
            .GET(INNODB_BUFFER_CACHE_HIT_RATE_QUERY, accept(APPLICATION_JSON), sqlMonitorService::getInnodbBufferCacheHitRate)
            .GET(SERVER_UPTIME_QUERY, accept(APPLICATION_JSON), sqlMonitorService::getServerUpTime)
            .build();
    }
}