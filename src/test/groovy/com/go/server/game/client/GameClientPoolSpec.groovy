package com.go.server.game.client


import spock.lang.Specification


class GameClientPoolSpec extends Specification {
    def 'acquire creates a game client if there are no free clients'() {
        given:
        def gameClient = Mock(GameClient)
        def gameClientFactory = Mock(GameClientFactory)
        def gameClientPool = new GameClientPool(gameClientFactory)

        when:
        gameClientFactory.createGameClient() >> gameClient
        def newGameClient = gameClientPool.acquire()

        then:
        newGameClient == gameClient
    }

    def 'acquire returns a game client if there are free clients'() {
        given:
        def gameClient = Mock(GameClient)
        def gameClientFactory = Mock(GameClientFactory)
        def gameClientPool = new GameClientPool(gameClientFactory)

        when:
        gameClientFactory.createGameClient() >> gameClient
        def newGameClient = gameClientPool.acquire()
        gameClientPool.release(newGameClient)
        def createdGameClient = gameClientPool.acquire()

        then:
        createdGameClient == gameClient
    }
}
