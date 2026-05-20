package com.elias.common;

public enum PayStatus {

//    （PENDING/PAID/EXPIRED/CANCELLED）
    PENDING("PENDING"),

    PAID("PAID"),

    EXPIRED("EXPIRED"),

    CANCELLED("CANCELLED");


    private final String status;

    PayStatus(String status) {
            this.status = status;
        }

    /** 返回写入数据库 / 传输给 Python 的阶段码字符串。 */
    public String getStatus() {
        return status;
    }
}
