package com.go.server.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

import java.net.URI;

import static com.go.server.configuration.WebSocketConfigConstants.*;

@Configuration
@EnableWebSocketMessageBroker
public class SessionConfig implements WebSocketMessageBrokerConfigurer {
    private final static int MESSAGE_BUFFER_SIZE = 1000 * 1024;

    @Override
    public void configureWebSocketTransport(final WebSocketTransportRegistration registry) {
         registry.setMessageSizeLimit(MESSAGE_BUFFER_SIZE);
     }
 
     @Override
     public void registerStompEndpoints(final StompEndpointRegistry registry) {
         registry.addEndpoint(ENDPOINT).setAllowedOrigins(ALLOWED_ORIGINS);
     }
 
     @Override
    public void configureMessageBroker(final MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(DESTINATION_PREFIX, QUEUE_PREFIX, TOPIC_PREFIX);
        registry.setApplicationDestinationPrefixes(DESTINATION_PREFIX);
        registry.setUserDestinationPrefix(USER_DESTINATION_PREFIX);
    }
}
