package com.jesse.sqlmonitor.monitor.impl.network_traffic.impl;

import com.jesse.sqlmonitor.monitor.snapshot.SnapshotBase;
import lombok.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** 数据库网络流量信息快照。*/
@Getter
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class TrafficStateSnapshot extends SnapshotBase<TrafficStateSnapshot>
{
    /** [服务器发送给客户端] 的全部字节数 */
    private final long totalBytesSent;

    /** [所有客户端发送给服务器] 的全部字节数 */
    private final long totalBytesReceive;

    /** 构造一个空快照。*/
    @Contract(" -> new")
    public static @NotNull TrafficStateSnapshot
    empty()
    {
        return new
        TrafficStateSnapshot(0L, 0L);
    }

    /** 是否为空快照。*/
    @Override
    public boolean isEmpty()
    {
        return
        this.totalBytesSent == 0L &&
        this.totalBytesReceive == 0L;
    }

    /** 比较前后两次快照，检查指标是否被外部重置。*/
    @Override
    public boolean
    isReset(@NotNull TrafficStateSnapshot current)
    {
        return
        current.totalBytesSent < this.totalBytesSent ||
        current.totalBytesReceive < this.totalBytesReceive;
    }
}