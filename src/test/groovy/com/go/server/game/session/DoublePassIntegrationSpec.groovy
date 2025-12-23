package com.go.server.game.session

import com.go.server.BaseIntegrationSpec
import com.go.server.game.session.model.input.CreateSessionDto
import com.go.server.game.session.model.output.SessionDto

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class DoublePassIntegrationSpec extends BaseIntegrationSpec {

    def "game should end when both players pass"() {
        given: "two authenticated users"
        def p1Id = UUID.randomUUID().toString()
        def p2Id = UUID.randomUUID().toString()
        def p1Token = registerUser(p1Id, "password").accessToken
        def p2Token = registerUser(p2Id, "password").accessToken

        and: "connected sessions"
        def p1Session = connect(p1Token)
        def p2Session = connect(p2Token)
        
        def sessionFuture = new CompletableFuture<Map>()
        
        def gameUpdateFuture1 = new CompletableFuture<Map>()
        def gameUpdateFuture2 = new CompletableFuture<Map>()

        when: "player 1 creates a game"
        subscribe(p1Session, "/user/game/session/created", Map, sessionFuture)
        p1Session.send("/game/session/create", new CreateSessionDto(playerId: p1Id, boardSize: 9))
        
        then: "session is created"
        def sessionMap = sessionFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        def sessionId = sessionMap.id
        
        when: "player 2 joins the game"
        def joinFuture = new CompletableFuture<SessionDto>()
        subscribe(p2Session, "/user/game/session/joined", SessionDto, joinFuture)
        p2Session.send("/game/session/${sessionId}/join", [:])
        
        then: "response indicates success"
        def joinResponse = joinFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        joinResponse.id == sessionId
        joinResponse.players.size() == 2
        
        when: "subscribing to game updates"
        def topic = "/game/session/${sessionId}/updated"
        subscribe(p1Session, topic, Map, gameUpdateFuture1)
        
        and: "Player 1 Passes"
        p1Session.send("/game/session/${sessionId}/update", [command: [name: "Pass"]])
        
        then: "Game updates (P1 Pass)"
        def update1 = gameUpdateFuture1.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        update1.activePlayer.color == "White" // P2 is White
        
        when: "Player 2 Passes"
        subscribe(p1Session, topic, Map, gameUpdateFuture2) 
        
        def endGameFuture = new CompletableFuture<Map>()
        subscribe(p1Session, "/game/session/${sessionId}/endgame", Map, endGameFuture)
        
        p2Session.send("/game/session/${sessionId}/update", [command: [name: "Pass"]])
        
        then: "Game ends (Double Pass)"
        def update2 = gameUpdateFuture2.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        update2.isGameEnded == true 
        
        and: "EndGame message received"
        def endGameMsg = endGameFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        endGameMsg.score != null
    }
}
