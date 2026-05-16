package com.airesumeforge.config;

public enum GatewayHeader {
    USER_ID("X-User-Id");

    private String name;

    GatewayHeader(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}