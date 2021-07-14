package com.go.server.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

import java.net.URI;

@Configuration
@EnableWebSocketMessageBroker
public class SessionConfig implements WebSocketMessageBrokerConfigurer {
    private final static int MESSAGE_BUFFER_SIZE = 1000 * 1024;
    public final static String DESTINATION_PREFIX = "/game/session";
    private final static String ALLOWED_ORIGIN = "*";
    private final static String ENDPOINT = "/";
    @Value("${game.client.socket.url}")
    private String gameClientSocketUrl;

    @Override
    public void configureWebSocketTransport(final WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(SessionConfig.MESSAGE_BUFFER_SIZE);
    }

    @Override
    public void registerStompEndpoints(final StompEndpointRegistry registry) {
        registry.addEndpoint(SessionConfig.ENDPOINT).setAllowedOrigins(SessionConfig.ALLOWED_ORIGIN);
    }

    @Override
    public void configureMessageBroker(final MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(SessionConfig.DESTINATION_PREFIX);
        registry.setApplicationDestinationPrefixes(SessionConfig.DESTINATION_PREFIX);
    }

    public URI getGameClientSocketUrl() {
        return URI.create(gameClientSocketUrl);
    }
}
