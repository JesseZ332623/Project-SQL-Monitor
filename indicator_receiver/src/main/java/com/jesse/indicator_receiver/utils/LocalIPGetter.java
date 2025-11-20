package com.jesse.indicator_receiver.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

/** 本机 IP 地址获取器。*/
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class LocalIPGetter
{
    /** 判断是否为优选IPv4地址 */
    private static boolean
    isPreferredIPv4(@NotNull String ip)
    {
        // 优选：非 Docker、非常见内网段的IP
        return !ip.startsWith("172.17.") &&
            !ip.startsWith("172.18.")    &&
            !ip.startsWith("192.168.")   &&
            !ip.startsWith("10.")        &&
            !ip.startsWith("169.254."); // 链路本地地址
    }

    /** 获取本机 IP 字符串，可在复杂网络环境中使用。*/
    public static
    @NotNull String getLocalIP()
    {
        List<String> IPv4Candidates = new LinkedList<>();
        List<String> IPv6Candidates = new LinkedList<>();

        try
        {
            // 获取本机所有网络接口的抽象实例
            Enumeration<NetworkInterface> interfaces
                = NetworkInterface.getNetworkInterfaces();

            // 若本机存在多块网卡（不论是虚拟的还是物理的）
            while (interfaces.hasMoreElements())
            {
                NetworkInterface networkInterface
                    = interfaces.nextElement();

                // 跳过回环和未启用的接口
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                // 获取这块网卡分配的所有地址信息
                Enumeration<InetAddress> addresses
                    = networkInterface.getInetAddresses();

                // 如果分配了多个地址，需要进一步筛选
                while (addresses.hasMoreElements())
                {
                    InetAddress address = addresses.nextElement();

                    // 跳过回环地址
                    if (!address.isLoopbackAddress())
                    {
                        String ip = address.getHostAddress();

                        if (address instanceof Inet4Address)    // 优先使用 IPv4
                        {
                            // 分类收集
                            if (isPreferredIPv4(ip)) {
                                // 优选 IP 要放在第一个
                                IPv4Candidates.addFirst(ip);
                            }
                            else {
                                IPv4Candidates.add(ip);
                            }
                        }
                        else {
                            IPv6Candidates.add(ip);
                        }
                    }
                }
            }

            if (!IPv4Candidates.isEmpty()) {
                return IPv4Candidates.getFirst();
            }

            if (!IPv6Candidates.isEmpty()) {
                return IPv6Candidates.getFirst();
            }

            // 如果没有候选 IP，尝试直接调用
            return
            InetAddress.getLocalHost().getHostAddress();
        }
        catch (Exception exception)
        {
            log.error(
                "Failed to get local IP! Caused by: {}",
                exception.getMessage(), exception
            );

            // 最后的挣扎。。。
            return "127.0.0.1";
        }
    }
}