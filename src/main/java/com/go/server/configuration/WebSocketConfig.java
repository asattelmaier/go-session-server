package com.go.server.configuration;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final static int MESSAGE_BUFFER_SIZE = 1000 * 1024;

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        final ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();

        container.setMaxTextMessageBufferSize(WebSocketConfig.MESSAGE_BUFFER_SIZE);
        container.setMaxBinaryMessageBufferSize(WebSocketConfig.MESSAGE_BUFFER_SIZE);

        return container;
    }

    @Override
    public void registerWebSocketHandlers(final @NotNull WebSocketHandlerRegistry webSocketHandlerRegistry) {
    }
}
