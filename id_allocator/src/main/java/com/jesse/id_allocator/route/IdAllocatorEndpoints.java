package com.jesse.id_allocator.route;

/** ID 分配器服务端点配置类。*/
public class IdAllocatorEndpoints
{
    public static final String
    ROOT = "/api/id_allocator";

    public static final String
    START_ALLOCATOR = "/start";

    public static final String
    STOP_ALLOCATOR = "/stop";

    public static final String
    RUN_STATUS     = "/run-status";

    public static final String
    NEXT_ID = "/next";

    public static final String
    NEXT_BATCH_IDS = "/next-batch";
}