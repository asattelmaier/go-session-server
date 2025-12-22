package com.go.server.game.engine

import com.go.server.game.session.model.BotDifficulty
import spock.lang.Specification
import spock.lang.Unroll

class GnuGoGameEngineSpec extends Specification {

    def "toGtpCoord should correctly convert coordinates and skip 'I'"() {
        given:
        def service = new GnuGoGameEngine()

        expect:
        service.toGtpCoord(x, y, 9) == expectedGtp

        where:
        x | y | expectedGtp
        0 | 8 | "A1"
        0 | 0 | "A9"
        7 | 8 | "H1"
        8 | 8 | "J1" // Skip I
        8 | 0 | "J9"
    }

    def "parseGnuGoResponse should correctly convert GTP coordinates back"() {
        // parseGnuGoResponse returns Optional<DeviceMove>. 
        // We can't access private method parseGtpCoord directly if it's private.
        // It's package-private in implementation.
        // But logic is tested via parseGnuGoResponse indirectly? 
        // Actually GnuGoGameEngine.parseGnuGoResponse calls DeviceMove.at(x,y).
        
        given:
        def service = new GnuGoGameEngine()

        expect:
        def moveOpt = service.parseGnuGoResponse("= " + gtpCoord, 9) // Adding = prefix simulation
        def move = moveOpt.get()
        move.x == expectedX
        move.y == expectedY

        where:
        gtpCoord | expectedX | expectedY
        "A1"     | 0         | 8
        "A9"     | 0         | 0
        "H1"     | 7         | 8
        "J1"     | 8         | 8 // Adjust for skipped I
        "J9"     | 8         | 0
    }

    def "mapDifficultyToLevel should map enums to correct GnuGo levels"() {
        given:
        def service = new GnuGoGameEngine()

        expect:
        service.mapDifficultyToLevel(difficulty) == expectedLevel

        where:
        difficulty           | expectedLevel
        BotDifficulty.EASY   | 1
        BotDifficulty.MEDIUM | 10
        BotDifficulty.HARD   | 20
        null                 | 10
    }

    @Unroll
    def "parseGnuGoResponse should handle GTP success and special responses"() {
        given:
        def service = new GnuGoGameEngine()

        when:
        def result = service.parseGnuGoResponse(response, 9)

        then:
        result.isPresent() == expectedPresent
        if (expectedPass != null) {
            result.get().isPass() == expectedPass
        }
        if (expectedX != null) {
            result.get().x == expectedX
            result.get().y == expectedY
        }

        where:
        response     | expectedPresent | expectedPass | expectedX | expectedY
        "= A1"       | true            | false        | 0         | 8
        "= PASS"     | true            | true         | null      | null
        "? unknown"  | false           | null         | null      | null
        ""           | false           | null         | null      | null
    }
}
