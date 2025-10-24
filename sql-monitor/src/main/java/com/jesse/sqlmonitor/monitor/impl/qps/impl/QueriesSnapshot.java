package com.jesse.sqlmonitor.monitor.impl.qps.impl;

import com.jesse.sqlmonitor.monitor.snapshot.SnapshotBase;
import lombok.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** 数据库总查询数快照。*/
@Getter
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class QueriesSnapshot extends SnapshotBase<QueriesSnapshot>
{
    /** 上一次获取的总查询数 */
    private final Long queries;

    @Contract(" -> new")
    public static @NotNull QueriesSnapshot empty() {
        return new QueriesSnapshot(0L);
    }

    @Contract("_ -> new")
    public static @NotNull QueriesSnapshot
    of(Long queries) {
        return new QueriesSnapshot(queries);
    }

    /** 是否为空快照。*/
    @Override
    public boolean isEmpty() {
        return this.queries == 0L;
    }

    @Override
    public boolean
    isReset(@NotNull QueriesSnapshot current) {
        return current.queries < this.queries;
    }
}