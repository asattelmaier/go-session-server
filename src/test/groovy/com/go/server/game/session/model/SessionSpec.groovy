package com.go.server.game.session.model


import com.go.server.game.session.model.output.PlayerDto
import spock.lang.Specification

import java.time.LocalTime

class SessionSpec extends Specification {
    def 'notFound creates a session with error'() {
        given:
        def sessionId = "some-id"

        when:
        def session = Session.notFound(sessionId)

        then:
        session.toDto().hasError
        session.toDto().errorMessage == "Session with id \"some-id\" not found"
    }

    def 'add player to session'() {
        given:
        def player = Mock(Player)
        def playerDto = Mock(PlayerDto)
        def session = new Session(LocalTime.now())

        when:
        player.toDto() >> playerDto
        session.addPlayer(player)

        then:
        session.toDto().players.first() == playerDto
    }

    def 'terminate removes all player'() {
        given:
        def player = Mock(Player)
        def session = new Session(LocalTime.now())

        when:
        session.addPlayer(player)
        session.terminate()

        then:
        session.toDto().players.size() == 0
    }

    def 'has'() {
        given:
        def session = new Session(LocalTime.now())

        when:
        def id = session.getId()
        def has = session.has(id)

        then:
        has
    }

    def 'has not'() {
        given:
        def session = new Session(LocalTime.now())

        when:
        def has = session.has("some-id")

        then:
        !has
    }

    def 'is in use'() {
        given:
        def session = new Session(LocalTime.now().minusSeconds(119))

        when:
        def isInUse = session.isInUse()

        then:
        isInUse
    }

    def 'is unused'() {
        given:
        def session = new Session(LocalTime.now().minusSeconds(120))

        when:
        def isInUse = session.isInUse()

        then:
        !isInUse
    }
}
