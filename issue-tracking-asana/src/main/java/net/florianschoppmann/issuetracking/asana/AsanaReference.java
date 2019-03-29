package net.florianschoppmann.issuetracking.asana;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class AsanaReference {
    private final String href;
    private final String type;
    private final @Nullable String gid;

    AsanaReference(String href, String type, @Nullable String gid) {
        this.href = href;
        this.type = type;
        this.gid = gid;
    }

    public String getHref() {
        return href;
    }

    public String getType() {
        return type;
    }

    public @Nullable String getGid() {
        return gid;
    }
}
