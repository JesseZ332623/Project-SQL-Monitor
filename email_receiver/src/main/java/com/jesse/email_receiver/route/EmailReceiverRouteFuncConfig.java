package com.jesse.email_receiver.route;

import com.jesse.email_receiver.route.filter.MonitoringFilter;
import com.jesse.email_receiver.service.ReceiverLifeCycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.jetbrains.annotations.NotNull;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static com.jesse.email_receiver.route.EmailSenderEndpoints.*;

/**  */
@Configuration
public class EmailReceiverRouteFuncConfig
{
    @Bean(name = "indicatorReceiverRouteFunc")
    @RouterOperations({
        @RouterOperation(
            path      = EmailSenderEndpoints.ROOT + RUN_STATUS,
            method    = { RequestMethod.GET },
            operation = @Operation(
                operationId = "runStatus",
                summary     = "获取邮件消费者运行状态",
                tags        = {"邮件消费者运行状态获取"},
                responses   = {
                    @ApiResponse(
                        responseCode = "200",
                        description  = "运行状态（RUNNING 或者 STOPPING）"
                    )
                }
            )
        ),
        @RouterOperation(
            path      = EmailSenderEndpoints.ROOT + START_RECEIVER,
            method    = { RequestMethod.POST },
            operation = @Operation(
                operationId = "start",
                summary     = "手动启动邮件消费者",
                tags        = {"手动启动邮件消费者"},
                responses   =  {
                    @ApiResponse(
                        responseCode = "200",
                        description  = "成功启动"
                    ),
                    @ApiResponse(
                        responseCode = "400",
                        description  = "重复启动"
                    )
                }
            )
        ),
        @RouterOperation(
            path      = EmailSenderEndpoints.ROOT + STOP_RECEIVER,
            method    = { RequestMethod.POST },
            operation = @Operation(
                operationId = "stop",
                summary     = "手动关闭邮件消费者",
                tags        = {"手动关闭邮件消费者"},
                responses   =  {
                    @ApiResponse(
                        responseCode = "200",
                        description  = "成功关闭"
                    ),
                    @ApiResponse(
                        responseCode = "400",
                        description  = "重复关闭"
                    )
                }
            )
        )
    })
    public RouterFunction<ServerResponse>
    indicatorReceiverRouteFunc(
        @NotNull @Autowired
        final ReceiverLifeCycleService receiverLifeCycleService
    )
    {
        return
        RouterFunctions.nest(
            RequestPredicates.path(EmailSenderEndpoints.ROOT)
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
            RouterFunctions.route()
                .GET(RUN_STATUS,      receiverLifeCycleService::runStatus)
                .POST(START_RECEIVER, receiverLifeCycleService::start)
                .POST(STOP_RECEIVER,  receiverLifeCycleService::stop)
                .filter(MonitoringFilter::doFilter)
                .build()
        );
    }
}
