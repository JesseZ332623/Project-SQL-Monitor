package com.jesse.sqlmonitor.monitor.impl.network_traffic.impl;

import com.jesse.sqlmonitor.monitor.constants.SizeUnit;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.jesse.sqlmonitor.monitor.constants.MonitorConstants.MIN_TIME_DIFF_MS;

/** 数据库服务器网络流量计算器。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class TrafficRateCalculator
{
    /** 1 KB = 1024 Bytes */
    private static final
    BigDecimal BYTES_PER_KB = BigDecimal.valueOf(1024);

    /** 通过两张流量快照，计算数据库网络流量。*/
    public TrafficRate
    calculateRate(
        TrafficStateSnapshot previous,
        TrafficStateSnapshot current,
        SizeUnit unit
    )
    {
        // 计算两次快照的时间差
        long timeDiff = previous.getTimeDiffMills(current);

        // 如果时间差小于 MIN_TIME_DIFF_MS 毫秒，直接返回空结果
        if (timeDiff < MIN_TIME_DIFF_MS) {
            return TrafficRate.zero(unit);
        }

        // 若检查到指标被重置，返回重置结果
        if (previous.isReset(current)) {
            return TrafficRate.reset(unit);
        }

        BigDecimal sentRate
            = calculateSingleRate(
                BigDecimal.valueOf(current.getTotalBytesSent() - previous.getTotalBytesSent()),
                timeDiff, unit
        );

        BigDecimal receiveRate
            = calculateSingleRate(
                BigDecimal.valueOf(current.getTotalBytesReceive() - previous.getTotalBytesReceive()),
                timeDiff, unit
        );

        return
        TrafficRate.of(sentRate, receiveRate, timeDiff, unit, false);
    }

    /**
     * 计算某一个网络流量（结果保留 10 位小数且四舍五入）。
     * <pre>公式：bytesDiff / (timeDiffMs * 1000) / (1024 ^ exponent)</pre>
     *
     * @param bytesDiff  通过两次快照计算的总字节差
     * @param timeDiffMs 两次快照的时间差
     * @param unit       流量单位 {@link SizeUnit}
     */
    private BigDecimal
    calculateSingleRate(
        BigDecimal bytesDiff,
        long timeDiffMs, SizeUnit unit
    )
    {
        BigDecimal timeDiffSeconds
            = BigDecimal.valueOf(timeDiffMs)
                .divide(BigDecimal.valueOf(1000L), 10, RoundingMode.HALF_UP);

        return
        bytesDiff.divide(timeDiffSeconds, 10, RoundingMode.HALF_UP)
                 .divide(BYTES_PER_KB.pow(unit.getExponent()), 10, RoundingMode.HALF_UP);
    }

    @Value(staticConstructor = "of")
    public static class TrafficRate
    {
        /** 服务器每秒发送速率 */
        BigDecimal sentKBytesPerSec;

        /** 服务器每秒的接收速率 */
        BigDecimal receiveKBytesPerSec;

        /** 两次查询的时间间隔（单位：毫秒）*/
        long queryDiff;

        /** 本次流量统计的计量单位 */
        SizeUnit sizeUnit;

        /** 指标是否被外部重置？*/
        boolean resetDetected;

        @Contract("_ -> new")
        public static @NotNull
        TrafficRate zero(SizeUnit unit)
        {
            return new
            TrafficRate(
                BigDecimal.ZERO, BigDecimal.ZERO,
                0L, unit, false
            );
        }

        @Contract("_ -> new")
        public static @NotNull
        TrafficRate reset(SizeUnit unit)
        {
            return new
            TrafficRate(
                BigDecimal.ZERO, BigDecimal.ZERO,
                0L, unit, true
            );
        }
    }
}
