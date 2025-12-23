package com.go.server.features.gameplay

import com.go.server.BaseIntegrationSpec
import com.go.server.game.session.model.BotDifficulty
import spock.lang.Shared

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class GameMechanicsFeatureSpec extends BaseIntegrationSpec {

    @Shared TestUser userA
    @Shared TestUser userB

    def setup() {
        userA = registerUser(createUsername())
        connect(userA)
        userB = registerUser(createUsername())
        connect(userB)
    }

    def "Players are notified when an opponent makes a move"() {
        given: "A running 9x9 game between two humans"
        def game = createGame(userA, 9)
        joinGame(userB, game.id)

        and: "Player 2 is listening for updates"
        def p2UpdateFuture = new CompletableFuture<Map>()
        subscribe(userB.session, "/game/session/${game.id}/updated", Map, p2UpdateFuture)

        when: "Player 1 places a stone at C3 (2,2)"
        def updateA = playMove(userA, UUID.fromString(game.id), 2, 2)

        then: "Player 2 receives the game update"
        def updateB = p2UpdateFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        
        and: "The board reflects the move and turn change"
        updateA.activePlayer.color == "White"
        updateB.activePlayer.color == "White"
    }

    def "Game ends and score is broadcast when both players pass"() {
        given: "A running 9x9 game between two humans"
        def game = createGame(userA, 9)
        joinGame(userB, game.id)



        when: "Player 1 Passes"
        playMove(userA, UUID.fromString(game.id), 0, 0)
        passTurn(userA, UUID.fromString(game.id))

        and: "Player 2 Passes"
        def endGameFuture = new CompletableFuture<Map>()
        subscribe(userB.session, "/game/session/${game.id}/endgame", Map, endGameFuture)
        
        def finalUpdate = passTurn(userB, UUID.fromString(game.id))

        then: "The game marks itself as ended"
        finalUpdate.isGameEnded == true

        and: "The final score is broadcast to Player 2"
        def endGame = endGameFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        endGame.score != null
        endGame.score instanceof Number
        endGame.score != null
        endGame.score instanceof Number
    }

    def "Player can play a move against a Bot"() {
        given: "A running game against a Bot"
        def game = createGame(userA, 19, BotDifficulty.EASY)

        when: "Player makes a move"
        def update = playMove(userA, UUID.fromString(game.id), 2, 2)

        then: "The update confirms the move"
        update.activePlayer.color == "White"
    }
}
