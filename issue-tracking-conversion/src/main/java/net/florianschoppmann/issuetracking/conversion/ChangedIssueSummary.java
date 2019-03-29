package net.florianschoppmann.issuetracking.conversion;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public class ChangedIssueSummary {
    public @Nullable String issueKey;
    public @Nullable String oldSummary;
    public @Nullable String newSummary;

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        ChangedIssueSummary other = (ChangedIssueSummary) otherObject;
        return Objects.equals(issueKey, other.issueKey)
            && Objects.equals(oldSummary, other.oldSummary)
            && Objects.equals(newSummary, other.newSummary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issueKey, oldSummary, newSummary);
    }
}
