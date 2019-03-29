package net.florianschoppmann.issuetracking;

import java.net.URI;

final class Common {
    private Common() { }

    static URI uriFromString(String string) {
        return URI.create(string.endsWith("/")
            ? string
            : (string + '/')
        );
    }
}
