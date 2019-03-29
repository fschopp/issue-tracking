package net.florianschoppmann.issuetracking.util;

@FunctionalInterface
public interface LazyContext {
    String stringValueOf(Object object);
}
