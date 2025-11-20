package com.jesse.indicator_receiver.utils.exception;

/** 检查到非法 IPv4 格式时，抛出本异常。*/
public class InvalidIPv4Exception extends RuntimeException
{
    public InvalidIPv4Exception(String message) {
        super(message);
    }
}
