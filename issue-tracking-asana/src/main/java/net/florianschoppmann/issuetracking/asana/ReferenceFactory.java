package net.florianschoppmann.issuetracking.asana;

import org.checkerframework.checker.nullness.qual.Nullable;

@FunctionalInterface
interface ReferenceFactory {
    AsanaReference createReference(String href, String type, @Nullable String gid);
}
