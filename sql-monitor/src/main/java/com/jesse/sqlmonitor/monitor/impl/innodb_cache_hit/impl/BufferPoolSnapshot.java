package com.jesse.sqlmonitor.monitor.impl.innodb_cache_hit.impl;

import com.jesse.sqlmonitor.monitor.snapshot.SnapshotBase;
import lombok.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** 数据库缓冲池相关数据快照。*/
@Getter
@ToString(callSuper = true)
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class BufferPoolSnapshot extends SnapshotBase<BufferPoolSnapshot>
{
    /** Innodb_buffer_pool_read_ahead 预读操作总数。*/
    private final long readAhead;

    /** Innodb_buffer_pool_read_requests 逻辑读请求总数。*/
    private final long readRequests;

    /** Innodb_buffer_pool_reads 缓存未命中，从磁盘读取的总字节数。*/
    private final long reads;

    /** 构造空快照。*/
    @Contract(" -> new")
    public static @NotNull BufferPoolSnapshot empty()
    {
        return new
        BufferPoolSnapshot(0L, 0L, 0L);
    }

    /** 是否为空快照。*/
    @Override
    public boolean isEmpty()
    {
        return
        readAhead == 0L    &&
        readRequests == 0L &&
        reads == 0L;
    }

    /** 比较前后两次快照，检查指标是否被外部重置。*/
    @Override
    public boolean
    isReset(@NotNull BufferPoolSnapshot current)
    {
        return
        (current.reads < this.reads)         ||
        (current.readAhead < this.readAhead) ||
        (current.readRequests < this.readRequests);
    }
}
