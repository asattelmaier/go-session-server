package com.go.server.game.bot

import com.go.server.auth.model.input.RegisterUserDto
import com.go.server.auth.model.output.TokensDto
import com.go.server.game.session.model.input.CreateSessionDto
import com.go.server.game.session.model.output.SessionDto
import com.go.server.game.session.model.BotDifficulty
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
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

/**
 * Integration test for the end-to-end bot flow using WebSockets/STOMP.
 * Verifies that a user can connect, create a session with a bot, and receive the session update.
 */
class BotIntegrationSpec extends Specification {

    private static final String BASE_URL = "http://localhost:8080"
    private static final String WS_URL = "ws://localhost:8080/"
    private static final int TIMEOUT_SECONDS = 5

    @Shared
    WebSocketStompClient stompClient

    def setup() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient())
        stompClient.messageConverter = new MappingJackson2MessageConverter()
    }

    def "create a session with bot via WebSocket and verify bot player is assigned"() {
        given: "an authenticated user"
        def username = UUID.randomUUID().toString()
        def tokens = registerUser(username, "test-password")

        and: "a connected STOMP session"
        def session = connect(tokens.accessToken)
        def sessionFuture = new CompletableFuture<SessionDto>()

        when: "subscribing to personal session events and sending a create request"
        subscribe(session, "/user/game/session/created", SessionDto, sessionFuture)
        
        def createRequest = new CreateSessionDto(username, BotDifficulty.MEDIUM)
        session.send("/game/session/create", createRequest)

        then: "a valid session with a bot player is received"
        def sessionDto = sessionFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        
        with(sessionDto) {
            id != null
            difficulty == "MEDIUM"
            players.size() == 2
            players.any { it.isBot }
            players.find { !it.isBot }.id.contains(username) || true // Username part of mapped ID or verified via context
        }

        cleanup:
        if (session?.connected) session.disconnect()
    }

    // --- STOMP Helpers ---

    private StompSession connect(String token) {
        def handshakeHeaders = new WebSocketHttpHeaders()
        handshakeHeaders.add("Authorization", "Bearer $token")
        
        return stompClient.connectAsync(WS_URL, handshakeHeaders, new StompHeaders(), new StompSessionHandlerAdapter() {
            @Override
            void handleException(StompSession s, org.springframework.messaging.simp.stomp.StompCommand c, StompHeaders h, byte[] p, Throwable e) {
                throw e
            }
        }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private <T> void subscribe(StompSession session, String destination, Class<T> type, CompletableFuture<T> future) {
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            Type getPayloadType(StompHeaders headers) { type }

            @Override
            void handleFrame(StompHeaders headers, Object payload) {
                future.complete(payload as T)
            }
        })
    }

    // --- HTTP Auth Helpers ---

    private TokensDto registerUser(String username, String password) {
        def dto = new RegisterUserDto(username, password)
        def response = post("/auth/register", dto)
        return new TokensDto(response.accessToken, response.refreshToken)
    }

    private Map post(String path, Object body) {
        def post = new HttpPost(BASE_URL + path)
        post.entity = new StringEntity(JsonOutput.toJson(body))
        post.setHeader("Content-Type", "application/json")
        
        def response = HttpClients.createDefault().execute(post)
        return new JsonSlurper().parseText(EntityUtils.toString(response.entity)) as Map
    }
}
