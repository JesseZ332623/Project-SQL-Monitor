package com.jesse.indicator_receiver.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 本机 IP 地址获取器（仅限 Windows）。*/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class SystemIPv4Getter
{
    private static final Pattern IP_PATTERN
        = Pattern.compile("IPv4 地址[^:]*: ([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})");

    /** 自动专用 IP 寻址地址段。*/
    private static final
    String APIPA_PREFIX = "169.254";

    /** 回环地址。*/
    private static final
    String LOOPBACK = "127.0.0.1";

    public static @NotNull String getLocalIPByIPConfig()
    {
        Process process       = null;
        BufferedReader reader = null;
        try
        {
            ProcessBuilder processBuilder = new ProcessBuilder("ipconfig");
            process = processBuilder.start();

            reader
                = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "GBK")
                );

            String line;

            while ((line = reader.readLine()) != null)
            {
                Matcher matcher = IP_PATTERN.matcher(line);

                if (matcher.find())
                {
                    String ip = matcher.group(1);
                    if (!ip.startsWith(APIPA_PREFIX) && !ip.equals(LOOPBACK)) {
                        return ip;
                    }
                }
            }
        }
        catch (Exception exception) {
            exception.printStackTrace(System.err);
        }
        finally
        {
            try
            {
                if (Objects.nonNull(reader)) {
                    reader.close();
                }
                if (Objects.nonNull(process)) {
                    process.waitFor();
                }
            }
            catch (Exception exception)
            {
                exception.printStackTrace(System.err);
            }
        }

        return "127.0.0.1";
    }
}