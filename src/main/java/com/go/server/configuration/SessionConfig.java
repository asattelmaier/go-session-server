package com.go.server.configuration;

import com.go.server.auth.jwt.JwtService;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.*;

import java.net.URI;

import static com.go.server.configuration.WebSocketConfigConstants.*;

@Configuration
@EnableWebSocketMessageBroker
public class SessionConfig implements WebSocketMessageBrokerConfigurer {
    private final static int MESSAGE_BUFFER_SIZE = 1000 * 1024;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public SessionConfig(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

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

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    authenticateUser(accessor);
                }

                return message;
            }
        });
    }

    private void authenticateUser(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        String token = authHeader.substring(7);
        jwtService.extractUsername(token).ifPresent(username -> {
            if (jwtService.isTokenValid(token, username)) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                accessor.setUser(authToken);
            }
        });
    }
}
