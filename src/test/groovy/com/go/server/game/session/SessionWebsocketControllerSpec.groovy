package com.go.server.game.session

import com.fasterxml.jackson.databind.ObjectMapper
import com.go.server.game.model.DeviceMove
import com.go.server.game.session.model.GameCommandType
import com.go.server.game.session.model.input.CreateSessionDto
import spock.lang.Specification
import spock.lang.Unroll

class SessionWebsocketControllerSpec extends Specification {

    SessionService sessionService = Mock()
    SessionWebsocketController controller = new SessionWebsocketController(sessionService)

    def "should initialize game on CREATE command"() {
        given:
        String sessionId = "test-session"
        def payload = [command: [name: "Create"]]

        when:
        controller.updateSession(sessionId, payload)

        then:
        1 * sessionService.initializeGame(sessionId)
    }

    def "should process move on PLAY command"() {
        given:
        String sessionId = "test-session"
        def location = [x: 3, y: 4]
        def payload = [command: [name: "Play", location: location]]
        
        when:
        controller.updateSession(sessionId, payload)

        then:
        1 * sessionService.updateSession(sessionId, { DeviceMove move -> 
            move.x == 3 && move.y == 4 && move.type == DeviceMove.MoveType.PLAY
        })
    }

    def "should process pass on PASS command"() {
        given:
        String sessionId = "test-session"
        def payload = [command: [name: "Pass"]]

        when:
        controller.updateSession(sessionId, payload)

        then:
        1 * sessionService.updateSession(sessionId, { DeviceMove move -> 
            move.isPass() 
        })
    }

    def "should handle legacy payload (no command wrapper)"() {
        given:
        String sessionId = "test-session"
        // Legacy payload structure: just flat properties typically, or whatever DeviceMove deserializes from
        // Based on controller logic: objectMapper.convertValue(payload, DeviceMove.class)
        def payload = [x: 10, y: 10, type: "PLAY"] 

        when:
        controller.updateSession(sessionId, payload)

        then:
        1 * sessionService.updateSession(sessionId, { DeviceMove move -> 
            move.x == 10 && move.y == 10 
        })
    }

    def "should ignore unknown commands gracefully"() {
        given:
        String sessionId = "test-session"
        def payload = [command: [name: "NuclearLaunch"]]

        when:
        controller.updateSession(sessionId, payload)

        then:
        // verification: no calls to sessionService
        0 * sessionService._
    }

    def "should ignore malformed legacy payloads"() {
        given:
        String sessionId = "test-session"
        def payload = [some: "garbage"]

        when:
        controller.updateSession(sessionId, payload)

        then:
        0 * sessionService._
    }
}
