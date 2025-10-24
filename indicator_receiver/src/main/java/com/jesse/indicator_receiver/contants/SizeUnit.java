package com.jesse.indicator_receiver.contants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 不同容量大小的指数枚举。*/
@AllArgsConstructor
public enum SizeUnit
{
    B(0),
    KB(1),
    MB(2),
    GB(3);

    @Getter
    private final int exponent;
}
