package game.session

import com.go.server.game.client.GameClient
import com.go.server.game.client.GameClientPool
import com.go.server.game.message.handler.MessageHandler
import com.go.server.game.message.messages.JoinedMessage
import com.go.server.game.message.messages.TerminatedMessage
import com.go.server.game.message.messages.UpdatedMessage
import com.go.server.game.session.SessionRepository
import com.go.server.game.session.SessionService
import com.go.server.game.session.model.Session
import com.go.server.game.session.model.output.SessionDto
import spock.lang.Specification

class SessionServiceSpec extends Specification {
    def 'create a session'() {
        given:
        def repository = Mock(SessionRepository)
        def messageHandler = Mock(MessageHandler)
        def gameClientPool = Mock(GameClientPool)
        def service = new SessionService(repository, messageHandler, gameClientPool)

        when:
        def dto = service.createSession("player-id")

        then:
        dto.players.first().id == "player-id"
    }

    def 'terminate a session'() {
        given:
        def repository = Mock(SessionRepository)
        def messageHandler = Mock(MessageHandler)
        def gameClientPool = Mock(GameClientPool)
        def session = Mock(Session)
        def service = new SessionService(repository, messageHandler, gameClientPool)

        when:
        session.toDto() >> Mock(SessionDto)
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
        def service = new SessionService(repository, messageHandler, gameClientPool)

        when:
        gameClientPool.acquire() >> gameClient
        service.updateSession("some-id", new byte[0])

        then:
        1 * messageHandler.send(_ as UpdatedMessage)
        1 * gameClientPool.release(gameClient)
    }

    def 'join a session'() {
        given:
        def repository = Mock(SessionRepository)
        def messageHandler = Mock(MessageHandler)
        def gameClientPool = Mock(GameClientPool)
        def session = Mock(Session)
        def service = new SessionService(repository, messageHandler, gameClientPool)

        when:
        session.toDto() >> Mock(SessionDto)
        repository.getSession("some-id") >> session
        repository.updateSession(session) >> session
        service.joinSession("player-id", "some-id")

        then:
        1 * session.addPlayer({ it.toDto().id == "player-id" })
        1 * messageHandler.send(_ as JoinedMessage)
    }
}
