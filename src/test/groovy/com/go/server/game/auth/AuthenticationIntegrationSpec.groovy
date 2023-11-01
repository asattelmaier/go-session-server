package com.go.server.game.auth

import com.go.server.auth.model.input.RegisterUserDto
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import spock.lang.Specification

class AuthenticationIntegrationSpec extends Specification {
    def "register a new user"() {
        given:
        def dto = new RegisterUserDto("test-${UUID.randomUUID()}", "test")
        def url = "http://localhost:8080/auth/register"

        when:
        def response = post(url, dto)

        then:
        response.accessToken instanceof String
        response.refreshToken instanceof String
    }

    def "register a guest user"() {
        given:
        def url = "http://localhost:8080/auth/register/guest"

        when:
        def response = post(url)

        then:
        response.accessToken instanceof String
        response.refreshToken instanceof String
    }

    def "authenticate a user"() {
        given:
        def dto = new RegisterUserDto("test-${UUID.randomUUID()}", "test")
        def url = "http://localhost:8080/auth/authenticate"

        when:
        registerUser(dto)
        def response = post(url, dto)

        then:
        response.accessToken instanceof String
        response.refreshToken instanceof String
    }

    def "refresh a token"() {
        given:
        def dto = new RegisterUserDto("test-${UUID.randomUUID()}", "test")
        def url = "http://localhost:8080/auth/refresh-token"

        when:
        def tokens = registerUser(dto)
        Thread.sleep(1000)
        def response = post(url, tokens.refreshToken)

        then:
        response.accessToken instanceof String
        response.refreshToken instanceof String
        response.accessToken != tokens.accessToken
        response.refreshToken == tokens.refreshToken
    }

    def "request user data"() {
        given:
        def dto = new RegisterUserDto("test-${UUID.randomUUID()}", "test")
        def url = "http://localhost:8080/user"

        when:
        def tokens = registerUser(dto)
        def response = getJson(url, tokens.accessToken)

        then:
        response.username == dto.username
    }

    def "logout user"() {
        given:
        def dto = new RegisterUserDto("test-${UUID.randomUUID()}", "test")
        def logoutUrl = "http://localhost:8080/auth/logout"
        def userUrl = "http://localhost:8080/user"

        when:
        def tokens = registerUser(dto)
        post(logoutUrl, tokens.accessToken)
        def response = get(userUrl, tokens.accessToken)

        then:
        response.getStatusLine().statusCode == 403
    }

    def post(String url, Object object) {
        def post = new HttpPost(url)

        post.setEntity(new StringEntity(JsonOutput.toJson(object)))

        return toJson(request(post))
    }

    def post(String url) {
        def post = new HttpPost(url)

        return toJson(request(post))
    }

    def post(String url, String token) {
        def post = new HttpPost(url)

        post.setHeader("Authorization", "Bearer $token")

        return toJson(request(post))
    }

    def getJson(String url, String token) {
        def get = new HttpGet(url)

        get.setHeader("Authorization", "Bearer $token")

        return toJson(request(get))
    }

    def get(String url, String token) {
        def get = new HttpGet(url)

        get.setHeader("Authorization", "Bearer $token")

        return request(get)
    }

    def registerUser(Object object) {
        def url = "http://localhost:8080/auth/register"
        def post = new HttpPost(url)

        post.setEntity(new StringEntity(JsonOutput.toJson(object)))

        return toJson(request(post))
    }

    def request(HttpRequestBase http) {
        def httpClient = HttpClients.createDefault()

        http.setHeader("Content-Type", "application/json")

        return httpClient.execute(http)
    }

    def toJson(CloseableHttpResponse response) {
        def jsonSlurper = new JsonSlurper()
        def payload = EntityUtils.toString(response.getEntity())

        if (payload.length()) {
            return jsonSlurper.parseText(payload)
        }

        return payload
    }
}

