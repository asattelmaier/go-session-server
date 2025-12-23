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



        when: "Player 1 Plays"
        playMove(userA, UUID.fromString(game.id), 0, 0)

        and: "Player 2 Passes"
        passTurn(userB, UUID.fromString(game.id))

        and: "Player 1 Passes (Double Pass)"
        def endGameFuture = new CompletableFuture<Map>()
        subscribe(userB.session, "/game/session/${game.id}/endgame", Map, endGameFuture)
        
        def finalUpdate = passTurn(userA, UUID.fromString(game.id))

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

    def "User cannot play on an occupied intersection"() {
        given: "A running 9x9 game"
        def game = createGame(userA, 9)
        joinGame(userB, game.id)

        and: "Player 1 plays at 0,0"
        playMove(userA, UUID.fromString(game.id), 0, 0)

        when: "Player 2 tries to play at 0,0"
        def error = expectError(userB) {
            sendPlayMove(userB, game.id, 0, 0)
        }

        then: "An Invalid Move error is returned"
        error.code == "INVALID_MOVE"
    }

    def "User cannot play when it is not their turn"() {
        given: "A running 9x9 game (Black starts)"
        def game = createGame(userA, 9)
        joinGame(userB, game.id)

        when: "Player 2 (White) tries to play first"
        def error = expectError(userB) {
            sendPlayMove(userB, game.id, 5, 5)
        }

        then: "An Invalid Move error is returned"
        error.code == "INVALID_MOVE"
    }
    def "Guest User should be able to create game and play move"() {
        given: "A registered guest user"
        def user = registerGuest()
        assert user != null
        assert user.isRegistered()
        assert user.username.startsWith("Guest-")
        connect(user)

        when: "Guest creates a game and plays a move"
        def game = createGame(user, 9, BotDifficulty.EASY)
        def update = playMove(user, UUID.fromString(game.id), 2, 2)

        then: "Move is accepted and stone is placed"
        update != null
        update.positions.last().rows.collectMany { it.cols }.find { it.location.x == 2 && it.location.y == 2 }.state == "Black"
    }
}
