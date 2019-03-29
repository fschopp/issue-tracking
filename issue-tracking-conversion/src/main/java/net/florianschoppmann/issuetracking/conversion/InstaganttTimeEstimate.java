package net.florianschoppmann.issuetracking.conversion;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class InstaganttTimeEstimate {
    private static final Pattern SUMMARY_WITH_ESTIMATE_PATTERN = Pattern.compile("\\s*\\[(\\d+)]\\s*$");

    private final String taskName;
    private final @Nullable Duration timeEstimate;

    InstaganttTimeEstimate(String taskName) {
        Matcher matcher = SUMMARY_WITH_ESTIMATE_PATTERN.matcher(taskName);
        if (matcher.find()) {
            this.taskName = taskName.substring(0, taskName.length() - matcher.group().length());
            timeEstimate = Duration.ofHours(Integer.valueOf(matcher.group(1)));
        } else {
            this.taskName = taskName;
            timeEstimate = null;
        }
    }

    String getTaskName() {
        return taskName;
    }

    @Nullable
    String getTimeEstimateInMinutes() {
        return timeEstimate == null
            ? null
            : Long.toString(timeEstimate.toMinutes());
    }
}
