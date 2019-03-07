package net.florianschoppmann.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

/**
 * The LazyString class represents strings with lazy evaluation.
 *
 * Such a lazy-evaluation string can contain references to other objects that are evaluated (that is, converted into a
 * string representation) only when {@link #toStringWithContext(LazyContext)} is called.
 */
public final class LazyString {
    private final List<Object> objects;
    private final int expectedMaxLength;

    private static final List<Class<?>> KNOWN_IMMUTABLE_TYPES = Arrays.asList(String.class, Character.class);

    public static class Builder {
        private final StringBuilder stringBuilder;
        private final List<Object> objectList = new ArrayList<>();
        private final int expectedMaxLength;

        public Builder(int expectedMaxLength) {
            stringBuilder = new StringBuilder(expectedMaxLength);
            this.expectedMaxLength = expectedMaxLength;
        }

        private void flushStringBuilder() {
            if (stringBuilder.length() > 0) {
                objectList.add(stringBuilder.toString());
                stringBuilder.setLength(0);
            }
        }

        public Builder append(@Nullable Object object) {
            if (object == null || KNOWN_IMMUTABLE_TYPES.contains(object.getClass())) {
                stringBuilder.append(object);
            } else {
                flushStringBuilder();
                objectList.add(object);
            }
            return this;
        }

        public LazyString build() {
            flushStringBuilder();
            return new LazyString(objectList, expectedMaxLength);
        }
    }

    public static LazyString of(String string) {
        return new LazyString(Collections.singletonList(string), string.length());
    }

    private LazyString(List<?> objects, int expectedMaxLength) {
        this.objects = new ArrayList<>(objects);
        this.expectedMaxLength = expectedMaxLength;
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        LazyString other = (LazyString) otherObject;
        if (objects.size() != other.objects.size()) {
            return false;
        }
        Iterator<Object> otherIt = other.objects.iterator();
        for (Object listItem : objects) {
            Object otherListItem = otherIt.next();
            if (KNOWN_IMMUTABLE_TYPES.contains(listItem.getClass())) {
                if (!listItem.equals(otherListItem)) {
                    return false;
                }
            } else {
                if (System.identityHashCode(listItem) != System.identityHashCode(otherListItem)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (Object listItem : objects) {
            result = 31 * result + (
                KNOWN_IMMUTABLE_TYPES.contains(listItem.getClass())
                    ? listItem.hashCode()
                    : System.identityHashCode(listItem)
            );
        }
        return result;
    }

    @Override
    public String toString() {
        return toStringWithContext(String::valueOf);
    }

    public String toStringWithContext(LazyContext context) {
        StringBuilder stringBuilder = new StringBuilder(expectedMaxLength);
        for (Object object : objects) {
            stringBuilder.append(context.stringValueOf(object));
        }
        return stringBuilder.toString();
    }
}
