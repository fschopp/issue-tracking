package net.florianschoppmann.asana;

import javax.annotation.Nullable;

public final class AsanaReference {
    private final String href;
    private final String type;
    @Nullable private final String gid;

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

    @Nullable
    public String getGid() {
        return gid;
    }
}
