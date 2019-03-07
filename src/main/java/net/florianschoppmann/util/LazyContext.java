package net.florianschoppmann.util;

@FunctionalInterface
public interface LazyContext {
    String stringValueOf(Object object);
}
