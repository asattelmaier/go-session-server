package com.go.server.features.authentication

import com.go.server.BaseIntegrationSpec


class AuthenticationFeatureSpec extends BaseIntegrationSpec {

    def "Guest user can sign in anonymously"() {
        when: "A guest registers with a username"
        def username = createUsername()
        def user = registerUser(username)

        then: "The registration is successful"
        user.isRegistered()
    }

    def "Registered guest can reuse their authentication to connect"() {
        given: "A registered guest user"
        def username = createUsername()
        def user = registerUser(username)

        when: "The user connects to the websocket"
        def session = connect(user)

        then: "The connection is successful and the session is established"
        session.isConnected()
    }

    def "Use cannot register with an empty username"() {
        when: "A guest tries to register with an empty username"
        def user = registerUser("")

        then: "The registration fails"
        !user.isRegistered()
    }

    def "User cannot connect with an invalid token"() {
        when: "Connecting with an invalid token"
        connect("invalid.token.signature")

        then: "The connection is forbidden"
        thrown(Exception)
    }


    def "User can register with standard credentials"() {
        given: "A new user"
        def username = createUsername()
        def password = "secure-password"

        when: "The user registers"
        def user = registerUser(username, password)

        then: "The registration is successful"
        user.isRegistered()
    }

    def "User can login with credentials"() {
        given: "A registered user"
        def username = createUsername()
        def password = "secure-password"
        registerUser(username, password)

        when: "The user authenticates"
        def user = login(username, password)

        then: "The login is successful"
        user.isRegistered()
    }

    def "User can refresh access token"() {
        given: "A registered user"
        def user = registerUser(createUsername())

        when: "The user refreshes the token"
        waitForTokenCycle()
        def newTokens = refresh(user.tokens.refreshToken)

        then: "A new access token is returned"
        newTokens.accessToken instanceof String
        newTokens.accessToken != user.tokens.accessToken
        newTokens.refreshToken == user.tokens.refreshToken
    }

    def "User can retrieve their own profile"() {
        given: "A registered user"
        def user = registerUser(createUsername())

        when: "The user requests their profile"
        def profile = getProfile(user)

        then: "The profile data is correct"
        profile.username == user.username
    }

    def "User can logout"() {
        given: "A registered user"
        def user = registerUser(createUsername())

        when: "The user logs out"
        logout(user)

        and: "Tries to access protected resource"
        def profile = getProfile(user)

        then: "Access is forbidden"
        profile == null
    }
}
