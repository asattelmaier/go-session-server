package com.go.server.game.bot

import com.fasterxml.jackson.databind.ObjectMapper
import com.go.server.game.model.DeviceMove
import com.go.server.game.session.model.Colors
import com.go.server.game.session.model.Player
import com.go.server.game.session.model.Session
import java.time.Instant
import java.util.function.BiConsumer
import spock.lang.Specification

class BotManagerSpec extends Specification {
    def objectMapper = new ObjectMapper()
    def botService = Mock(BotService)
    def botManager = new BotManager(botService)

    def "checkForBotMove should trigger bot move when it is bot's turn"() {
        given:
        def session = new Session(Instant.now())
        def botPlayer = Player.bot(UUID.randomUUID(), Colors.WHITE)
        def humanPlayer = Player.human(UUID.randomUUID(), Colors.BLACK)
        session.addPlayer(humanPlayer)
        session.addPlayer(botPlayer)

        // Game state where White (Bot) is active
        def gameState = """{
            "activePlayer": "White",
            "positions": [[{"state": "Empty"}]]
        }""".getBytes()

        def messageSender = Mock(BiConsumer)

        when:
        botManager.checkForBotMove(session, gameState, messageSender)

        then:
        1 * botService.getNextMove(_, _) >> DeviceMove.at(1, 1)
        1 * messageSender.accept(session.id, { 
            def node = objectMapper.readTree(it)
            node.path("command").path("name").asText() == "Play" &&
            node.path("command").path("location").path("x").asInt() == 1 &&
            node.path("command").path("location").path("y").asInt() == 1
        })
    }

    def "checkForBotMove should handle bot pass"() {
        given:
        def session = new Session(Instant.now())
        session.addPlayer(Player.bot(UUID.randomUUID(), Colors.WHITE))
        
        def gameState = """{"activePlayer": "White"}""".getBytes()
        def messageSender = Mock(BiConsumer)

        when:
        botManager.checkForBotMove(session, gameState, messageSender)

        then:
        1 * botService.getNextMove(_, _) >> DeviceMove.pass()
        1 * messageSender.accept(session.id, { 
            def node = objectMapper.readTree(it)
            node.path("command").path("name").asText() == "Pass"
        })
    }

    def "checkForBotMove should do nothing if it is human's turn"() {
        given:
        def session = new Session(Instant.now())
        session.addPlayer(Player.bot(UUID.randomUUID(), Colors.WHITE))
        session.addPlayer(Player.human(UUID.randomUUID(), Colors.BLACK))
        
        // Game state where Black (Human) is active
        def gameState = """{"activePlayer": "Black"}""".getBytes()
        def messageSender = Mock(BiConsumer)

        when:
        botManager.checkForBotMove(session, gameState, messageSender)

        then:
        0 * botService.getNextMove(_, _)
        0 * messageSender.accept(_, _)
    }
}
