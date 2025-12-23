package com.go.server.game.session
import com.fasterxml.jackson.databind.ObjectMapper
import com.go.server.game.engine.GameEngine
import com.go.server.game.message.handler.MessageHandler
import com.go.server.game.message.messages.JoinedMessage
import com.go.server.game.message.messages.SimpleMessage
import com.go.server.game.message.messages.TerminatedMessage
import com.go.server.game.model.DeviceMove
import com.go.server.game.model.dto.GameDto
import com.go.server.game.session.model.BotDifficulty
import com.go.server.game.session.model.Session
import com.go.server.game.session.model.input.CreateSessionDto
import com.go.server.game.session.model.output.SessionDto
import com.go.server.game.session.repository.SessionRepository
import spock.lang.Ignore
import spock.lang.Specification

class SessionServiceSpec extends Specification {
    private ObjectMapper objectMapper = new ObjectMapper()

    def 'create a session'() {
        given:
        def repository = Mock(SessionRepository)
        def messageHandler = Mock(MessageHandler)
        def gameEngine = Mock(GameEngine)

        def service = new SessionService(repository, messageHandler, gameEngine)
        def playerId = UUID.randomUUID()
        def createSessionDto = new CreateSessionDto()

        when:
        createSessionDto.playerId = playerId
        repository.getSessionByPlayerId(playerId) >> Optional.empty()
        def dto = service.createSession(createSessionDto)

        then:
        dto.players.first().id == playerId.toString()
        dto.difficulty == null
        dto.players.size() == 1
    }

    def 'create a session with bot difficulty'() {
        given:
        def repository = Mock(SessionRepository)
        def messageHandler = Mock(MessageHandler)
        def gameEngine = Mock(GameEngine)
        def service = new SessionService(repository, messageHandler, gameEngine)
        def playerId = UUID.randomUUID()
        def createSessionDto = new CreateSessionDto()
        createSessionDto.playerId = playerId
        createSessionDto.difficulty = BotDifficulty.HARD
        createSessionDto.boardSize = 13

        when:
        repository.getSessionByPlayerId(playerId) >> Optional.empty()
        def dto = service.createSession(createSessionDto)

        then:
        dto.difficulty == BotDifficulty.HARD.name()
        dto.players.size() == 2
        dto.players.find { it.isBot } != null
        dto.players.find { !it.isBot }.id == playerId.toString()
    }

    def 'terminate a session'() {
        given:
        def repository = Mock(SessionRepository)
        def messageHandler = Mock(MessageHandler)
        def gameEngine = Mock(GameEngine)
        def session = Mock(Session)
        def service = new SessionService(repository, messageHandler, gameEngine)

        when:
        session.toDto() >> Mock(SessionDto)
        repository.findSession("some-id") >> Optional.of(session)
        repository.getSession("some-id") >> session
        service.terminateSession("some-id")

        then:
        1 * repository.removeSession(session)
        1 * messageHandler.send(_ as TerminatedMessage)
    }

    def 'update a session'() {
        given:
        def repository = Mock(SessionRepository)
        def messageHandler = Mock(MessageHandler)
        def gameEngine = Mock(GameEngine)
        def session = Mock(Session)
        def service = new SessionService(repository, messageHandler, gameEngine)
        
        def move = DeviceMove.at(3, 3)

        def activePlayer = Mock(com.go.server.game.session.model.Player)
        activePlayer.toDto() >> new com.go.server.game.session.model.output.PlayerDto("p1", "Black", false)
        activePlayer.getColor() >> com.go.server.game.session.model.Colors.BLACK

        def passivePlayer = Mock(com.go.server.game.session.model.Player)
        passivePlayer.toDto() >> new com.go.server.game.session.model.output.PlayerDto("p2", "White", false)

        def game = Mock(com.go.server.game.model.Game)
        game.getBoardSize() >> 9
        game.getActivePlayer() >> activePlayer
        game.getPassivePlayer() >> passivePlayer
        game.isGameEnded() >> false
        game.getPositions() >> [] // Empty board for simplicity of test (stream of empty list)

        when:
        repository.findSession("some-id") >> Optional.of(session)
        repository.getSession("some-id") >> session
        session.getId() >> "some-id"
        // Bot check shouldn't trigger if invalid players
        session.getPlayers() >> [] 
        
        service.updateSession("some-id", move)

        then:
        1 * gameEngine.processMove(session, _ as DeviceMove) >> game
        1 * session.update()
        // Broadcasts game state
        1 * messageHandler.send(_ as SimpleMessage)
    }

    def 'join a session'() {
        given:
        def repository = Mock(SessionRepository)
        def messageHandler = Mock(MessageHandler)
        def gameEngine = Mock(GameEngine)
        def session = Mock(Session)
        def playerId = UUID.randomUUID()
        def service = new SessionService(repository, messageHandler, gameEngine)

        when:
        session.toDto() >> Mock(SessionDto)
        repository.findSession("some-id") >> Optional.of(session)
        repository.getSession("some-id") >> session
        repository.updateSession(session) >> session
        service.joinSession(playerId.toString(), "some-id")

        then:
        1 * session.addPlayer({ it.toDto().id == playerId.toString() })
        1 * messageHandler.send(_ as JoinedMessage)
    }
}
