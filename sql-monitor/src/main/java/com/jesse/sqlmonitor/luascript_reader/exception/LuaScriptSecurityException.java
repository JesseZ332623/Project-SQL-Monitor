package com.jesse.sqlmonitor.luascript_reader.exception;

import lombok.Getter;

import java.nio.file.Path;

/** 非法 Lua 脚本路径异常。*/
public class LuaScriptSecurityException extends RuntimeException
{
    @Getter
    private final Path illegalPath; // 记录非法路径。

    public LuaScriptSecurityException(String message, Path illegalPath)
    {
        super(message);
        this.illegalPath = illegalPath;
    }
}