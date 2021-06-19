package com.go.server.game.message.handler;

import com.go.server.game.message.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageHandler {
    private final Logger logger = LoggerFactory.getLogger(MessageHandler.class);
    private final SimpMessagingTemplate messagingTemplate;

    public MessageHandler(final SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void send(final Message message) {
        logger.info("Message send to " + message.getDestination() + ", with payload: " + message.getPayload());
        this.messagingTemplate.convertAndSend(message.getDestination(), message.getPayload());
    }
}
