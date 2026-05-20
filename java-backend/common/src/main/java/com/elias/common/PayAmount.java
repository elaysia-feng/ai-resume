package com.elias.common;

public record PayAmount(Long amount) {
    // 2块钱
    public static final PayAmount START = new PayAmount(200L);
    // 5块钱
    public static final PayAmount PLUS = new PayAmount(500L);
    // 10块钱
    public static final PayAmount MAX = new PayAmount(1000L);
}