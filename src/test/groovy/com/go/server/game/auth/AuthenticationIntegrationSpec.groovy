package com.go.server.game.auth

import com.go.server.BaseIntegrationSpec
import com.go.server.auth.model.input.RefreshTokenDto
import com.go.server.auth.model.input.RegisterUserDto
import groovy.json.JsonSlurper
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

class AuthenticationIntegrationSpec extends BaseIntegrationSpec {

    def "register a new user"() {
        given:
        def dto = new RegisterUserDto("test-${UUID.randomUUID()}", "test")

        when:
        def response = post("/auth/register", dto)

        then:
        response.accessToken instanceof String
        response.refreshToken instanceof String
    }

    def "register a guest user"() {
        when:
        def response = post("/auth/register/guest", null)

        then:
        response.accessToken instanceof String
        response.refreshToken instanceof String
    }

    def "authenticate a user"() {
        given:
        def dto = new RegisterUserDto("test-${UUID.randomUUID()}", "test")

        when:
        registerUser(dto.username(), dto.password())
        def response = post("/auth/authenticate", dto)

        then:
        response.accessToken instanceof String
        response.refreshToken instanceof String
    }

    def "refresh a token"() {
        given:
        def dto = new RegisterUserDto("test-${UUID.randomUUID()}", "test")

        when:
        def tokens = registerUser(dto.username(), dto.password())
        def refreshTokenDto = new RefreshTokenDto(tokens.refreshToken)
        Thread.sleep(1000)
        def response = post("/auth/refresh-token", refreshTokenDto)

        then:
        response.accessToken instanceof String
        response.refreshToken instanceof String
        response.accessToken != tokens.accessToken
        response.refreshToken == tokens.refreshToken
    }

    def "request user data"() {
        given:
        def dto = new RegisterUserDto("test-${UUID.randomUUID()}", "test")

        when:
        def tokens = registerUser(dto.username(), dto.password())
        def response = getJson("/user", tokens.accessToken)

        then:
        response.username == dto.username()
    }

    def "logout user"() {
        given:
        def dto = new RegisterUserDto("test-${UUID.randomUUID()}", "test")

        when:
        def tokens = registerUser(dto.username(), dto.password())
        post("/auth/logout", tokens.accessToken) // Assuming post handles authorization header if passed differently logic needed
        // wait, base post doesn't handle auth header easily.
        // Let's use specific helper for this test or add it to Base
        def postLogout = new org.apache.http.client.methods.HttpPost(BASE_URL + "/auth/logout")
        postLogout.setHeader("Authorization", "Bearer " + tokens.accessToken)
        HttpClients.createDefault().execute(postLogout)
        
        def response = get("/user", tokens.accessToken)

        then:
        response.getStatusLine().statusCode == 403
    }
    
    // Minimal helpers not in BaseIntegrationSpec yet
    
    def getJson(String path, String token) {
        def response = get(path, token)
        def content = EntityUtils.toString(response.getEntity())
        return new JsonSlurper().parseText(content)
    }

    def get(String path, String token) {
        def fullUrl = path.startsWith("http") ? path : BASE_URL + path
        def get = new HttpGet(fullUrl)
        get.setHeader("Authorization", "Bearer $token")
        return HttpClients.createDefault().execute(get)
    }
}

