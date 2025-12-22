package com.go.server.game.bot

import com.go.server.BaseIntegrationSpec
import com.go.server.game.session.model.input.CreateSessionDto
import com.go.server.game.session.model.BotDifficulty
import spock.lang.Unroll

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class BoardSizeIntegrationSpec extends BaseIntegrationSpec {

    @Unroll
    def "should create a session with board size #requestedSize and verify initial state"() {
        given: "an authenticated user"
        def username = UUID.randomUUID().toString()
        def tokens = registerUser(username, "test-password")

        and: "a connected STOMP session"
        def stompSession = connect(tokens.accessToken)
        def sessionFuture = new CompletableFuture<Map>()
        def gameFuture = new CompletableFuture<Map>()

        when: "subscribing to session creation"
        subscribe(stompSession, "/user/game/session/created", Map, sessionFuture)
        
        and: "sending a create request with size #requestedSize"
        def createRequest = new CreateSessionDto(username, BotDifficulty.EASY, requestedSize)
        stompSession.send("/game/session/create", createRequest)

        then: "a valid session with size #requestedSize is received"
        def sessionMap = sessionFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        sessionMap.boardSize == requestedSize
        
        when: "subscribing to game updates and requesting initial state"
        String topic = "/game/session/${sessionMap.id}/updated"
        subscribe(stompSession, topic, Map, gameFuture)
        Thread.sleep(2000) // Ensure subscription is established

        // Send a Create command to trigger initial GameDto broadcast
        def initCommand = [command: [name: "Create"]]
        stompSession.send("/game/session/${sessionMap.id}/update", initCommand)

        then: "an initial GameDto matching the size is received"
        def gameUpdate = gameFuture.get(TIMEOUT_SECONDS * 2, TimeUnit.SECONDS)
        
        gameUpdate.settings.boardSize == requestedSize
        
        // Check if there are NO stones (positions might be nested lists)
        def currentBoard = gameUpdate.positions[0]
        
        currentBoard.size() == requestedSize
        currentBoard[0].size() == requestedSize
        
        // All intersections should be EMPTY
        currentBoard.every { row -> row.every { intersection -> 
             intersection.state == "Empty" 
        } }

        cleanup:
        stompSession?.disconnect()

        where:
        requestedSize << [9, 13, 19]
    }
}
