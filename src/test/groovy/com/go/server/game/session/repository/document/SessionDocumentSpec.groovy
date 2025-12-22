package com.go.server.game.session.repository.document

import com.go.server.game.session.model.BotDifficulty
import com.go.server.game.session.model.Colors
import com.go.server.game.session.model.Player
import com.go.server.game.session.model.Session
import com.google.cloud.Timestamp
import spock.lang.Specification

import java.time.Instant

class SessionDocumentSpec extends Specification {

    def "fromSession should correctly map all fields"() {
        given: "a session with moves and custom board size"
        def players = [
            Player.human(UUID.randomUUID(), Colors.BLACK),
            Player.bot(UUID.randomUUID(), Colors.WHITE)
        ]
        def session = new Session(UUID.randomUUID().toString(), Instant.now(), players)
        session.boardSize = 9
        session.difficulty = BotDifficulty.HARD
        session.addMove("C3")
        session.addMove("D4")

        when: "converting to document"
        def doc = SessionDocument.fromSession(session)

        then: "fields are correctly mapped"
        doc.id == session.id
        doc.boardSize == 9
        doc.difficulty == BotDifficulty.HARD
        doc.moves == ["C3", "D4"]
        doc.playerIds.size() == 2
        doc.playerIds.contains(players[0].id.toString())
        doc.playerIds.contains(players[1].id.toString())
        doc.players.size() == 2
        doc.updated != null
    }

    def "toSession should correctly restore all fields"() {
        given: "a session document"
        def playerDocs = [
            new PlayerDocument(UUID.randomUUID().toString(), Colors.BLACK, false),
            new PlayerDocument(UUID.randomUUID().toString(), Colors.WHITE, true)
        ]
        def doc = new SessionDocument(
            "test-session",
            playerDocs,
            BotDifficulty.MEDIUM,
            13,
            ["E5", "F6"],
            Instant.now()
        )

        when: "converting back to session"
        def session = SessionDocument.toSession(doc)

        then: "session reflects document state"
        session.id == "test-session"
        session.boardSize == 13
        session.difficulty.get() == BotDifficulty.MEDIUM
        session.moves == ["E5", "F6"]
        session.players.size() == 2
        session.players.find { it.isBot() }.color == Colors.WHITE
    }

    def "toSession should handle missing boardSize by defaulting to 19"() {
        given: "a document with null or zero boardSize"
        def doc = new SessionDocument()
        doc.id = "no-size"
        doc.players = [new PlayerDocument(UUID.randomUUID().toString(), Colors.BLACK, false)]
        doc.updated = Timestamp.now()
        doc.boardSize = inputSize

        when:
        def session = SessionDocument.toSession(doc)

        then:
        session.boardSize == expectedSize

        where:
        inputSize | expectedSize
        null      | 19
        0         | 19
        9         | 9
    }
}
