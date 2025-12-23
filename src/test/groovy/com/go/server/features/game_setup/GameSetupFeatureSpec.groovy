package com.go.server.features.game_setup

import com.go.server.BaseIntegrationSpec
import com.go.server.game.session.model.BotDifficulty
import com.go.server.game.session.model.input.CreateSessionDto
import com.go.server.game.session.model.output.SessionDto
import spock.lang.Unroll

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class GameSetupFeatureSpec extends BaseIntegrationSpec {

    def "Guest can create a new game lobby"() {
        given: "An authenticated guest user"
        def user = registerUser(createUsername())
        connect(user)

        when: "The user creates a new game session"
        def game = createGame(user, 19)

        then: "A new session is created and returned"
        isGameCreated(game)
        hasPlayers(game, 1)
        hasPlayer(game, user)
    }

    def "Guest can cancel a pending game"() {
        given: "An authenticated user with a pending game"
        def user = registerUser(createUsername())
        connect(user)
        def game = createGame(user, 19)

        when: "The user cancels the game"
        def terminatedGame = terminateGame(user, game.id)

        then: "The game is terminated"
        isGameCreated(terminatedGame)
        terminatedGame.id == game.id
    }

    def "Only pending sessions are listed"() {
        given: "Three game scenarios"
        def userA = registerUser(createUsername())
        connect(userA)
        def userB = registerUser(createUsername())
        connect(userB)

        when: "1. Terminated games are not listed"
        def gameTerminated = createGame(userA, 19)
        terminateGame(userA, gameTerminated.id)

        and: "2. Active (full) games are not listed"
        def gameActive = createGame(userA, 19)
        joinGame(userB, gameActive.id)

        and: "3. Bot games (instantly full) are not listed"
        createGame(userA, 9, BotDifficulty.EASY)

        and: "4. Pending games ARE listed"
        def gamePending = createGame(userA, 13)

        then: "Only the pending game is found in the list"
        def pendingSessions = waitForSessionInPendingList(userA, gamePending)

        pendingSessions.size() > 0
        isGameInList(gamePending, pendingSessions)
        isGameNotInList(gameTerminated, pendingSessions)
        isGameNotInList(gameActive, pendingSessions)
    }

    def "Guest can join an existing game"() {
        given: "A game created by user A"
        def userA = registerUser(createUsername())
        connect(userA)
        def game = createGame(userA, 19)

        and: "A second authenticated guest user B"
        def userB = registerUser(createUsername())
        connect(userB)

        when: "User B joins the game"
        def joinResponse = joinGame(userB, game.id)

        then: "Both players are in the game session"
        isGameCreated(joinResponse)
        joinResponse.id == game.id
        hasPlayers(joinResponse, 2)
        hasPlayer(joinResponse, userB)
    }

    @Unroll
    def "Guest can create a game with board size #size"() {
        given: "An authenticated user"
        def user = registerUser(createUsername())
        connect(user)

        when: "Creating a game with size #size"
        def game = createGame(user, size)

        then: "The created game has the correct board size"
        game.boardSize == size

        where:
        size << [9, 13, 19]
    }

    @Unroll
    def "Guest can play against a Bot with difficulty #difficulty"() {
        given: "An authenticated user"
        def user = registerUser(createUsername())
        connect(user)

        when: "Creating a game with bot difficulty #difficulty"
        def game = createGame(user, 9, difficulty)

        then: "The game includes a Bot player"
        hasPlayers(game, 2)
        hasBot(game, difficulty)

        where:
        difficulty << [BotDifficulty.EASY, BotDifficulty.MEDIUM, BotDifficulty.HARD]
    }
}
