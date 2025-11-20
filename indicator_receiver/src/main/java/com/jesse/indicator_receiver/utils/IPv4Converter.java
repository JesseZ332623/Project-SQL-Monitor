package com.jesse.indicator_receiver.utils;

import com.jesse.indicator_receiver.utils.exception.InvalidIPv4Exception;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/** IPv4 转换器。*/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class IPv4Converter
{
    /** 严格匹配 IPV4 的正则表达式 Pattern。*/
    private final static
    Pattern IPV4_PATTERN
        = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)){3}$"
        );

    /** 检查一个 IPv4 字符串是否合法 */
    private static boolean isValidIpv4(@NotNull String ip) {
        return IPV4_PATTERN.matcher(ip).matches();
    }

    /** IPV4 String -> UINT32 */
    public static Long ipToLong(@NotNull String ip)
    {
        // 如果配置中填了 localhost，
        // 我们便直接调用 ipconfig 拿到本机 IP
        if (ip.equals("localhost")) {
            ip = LocalIPGetter.getLocalIP();
        }

        if (!isValidIpv4(ip))
        {
            throw new
            InvalidIPv4Exception(
                String.format("Invalid IPv4 address: %s", ip)
            );
        }

        String[] parts = ip.split("\\.");

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