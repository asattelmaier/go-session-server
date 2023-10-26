package com.go.server.user


import com.go.server.user.model.User
import spock.lang.Specification

class UserRepositorySpec extends Specification {
    def 'creates a user'() {
        given:
        def userId = UUID.randomUUID()
        def mockUser = Mock(User)
        def repository = new UserRepository()

        when:
        mockUser.getId() >> userId
        repository.createUser(mockUser)
        def user = repository.getUser(userId)

        then:
        mockUser == user.get()
    }

    def 'is present if user is found'() {
        given:
        def userId = UUID.randomUUID()
        def mockUser = Mock(User)
        def repository = new UserRepository()

        when:
        mockUser.getId() >> userId
        repository.createUser(mockUser)
        repository.getUser(userId)
        def user = repository.getUser(userId)

        then:
        mockUser == user.get()
    }

    def 'is empty if user is not found'() {
        given:
        def userId = UUID.randomUUID()
        def mockUser = Mock(User)
        def repository = new UserRepository()

        when:
        mockUser.getId() >> userId
        repository.getUser(userId)
        def user = repository.getUser(userId)

        then:
        user.isEmpty()
    }
}
