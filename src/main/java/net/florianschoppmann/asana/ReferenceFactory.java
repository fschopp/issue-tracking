package net.florianschoppmann.asana;

import javax.annotation.Nullable;

@FunctionalInterface
interface ReferenceFactory {
    AsanaReference createReference(String href, String type, @Nullable String gid);
}
