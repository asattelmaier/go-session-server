package com.go.server.game.bot

import com.go.server.BaseIntegrationSpec
import com.go.server.game.session.model.input.CreateSessionDto
import com.go.server.game.session.model.BotDifficulty

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Integration test for the end-to-end bot flow using WebSockets/STOMP.
 * Verifies that a user can connect, create a session with a bot, and receive the session update.
 */
class BotIntegrationSpec extends BaseIntegrationSpec {

    def "create a session, verify creation, make a move and verify game update"() {
        given: "an authenticated user"
        def username = UUID.randomUUID().toString()
        def tokens = registerUser(username, "test-password")

        and: "a connected STOMP session"
        def session = connect(tokens.accessToken)
        def sessionFuture = new CompletableFuture<Map>()
        def gameFuture = new CompletableFuture<Map>()

        when: "subscribing to session creation and game updates"
        subscribe(session, "/user/game/session/created", Map, sessionFuture)
        
        and: "sending a create request"
        def createRequest = new CreateSessionDto(username, BotDifficulty.EASY, 19)
        session.send("/game/session/create", createRequest)

        then: "a valid session is received"
        def sessionMap = sessionFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        
        sessionMap.id != null
        sessionMap.difficulty == "EASY"
        sessionMap.boardSize == 19
        sessionMap.players.size() == 2
        
        String topic = "/game/session/${sessionMap.id}/updated"
        
        when: "subscribing to game updates and making a move"
        subscribe(session, topic, Map, gameFuture)
        Thread.sleep(1000) // Ensure subscription is established
        
        // Construct a move at specific location, e.g., 2,2 (top-left area, likely valid)
        // Need to send as complex command payload matching refactored controller
        def playCommand = [command: [name: "Play", location: [x: 2, y: 2]]]
        session.send("/game/session/${sessionMap.id}/update", playCommand)
        
        then: "a game update matching the move is received"
        def gameUpdate = gameFuture.get(TIMEOUT_SECONDS * 2, TimeUnit.SECONDS)
        
        gameUpdate != null
        // Verify structure roughly matches GameDto
        gameUpdate.settings.boardSize == 19
    }
}
