package game.client

import com.go.server.game.client.GameClientHandler
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import spock.lang.Specification

import java.util.function.Function

class GameClientHandlerSpec extends Specification {
    def 'applies incoming text message to listener'() {
        given:
        def listener = Mock(Function<byte[], Void>)
        def session = Mock(WebSocketSession)
        def message = new TextMessage("Test")
        def gameClientHandler = new GameClientHandler()

        when:
        gameClientHandler.addListener(listener)
        gameClientHandler.handleTextMessage(session, message)

        then:
        1 * listener.apply("Test".getBytes())
    }
}
