package com.go.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.go.server.auth.model.output.TokensDto
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.messaging.converter.ByteArrayMessageConverter
import org.springframework.messaging.converter.CompositeMessageConverter
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import spock.lang.Shared
import spock.lang.Specification

import java.lang.reflect.Type
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = [
    "FIRESTORE_EMULATOR_ENABLED=true",
    "FIRESTORE_EMULATOR_HOST_PORT=localhost:9000",
    "FIRESTORE_EMULATOR_PROJECT_ID=local-project",
    "SECURITY_JWT_SECRET_KEY=8Gr0MjVACbywAYACtN6o0wl4FKIG4s3F2iOGwMA1BQLKXh5ScLIuun0PgZnZ94vm",
    "SECURITY_JWT_ACCESS_TOKEN_EXPIRATION=86400000",
    "SECURITY_JWT_REFRESH_TOKEN_EXPIRATION=604800000",
    "SECURITY_GUEST_PASSWORD=guest-password",
    "GAME_CLIENT_SOCKET_HOST=localhost",
    "GAME_CLIENT_SOCKET_PORT=8000",
    "gnugo.host=localhost",
    "gnugo.port=8001"
])
abstract class BaseIntegrationSpec extends Specification {

    protected static final String BASE_URL = "http://localhost:8080"
    protected static final String WS_URL = "ws://localhost:8080/"
    protected static final int TIMEOUT_SECONDS = 30

    @Shared
    WebSocketStompClient stompClient

    def setup() {
        def container = jakarta.websocket.ContainerProvider.getWebSocketContainer()
        container.setDefaultMaxTextMessageBufferSize(1024 * 1024)
        container.setDefaultMaxBinaryMessageBufferSize(1024 * 1024)
        stompClient = new WebSocketStompClient(new StandardWebSocketClient(container))
        stompClient.setInboundMessageSizeLimit(1024 * 1024)
        stompClient.messageConverter = new CompositeMessageConverter([
            new ByteArrayMessageConverter(),
            new MappingJackson2MessageConverter()
        ])
    }

    protected StompSession connect(String token) {
        def handshakeHeaders = new WebSocketHttpHeaders()
        handshakeHeaders.add("Authorization", "Bearer $token")
        
        return stompClient.connectAsync(WS_URL, handshakeHeaders, new StompHeaders(), new StompSessionHandlerAdapter() {
            @Override
            void handleException(StompSession s, org.springframework.messaging.simp.stomp.StompCommand c, StompHeaders h, byte[] p, Throwable e) {
                e.printStackTrace()
            }
        }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    protected <T> void subscribe(StompSession session, String destination, Class<T> type, CompletableFuture<T> future) {
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            Type getPayloadType(StompHeaders headers) { type }

            @Override
            void handleFrame(StompHeaders headers, Object payload) {
                future.complete(payload as T)
            }
        })
    }

    protected TokensDto registerUser(String username, String password) {
        def dto = [username: username, password: password]
        def response = post("/auth/register", dto)
        return new TokensDto(response.accessToken, response.refreshToken)
    }

    protected Map post(String path, Object body) {
        def fullUrl = path.startsWith("http") ? path : BASE_URL + path
        def post = new HttpPost(fullUrl)
        post.entity = new StringEntity(JsonOutput.toJson(body))
        post.setHeader("Content-Type", "application/json")
        
        def response = HttpClients.createDefault().execute(post)
        if (response.entity == null) return [:]
        def content = EntityUtils.toString(response.entity)
        if (content == null || content.isEmpty()) return [:]
        return new JsonSlurper().parseText(content) as Map
    }
}
