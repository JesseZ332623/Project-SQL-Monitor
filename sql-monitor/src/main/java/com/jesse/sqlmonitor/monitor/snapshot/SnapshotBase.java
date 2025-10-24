package com.jesse.sqlmonitor.monitor.snapshot;

import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/** 所有 Snapshot 的基类。*/
@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public abstract class SnapshotBase<T extends SnapshotBase<T>>
{
    /** 快照创建时间戳 */
    private final Instant timestamp = Instant.now();

    /** 是否为空快照。*/
    public abstract  boolean isEmpty();

    /** 比较前后两次快照，检查指标是否被外部重置。*/
    public abstract boolean isReset(@NotNull T current);

    /** 计算前后两次快照的时间差（单位：毫秒）。*/
    public long
    getTimeDiffMills(@NotNull T current)
    {
        return
        current.getTimestamp().toEpochMilli() - this.timestamp.toEpochMilli();
    }
}