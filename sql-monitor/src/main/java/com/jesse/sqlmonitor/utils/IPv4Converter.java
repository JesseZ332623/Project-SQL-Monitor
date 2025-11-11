package com.jesse.sqlmonitor.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static com.jesse.sqlmonitor.utils.LocalIPGetter.getLocalIP;


/** IPv4 <=> UINT32 转换器。*/
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class IPv4Converter
{
    /** IPV4 String -> UINT32 */
    public static Long ipToLong(@NotNull String ip)
    {
        // 如果配置中填了 localhost，
        // 我们便直接调用 ipconfig 拿到本机 IP（Linux 不适用）
        if (ip.equals("localhost")) {
            ip = getLocalIP();
        }

        String[] parts = ip.split("\\.");

        if (parts.length != 4)
        {
            throw new
            IllegalArgumentException(
                String.format("Invalid IPv4 address: %s", ip)
            );
        }

        long result = 0;

        for (int i = 0; i < 4; i++)
        {
            int octet = Integer.parseInt(parts[i]);
            result    = (result << 8) | octet;
        }

        return result;
    }

    /** UINT32 -> IPV4 String */
    @Contract(pure = true)
    public static @NotNull String longToIp(long ip)
    {
        return
            String.format(
                "%d.%d.%d.%d",
                (ip >> 24) & 0xFF,
                (ip >> 16) & 0xFF,
                (ip >> 8) & 0xFF,
                ip & 0xFF
            );
    }
}