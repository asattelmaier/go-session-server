package com.go.server.configuration;

public final class WebSocketConfigConstants {
    public static final String ENDPOINT = "/";
    public static final String ALLOWED_ORIGINS = "*";
    public static final String DESTINATION_PREFIX = "/game/session";
    public static final String QUEUE_PREFIX = "/queue";
    public static final String TOPIC_PREFIX = "/topic";
    public static final String USER_DESTINATION_PREFIX = "/user";
    public static final String ERROR_QUEUE = "/queue/errors";

    private WebSocketConfigConstants() {
        // Prevent instantiation
    }
}
