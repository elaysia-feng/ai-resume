package com.elias.common.config;

public enum GatewayHeader {
    USER_ID("X-User-Id"),
    SERVICE_TOKEN("X-Gateway-Service-Token");

    private String name;

    GatewayHeader(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
