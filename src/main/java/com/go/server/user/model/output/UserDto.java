package com.go.server.user.model.output;

import java.util.Objects;

public class UserDto {
    public final String id;
    public final String username;

    public UserDto(final String id, final String username) {
        this.id = id;
        this.username = username;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == null) {
            return false;
        }

        if (!(other instanceof UserDto otherObject)) {
            return false;
        }

        return otherObject.id.equals(id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
