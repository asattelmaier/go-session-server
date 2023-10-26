package com.go.server.user

import com.go.server.user.exeption.InvalidUserIdException
import com.go.server.user.exeption.UserNotFoundException
import com.go.server.user.model.User
import com.go.server.user.model.output.UserDto
import spock.lang.Specification

class UserServiceSpec extends Specification {
    def 'create a guest user'() {
        given:
        def repository = Mock(UserRepository)
        def service = new UserService(repository)

        when:
        def dto = service.createGuestUser()

        then:
        dto.username.startsWith('Guest')
    }

    def 'returns a user'() {
        given:
        def repository = Mock(UserRepository)
        def service = new UserService(repository)
        def userUuid = UUID.randomUUID()
        def userId = userUuid.toString()
        def user = Mock(User)
        def optionalUser = Optional.of(user)
        def userDto = Mock(UserDto)

        when:
        user.toDto() >> userDto
        repository.getUser(userUuid) >> optionalUser
        def dto = service.getUser(userId)

        then:
        userDto == dto
    }

    def 'throws UserNotFoundException if no user was found'() {
        given:
        def repository = Mock(UserRepository)
        def service = new UserService(repository)
        def userUuid = UUID.randomUUID()
        def userId = userUuid.toString()
        def optionalUser = Optional.empty()

        when:
        repository.getUser(userUuid) >> optionalUser
        service.getUser(userId)

        then:
        thrown(UserNotFoundException)
    }

    def 'throws InvalidUserIdException if the provided id is invalid'() {
        given:
        def repository = Mock(UserRepository)
        def service = new UserService(repository)
        def userUuid = UUID.randomUUID()
        def userId = 'invalid-id'
        def optionalUser = Optional.empty()

        when:
        repository.getUser(userUuid) >> optionalUser
        service.getUser(userId)

        then:
        thrown(InvalidUserIdException)
    }
}
