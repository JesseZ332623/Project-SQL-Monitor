package com.jesse.indicator_receiver.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static com.jesse.indicator_receiver.utils.SystemIPv4Getter.getLocalIPByIPConfig;

/** IPv4 转换器。*/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class IPv4Converter
{
    /** IPV4 String -> UINT32 */
    public static Long ipToLong(@NotNull String ip)
    {
        // 如果配置中填了 localhost，
        // 我们便直接调用 ipconfig 拿到本机 IP（Linux 不适用）
        if (ip.equals("localhost")) {
            ip = getLocalIPByIPConfig();
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