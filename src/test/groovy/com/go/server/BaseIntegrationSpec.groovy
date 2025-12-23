package com.go.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.go.server.auth.model.output.TokensDto
import com.go.server.game.session.model.BotDifficulty
import com.go.server.game.session.model.input.CreateSessionDto
import com.go.server.game.session.model.output.SessionDto
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.http.client.methods.HttpGet
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
import jakarta.websocket.ContainerProvider
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.messaging.simp.stomp.StompCommand
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
    "gnugo.port=8001",
    "spring.main.allow-bean-definition-overriding=true"
])

abstract class BaseIntegrationSpec extends Specification {

    @LocalServerPort
    protected int port

    protected String getBaseUrl() {
        return "http://localhost:${port}"
    }

    protected String getWsUrl() {
        return "ws://localhost:${port}/"
    }

    protected static final int TIMEOUT_SECONDS = 30

    @Shared
    WebSocketStompClient stompClient

    def setup() {
        def container = ContainerProvider.getWebSocketContainer()
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
        
        return stompClient.connectAsync(getWsUrl(), handshakeHeaders, new StompHeaders(), new StompSessionHandlerAdapter() {
            @Override
            void handleException(StompSession s, StompCommand c, StompHeaders h, byte[] p, Throwable e) {
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

    protected TestUser registerUser(String username, String password) {
        def dto = [username: username, password: password]
        def response = postRaw("/auth/register", dto)
        if (response.status != 200) return new TestUser(username, password, null)
        return new TestUser(username, password, new TokensDto(response.body.accessToken, response.body.refreshToken))
    }

    protected Map post(String path, Object body) {
        def result = postRaw(path, body)
        return result.body instanceof Map ? result.body : [:]
    }

    protected Map get(String path, Map<String, String> headers = [:]) {
        def fullUrl = path.startsWith("http") ? path : getBaseUrl() + path
        def get = new HttpGet(fullUrl)
        headers.each { k, v -> get.addHeader(k, v) }
        
        def response = HttpClients.createDefault().execute(get)
        def result = [status: response.statusLine.statusCode]
        
        if (response.entity != null) {
             def content = EntityUtils.toString(response.entity)
             if (content != null && !content.isEmpty()) {
                 try {
                     result.body = new JsonSlurper().parseText(content)
                 } catch (Exception e) {
                     result.body = content
                 }
             } else {
                 result.body = [:]
             }
         } else {
             result.body = [:]
         }
         return result
    }

    protected List getPendingSessions(TestUser user) {
        def response = get("/game/session/pending", ["Authorization": "Bearer " + user.tokens.accessToken])
        return response.status == 200 ? response.body : []
    }

    protected Map postRaw(String path, Object body) {
        def fullUrl = path.startsWith("http") ? path : getBaseUrl() + path
        def post = new HttpPost(fullUrl)
        post.entity = new StringEntity(JsonOutput.toJson(body))
        post.setHeader("Content-Type", "application/json")
        
        def response = HttpClients.createDefault().execute(post)
        def result = [status: response.statusLine.statusCode]
        
        if (response.entity != null) {
            def content = EntityUtils.toString(response.entity)
            if (content != null && !content.isEmpty()) {
                try {
                    result.body = new JsonSlurper().parseText(content)
                } catch (Exception e) {
                    result.body = content
                }
            } else {
                result.body = [:]
            }
        } else {
            result.body = [:]
        }
        return result
    }
    protected String createUsername() {
        return UUID.randomUUID().toString()
    }

    protected TestUser registerUser(String username) {
        def password = "guest-password"
        return registerUser(username, password)
    }

    protected StompSession connect(TestUser user) {
        user.session = connect(user.tokens.accessToken)
        return user.session
    }

    protected <T> T request(StompSession session, String sendTo, Object payload, String subscribeTo, Class<T> responseType) {
        def future = new CompletableFuture<T>()
        subscribe(session, subscribeTo, responseType, future)
        session.send(sendTo, payload)
        return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    protected Map createGame(TestUser user, int boardSize = 19, BotDifficulty difficulty = null) {
        def dto = new CreateSessionDto()
        dto.playerId = user.username
        dto.boardSize = boardSize
        dto.difficulty = difficulty
        
        return request(user.session, "/game/session/create", dto, "/user/game/session/created", Map)
    }

    protected def joinGame(TestUser user, String gameId) {
        return request(user.session, "/game/session/${gameId}/join", [:], "/user/game/session/joined", SessionDto)
    }

    protected def terminateGame(TestUser user, String gameId) {
        return request(user.session, "/game/session/${gameId}/terminate", [:], "/game/session/${gameId}/terminated", SessionDto)
    }

    // Assertion Helpers
    protected boolean isGameCreated(def game) {
        return game.id != null
    }

    protected boolean hasPlayers(def game, int count) {
        return game.players.size() == count
    }

    protected boolean hasPlayer(def game, TestUser user) {
        return game.players.find { it.id == user.username } != null
    }

    protected boolean hasBot(def game, BotDifficulty difficulty) {
        return game.players.any { it.isBot } && game.difficulty == difficulty.name()
    }

    protected boolean isGameInList(def game, List list) {
        return list.find { it.id == game.id } != null
    }

    protected boolean isGameNotInList(def game, List list) {
        return list.find { it.id == game.id } == null
    }


    protected Map playMove(TestUser user, UUID sessionId, int x, int y) {
        def updateFuture = new CompletableFuture<Map>()
        subscribe(user.session, "/game/session/${sessionId}/updated", Map, updateFuture)
        
        user.session.send("/game/session/${sessionId}/update", 
            [command: [name: "Play", location: [x: x, y: y]]]
        )
        return updateFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    protected Map passTurn(TestUser user, UUID sessionId) {
        def updateFuture = new CompletableFuture<Map>()
        subscribe(user.session, "/game/session/${sessionId}/updated", Map, updateFuture)
        
        user.session.send("/game/session/${sessionId}/update", [command: [name: "Pass"]])
        return updateFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    protected Map waitForEndGame(TestUser user, UUID sessionId) {
        def endGameFuture = new CompletableFuture<Map>()
        subscribe(user.session, "/game/session/${sessionId}/endgame", Map, endGameFuture)
        return endGameFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    protected List waitForSessionInPendingList(TestUser user, Map game) {
        return waitForSessionInPendingList(user, UUID.fromString(game.id))
    }

    protected List waitForSessionInPendingList(TestUser user, UUID sessionId) {
        def pendingSessions = []
        for (int i = 0; i < 20; i++) {
            pendingSessions = getPendingSessions(user)
            if (pendingSessions.any { it.id == sessionId.toString() }) {
                return pendingSessions
            }
            Thread.sleep(200)
        }
        return pendingSessions
    }

    protected TestUser login(String username, String password) {
        def dto = [username: username, password: password]
        def response = postRaw("/auth/authenticate", dto)
        if (response.status != 200) return null
        return new TestUser(username, password, new TokensDto(response.body.accessToken, response.body.refreshToken))
    }

    protected TokensDto refresh(String refreshToken) {
        def response = postRaw("/auth/refresh-token", [refreshToken: refreshToken])
        if (response.status != 200) return null
        return new TokensDto(response.body.accessToken, response.body.refreshToken)
    }

    protected Map getProfile(TestUser user) {
        def response = get("/user", ["Authorization": "Bearer " + user.tokens.accessToken])
        return response.status == 200 ? response.body : null
    }

    protected void logout(TestUser user) {
        def fullUrl = getBaseUrl() + "/auth/logout"
        def post = new HttpPost(fullUrl)
        post.setHeader("Authorization", "Bearer " + user.tokens.accessToken)
        HttpClients.createDefault().execute(post)
    }

    protected void waitForTokenCycle() {
        Thread.sleep(1100)
    }

    class TestUser {
        String username
        String password
        TokensDto tokens
        StompSession session
        
        TestUser(String username, String password, TokensDto tokens) {
            this.username = username
            this.password = password
            this.tokens = tokens
        }
        
        boolean isRegistered() {
            return tokens != null && tokens.accessToken != null && tokens.refreshToken != null
        }
    }
}
