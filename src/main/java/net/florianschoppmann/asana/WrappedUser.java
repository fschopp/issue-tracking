package net.florianschoppmann.asana;

import com.asana.models.User;

import java.util.Objects;

public final class WrappedUser {
    private final String userId;
    private final User user;

    WrappedUser(User user) {
        userId = Objects.requireNonNull(user.id);
        this.user = user;
    }

    @Override
    public boolean equals(Object otherObject) {
        return this == otherObject
            || (otherObject instanceof WrappedUser && userId.equals(((WrappedUser) otherObject).userId));
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }

    public String getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }
}
