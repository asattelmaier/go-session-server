package com.go.server.game.session


import com.go.server.game.session.model.Player
import com.go.server.game.session.model.Session
import spock.lang.Specification

class SessionRepositorySpec extends Specification {
    def 'adds a Session'() {
        given:
        def repository = new SessionRepository()

        when:
        repository.addSession(Mock(Session))
        repository.addSession(Mock(Session))
        def sessions = repository.getAll()

        then:
        sessions.size() == 2
    }

    def 'adds not a session if it is already added'() {
        given:
        def session = Mock(Session)
        def repository = new SessionRepository()

        when:
        repository.addSession(session)
        repository.addSession(session)
        def sessions = repository.getAll()

        then:
        sessions.size() == 1
    }

    def 'removes a session'() {
        given:
        def session = Mock(Session)
        def repository = new SessionRepository()

        when:
        repository.addSession(session)
        repository.removeSession(session)
        def sessions = repository.getAll()


        then:
        sessions.size() == 0
    }

    def 'removes nothing if the session did not exists'() {
        given:
        def session = Mock(Session)
        def repository = new SessionRepository()

        when:
        repository.addSession(session)
        repository.removeSession(Mock(Session))
        def sessions = repository.getAll()

        then:
        sessions.size() == 1
    }

    def 'returns a session'() {
        given:
        def mockSession = Mock(Session)
        def repository = new SessionRepository()

        when:
        mockSession.has("some-id") >> true
        repository.addSession(mockSession)
        def session = repository.getSession("some-id")


        then:
        session == mockSession
    }

    def 'returns a session session with error if the session was not found'() {
        given:
        def mockSession = Mock(Session)
        def repository = new SessionRepository()

        when:
        repository.addSession(mockSession)
        def session = repository.getSession("some-id")

        then:
        session.toDto().hasError
        session.toDto().errorMessage == "Session with id some-id not found"
    }

    def 'updates a session'() {
        given:
        def session = new Session()
        def repository = new SessionRepository()

        when:
        repository.addSession(session)
        session.addPlayer(Mock(Player))
        repository.updateSession(session)

        then:
        repository.getSession(session.id).toDto().players.size() == 1
    }

    def 'update returns a session with error if the session is not stored'() {
        given:
        def mockSession = Mock(Session)
        def repository = new SessionRepository()

        when:
        mockSession.getId() >> "some-id"
        def session = repository.updateSession(mockSession)

        then:
        session.toDto().hasError
        session.toDto().errorMessage == "Session with id some-id not found"
    }
}
