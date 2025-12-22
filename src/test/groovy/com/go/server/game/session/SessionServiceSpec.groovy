package com.go.server.game.session

import com.go.server.game.bot.BotManager
import com.go.server.game.bot.BotService
import com.go.server.game.bot.GnuGoBotService
import com.go.server.game.client.GameClient
import com.go.server.game.client.GameClientPool
import com.go.server.game.message.handler.MessageHandler
import com.go.server.game.message.messages.JoinedMessage
import com.go.server.game.message.messages.SimpleMessage
import com.go.server.game.message.messages.TerminatedMessage

import com.go.server.game.session.model.BotDifficulty
import com.go.server.game.session.model.Session
import com.go.server.game.session.model.input.CreateSessionDto
import com.go.server.game.session.model.output.SessionDto
import com.go.server.game.session.repository.SessionRepository
import spock.lang.Ignore
import spock.lang.Specification

class SessionServiceSpec extends Specification {
    def 'create a session'() {
        given:
        def repository = Mock(SessionRepository)
        def messageHandler = Mock(MessageHandler)
        def gameClientPool = Mock(GameClientPool)

        def botManager = Mock(BotManager)
        def service = new SessionService(repository, messageHandler, gameClientPool, botManager)
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
        def gameClientPool = Mock(GameClientPool)
        def botManager = Mock(BotManager)
        def service = new SessionService(repository, messageHandler, gameClientPool, botManager)
        def playerId = UUID.randomUUID()
        def createSessionDto = new CreateSessionDto()
        createSessionDto.playerId = playerId
        createSessionDto.difficulty = BotDifficulty.HARD

        when:
        repository.getSessionByPlayerId(playerId) >> Optional.empty()
        def dto = service.createSession(createSessionDto)

        then:
        dto.difficulty == BotDifficulty.HARD.name()
        dto.players.size() == 2
        dto.players.find { it.isBot } != null
        dto.players.find { !it.isBot }.id == playerId.toString()
    }

    @Ignore("Logic changed: New session is created even if one exists")
    def 'no new session will be created if the user is already in a session'() {
        given:
        def repository = Mock(SessionRepository)
        def messageHandler = Mock(MessageHandler)
        def gameClientPool = Mock(GameClientPool)

        def botManager = Mock(BotManager)
        def service = new SessionService(repository, messageHandler, gameClientPool, botManager)
        def playerId = UUID.randomUUID()
        def createSessionDto = new CreateSessionDto()
        def session = Mock(Session)
        def mockDto = Mock(SessionDto)

        when:
        createSessionDto.playerId = playerId
        session.getId() >> "some-id"
        repository.getSession("some-id") >> session
        repository.getSessionByPlayerId(playerId) >> Optional.of(session)
        session.toDto() >> mockDto
        def dto = service.createSession(createSessionDto)

        then:
        dto == mockDto
    }

    def 'terminate a session'() {
        given:
        def repository = Mock(SessionRepository)
        def messageHandler = Mock(MessageHandler)
        def gameClientPool = Mock(GameClientPool)
        def session = Mock(Session)

        def botManager = Mock(BotManager)
        def service = new SessionService(repository, messageHandler, gameClientPool, botManager)

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
        def gameClientPool = Mock(GameClientPool)
        def gameClient = Mock(GameClient)
        def session = Mock(Session)

        def botManager = Mock(BotManager)
        def service = new SessionService(repository, messageHandler, gameClientPool, botManager)

        when:
        repository.findSession("some-id") >> Optional.of(session)
        repository.getSession("some-id") >> session
        gameClientPool.acquire() >> gameClient
        service.updateSession("some-id", new byte[0])

        then:
        1 * session.update()
        1 * messageHandler.send(_ as SimpleMessage)
        1 * gameClientPool.release(gameClient)
    }

    def 'join a session'() {
        given:
        def repository = Mock(SessionRepository)
        def messageHandler = Mock(MessageHandler)
        def gameClientPool = Mock(GameClientPool)
        def session = Mock(Session)
        def playerId = UUID.randomUUID()

        def botManager = Mock(BotManager)
        def service = new SessionService(repository, messageHandler, gameClientPool, botManager)

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

    def 'returns all pending sessions'() {
        given:
        def repository = Mock(SessionRepository)
        def messageHandler = Mock(MessageHandler)
        def gameClientPool = Mock(GameClientPool)

        def botManager = Mock(BotManager)
        def service = new SessionService(repository, messageHandler, gameClientPool, botManager)
        def firstSession = Mock(Session)
        def secondSession = Mock(Session)

        when:
        firstSession.isPending() >> true
        firstSession.isPending() >> false
        repository.getAllSessions() >> [firstSession, secondSession]
        def sessions = service.getPendingSessions()

        then:
        sessions.size() == 1
    }
}
