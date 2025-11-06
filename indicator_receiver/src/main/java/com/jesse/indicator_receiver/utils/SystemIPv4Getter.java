package com.jesse.indicator_receiver.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** 本机 IP 地址获取器。*/
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class SystemIPv4Getter
{
    private static final String FULLBACK_IP = "127.0.0.1";

    public static @NotNull String
    getLocalHost()
    {
        try
        {
            return
            InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException unknownHostException)
        {
            log.warn("{}", unknownHostException.getMessage());
            return FULLBACK_IP;
        }
    }
}