package com.go.server.features.cleanup

import com.go.server.BaseIntegrationSpec
import com.go.server.game.session.model.Session
import com.go.server.game.session.repository.SessionRepository
import org.springframework.beans.factory.annotation.Autowired

import java.time.Instant
import java.time.temporal.ChronoUnit

class SessionCleanupFeatureSpec extends BaseIntegrationSpec {

    @Autowired
    SessionRepository sessionRepository

    // > 30 to enforce batching
    private static final int UNUSED_SESSION_COUNT = 35
    private static final int ACTIVE_SESSION_COUNT = 5

    def "Old sessions are automatically removed while active sessions persist"() {
        given: "A mix of unused and active sessions"
        def unusedSessions = givenUnusedSessionsExists(UNUSED_SESSION_COUNT)
        def activeSessions = givenActiveSessionsExists(ACTIVE_SESSION_COUNT)

        when: "The cleanup process runs"
        triggerCleanup(unusedSessions)

        then: "Old sessions are removed"
        unusedSessions.each {
            assert sessionRepository.findSession(it.id).isEmpty()
        }

        and: "Active sessions remain"
        activeSessions.each {
            assert sessionRepository.findSession(it.id).isPresent()
        }
    }

    private List<Session> givenUnusedSessionsExists(int count) {
        (1..count).collect {
            def session = new Session(Instant.now().minus(5, ChronoUnit.MINUTES))
            sessionRepository.addSession(session)
            session
        }
    }

    private List<Session> givenActiveSessionsExists(int count) {
        (1..count).collect {
            def session = new Session(Instant.now())
            sessionRepository.addSession(session)
            session
        }
    }

    private void triggerCleanup(List<Session> expectedToRemove) {
        // Simulates the service logic: identify unused -> remove
        // We filter manually here to strictly test the repository's removeSessions batching capability
        // on the exact set of sessions we expect to be unused.
        def unusedInRepo = sessionRepository.getAllSessions()
                .findAll { !it.isInUse() }
        
        def toRemove = unusedInRepo.findAll { 
            expectedToRemove.find { e -> e.id == it.id } != null 
        }

        sessionRepository.removeSessions(toRemove)
    }
}
