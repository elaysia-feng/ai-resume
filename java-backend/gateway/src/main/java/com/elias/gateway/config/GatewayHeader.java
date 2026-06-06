package com.elias.gateway.config;

public enum GatewayHeader {
    USER_ID("X-User-Id"),
    SERVICE_TOKEN("X-Gateway-Service-Token");

    private final String name;

    GatewayHeader(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
