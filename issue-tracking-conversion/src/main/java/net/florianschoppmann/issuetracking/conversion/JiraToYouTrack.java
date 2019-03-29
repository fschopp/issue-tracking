package net.florianschoppmann.issuetracking.conversion;

import static net.florianschoppmann.issuetracking.util.StringNode.rootOfStrings;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Field;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.IssueFieldId;
import com.atlassian.jira.rest.client.api.domain.IssueLinkType;
import com.atlassian.jira.rest.client.api.domain.User;
import net.florianschoppmann.issuetracking.jira.JiraClient;
import net.florianschoppmann.issuetracking.jira.rest.RemoteIssueLink;
import net.florianschoppmann.issuetracking.jira.rest.RemoteObject;
import net.florianschoppmann.issuetracking.util.ReferencingIssue;
import net.florianschoppmann.issuetracking.util.StringNode;
import net.florianschoppmann.issuetracking.youtrack.CommentUpdates;
import net.florianschoppmann.issuetracking.youtrack.CommentUpdates.CommentUpdate;
import net.florianschoppmann.issuetracking.youtrack.IssueUpdates;
import net.florianschoppmann.issuetracking.youtrack.IssueUpdates.IssueUpdate;
import net.florianschoppmann.issuetracking.youtrack.YouTrackClient;
import net.florianschoppmann.issuetracking.youtrack.rest.IssueComment;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class JiraToYouTrack {
    private static final Set<String> JIRA_REQUESTED_FIELDS_FOR_ISSUES
        = new TreeSet<>(Arrays.asList(IssueFieldId.LINKS_FIELD.id, "parent"));
    private static final StringNode YOUTRACK_REQUESTED_FIELDS_FOR_ISSUES
        = rootOfStrings("description", "numberInProject", "summary");
    private static final StringNode YOUTRACK_REQUESTED_FIELDS_FOR_COMMENTS = rootOfStrings("id", "text");
    private static final StringNode YOUTRACK_REQUESTED_FIELDS_FOR_PROJECTS = rootOfStrings("shortName");

    /**
     * Regular expression for user mentions. See
     * <a href="https://jira.atlassian.com/secure/WikiRendererHelpAction.jspa?section=links">Atlassian Jira: Text
     * Formatting Notation Help</a>
     */
    private static final Pattern JIRA_USER_MENTION_PATTERN = Pattern.compile("\\[~([\\S]+?)]");

    private static final Pattern ISSUE_KEY_PATTERN = Pattern.compile("(\\p{Upper}+)-(\\d+)");

    private final YouTrackClient youTrackClient;
    private final JiraClient jiraClient;
    private final JiraRestClient jiraLowLevelClient;

    public JiraToYouTrack(YouTrackClient youTrackClient, JiraClient jiraClient, JiraRestClient jiraLowLevelClient) {
        this.youTrackClient = Objects.requireNonNull(youTrackClient);
        this.jiraClient = Objects.requireNonNull(jiraClient);
        this.jiraLowLevelClient = Objects.requireNonNull(jiraLowLevelClient);
    }

    private static String issueKey(String projectAbbreviation, int numberInProject) {
        return projectAbbreviation + '-' + numberInProject;
    }

    private static <T, U> SortedMap<T, U> issueListToMap(List<U> issues,
            Function<U, T> issueToNumberInProject) {
        return issues.stream()
            .collect(Collectors.toMap(
                issueToNumberInProject,
                Function.identity(),
                (left, right) -> {
                    throw new IllegalStateException("Unexpected key collision in issue list.");
                },
                TreeMap::new
            ));
    }

    public Result youTrackFromJiraProject(String projectAbbrev) {
        @Nullable String jiraEpicFieldId = StreamSupport
            .stream(jiraLowLevelClient.getMetadataClient().getFields().claim().spliterator(), false)
            .filter(field -> "Epic Link".equals(field.getName()))
            .map(Field::getId)
            .findAny()
            .orElse(null);
        SortedSet<String> jiraUserNames = jiraClient.getUsers().stream()
            .map(User::getName)
            .collect(Collectors.toCollection(TreeSet::new));
        SortedSet<String> youTrackProjects = youTrackClient.getProjects(YOUTRACK_REQUESTED_FIELDS_FOR_PROJECTS).stream()
            .map(project -> project.shortName)
            .collect(Collectors.toCollection(TreeSet::new));
        SortedSet<String> youTrackLoginNames = youTrackClient.getUsers(rootOfStrings("login")).stream()
            .map(user -> user.login)
            .collect(Collectors.toCollection(TreeSet::new));
        SortedSet<String> jiraRequestedFieldsForIssues = new TreeSet<>(JIRA_REQUESTED_FIELDS_FOR_ISSUES);
        Optional.ofNullable(jiraEpicFieldId).ifPresent(jiraRequestedFieldsForIssues::add);
        SortedMap<String, Issue> jiraIssuesMap = issueListToMap(
            jiraClient.getIssues(projectAbbrev, jiraRequestedFieldsForIssues), Issue::getKey);
        SortedMap<String, net.florianschoppmann.issuetracking.youtrack.rest.Issue> youTrackIssuesMap = issueListToMap(
            youTrackClient.getIssues(projectAbbrev, YOUTRACK_REQUESTED_FIELDS_FOR_ISSUES),
            issue -> {
                if (issue.numberInProject == null) {
                    throw new IllegalStateException(
                        "Unknown number in project, even though requested from YouTrack server.");
                }
                return issueKey(projectAbbrev, issue.numberInProject);
            }
        );
        SortedMap<String, List<RemoteIssueLink>> issueKeyToRemoteIssueLinkMap
            = jiraClient.getRemoteIssueLinks(jiraIssuesMap.keySet());

        Request request = new Request(jiraUserNames, jiraIssuesMap, issueKeyToRemoteIssueLinkMap, jiraEpicFieldId,
            youTrackLoginNames, youTrackIssuesMap, youTrackProjects);
        return request.youTrackFromJiraProject();
    }

    private class Request {
        private final SortedSet<String> jiraUserNames;
        private final SortedMap<String, Issue> jiraIssuesMap;
        private final SortedMap<String, List<RemoteIssueLink>> issueKeyToRemoteIssueLinkMap;
        private final @Nullable String jiraEpicFieldId;
        private final SortedSet<String> youTrackLoginNames;
        private final SortedMap<String, net.florianschoppmann.issuetracking.youtrack.rest.Issue> youTrackIssuesMap;
        private final SortedSet<String> youTrackProjects;
        private final SortedMap<String, SortedSet<String>> missingUsers = new TreeMap<>();
        private final SortedMap<String, SortedSet<String>> missingProjects = new TreeMap<>();
        private final Set<String> checkedForeignIssues = new HashSet<>();
        private final ConversionWarnings conversionWarnings = new ConversionWarnings();

        private Request(SortedSet<String> jiraUserNames, SortedMap<String, Issue> jiraIssuesMap,
                SortedMap<String, List<RemoteIssueLink>> issueKeyToRemoteIssueLinkMap,
                @Nullable String jiraEpicFieldId,
                SortedSet<String> youTrackLoginNames,
                SortedMap<String, net.florianschoppmann.issuetracking.youtrack.rest.Issue> youTrackIssuesMap,
                SortedSet<String> youTrackProjects) {
            this.jiraUserNames = jiraUserNames;
            this.jiraIssuesMap = jiraIssuesMap;
            this.issueKeyToRemoteIssueLinkMap = issueKeyToRemoteIssueLinkMap;
            this.jiraEpicFieldId = jiraEpicFieldId;
            this.youTrackLoginNames = youTrackLoginNames;
            this.youTrackIssuesMap = youTrackIssuesMap;
            this.youTrackProjects = youTrackProjects;
        }

        private String updateUserMentions(String originalDescription, String referencingIssue) {
            return JIRA_USER_MENTION_PATTERN.matcher(originalDescription)
                .replaceAll(matchResult -> {
                    String userName = matchResult.group(1);
                    if (jiraUserNames.contains(userName)) {
                        if (!youTrackLoginNames.contains(userName)) {
                            missingUsers.computeIfAbsent(userName, ignoredUserName -> new TreeSet<>())
                                .add(referencingIssue);
                        }
                        return '@' + userName;
                    } else {
                        return matchResult.group();
                    }
                });
        }

        private void ensureSameSummary(Issue jiraIssue,
                net.florianschoppmann.issuetracking.youtrack.rest.Issue youTrackIssue) {
            if (youTrackIssue != null && !jiraIssue.getSummary().equals(youTrackIssue.summary)) {
                var changedIssueSummary = new ChangedIssueSummary();
                changedIssueSummary.issueKey = jiraIssue.getKey();
                changedIssueSummary.oldSummary = jiraIssue.getSummary();
                changedIssueSummary.newSummary = youTrackIssue.summary;
                conversionWarnings.changedIssueSummaries.add(changedIssueSummary);
            }
        }

        private void ensureForeignIssueHasSameSummary(String issueKey) {
            if (checkedForeignIssues.contains(issueKey)) {
                return;
            }

            checkedForeignIssues.add(issueKey);
            ensureSameSummary(
                Objects.requireNonNull(jiraClient.getIssue(issueKey)),
                youTrackClient.getIssue(issueKey, YOUTRACK_REQUESTED_FIELDS_FOR_ISSUES)
            );
        }

        private String project(String issueKey) {
            Matcher matcher = ISSUE_KEY_PATTERN.matcher(issueKey);
            if (!matcher.matches()) {
                throw new IllegalStateException(
                    String.format("Could not parse issue key \"%s\".", issueKey));
            }
            return matcher.group(1);
        }

        private void addIncomingLink(String issueKey, String linkOriginIssueKey, String linkType,
                List<net.florianschoppmann.issuetracking.youtrack.restold.List.Link> links) {
            String linkOriginProject = project(linkOriginIssueKey);
            if (youTrackProjects.contains(linkOriginProject)) {
                if (!linkOriginProject.equals(project(issueKey))) {
                    ensureForeignIssueHasSameSummary(linkOriginIssueKey);
                }
            } else {
                missingProjects.computeIfAbsent(linkOriginProject, ignoredProject -> new TreeSet<>()).add(issueKey);
            }

            var link = new net.florianschoppmann.issuetracking.youtrack.restold.List.Link();
            link.setSource(linkOriginIssueKey);
            link.setTarget(issueKey);
            link.setTypeName(linkType);
            links.add(link);
        }

        private net.florianschoppmann.issuetracking.youtrack.restold.List collectLinks(Collection<Issue> jiraIssues) {
            var linksRoot = new net.florianschoppmann.issuetracking.youtrack.restold.List();
            List<net.florianschoppmann.issuetracking.youtrack.restold.List.Link> links = linksRoot.getLink();
            for (Issue issue : jiraIssues) {
                String issueKey = issue.getKey();

                // 1. Add subtask link
                JiraClient.getParentIssueKey(issue)
                    .ifPresent(parentIssueKey -> addIncomingLink(issueKey, parentIssueKey, "Subtask", links));

                // 2. Add epic link
                Optional.ofNullable(jiraEpicFieldId)
                    .flatMap(fieldId -> Optional.ofNullable(issue.getField(fieldId)))
                    .map(IssueField::getValue)
                    .filter(String.class::isInstance)
                    .map(value -> (String) value)
                    .ifPresent(epicIssueKey -> addIncomingLink(issueKey, epicIssueKey, "Subtask", links));

                // 3. Add issue links
                assert IssueLinkType.Direction.values().length == 2;
                Optional.ofNullable(issue.getIssueLinks()).stream()
                    .flatMap(iterable -> StreamSupport.stream(iterable.spliterator(), false))
                    .filter(issueLink -> issueLink.getIssueLinkType().getDirection() == IssueLinkType.Direction.INBOUND)
                    .forEach(issueLink -> addIncomingLink(issueKey, issueLink.getTargetIssueKey(),
                        issueLink.getIssueLinkType().getName(), links));
            }
            return linksRoot;
        }

        private String appendRemoteIssueLinks(String description, Collection<RemoteIssueLink> remoteIssueLinks) {
            StringBuilder stringBuilder = new StringBuilder(description);
            boolean first = true;
            for (RemoteIssueLink remoteIssueLink : remoteIssueLinks) {
                @Nullable RemoteObject remoteObject = remoteIssueLink.object;
                if (remoteObject == null) {
                    continue;
                }
                // Note that the YouTrack JIRA importer currently produces YouTrack Wiki, not Markdown
                if (first) {
                    first = false;
                    stringBuilder.append("\n\n{html}<hr/>{html}\n==Links==\n");
                }
                stringBuilder.append("- ");
                Optional<String> iconUrl = Optional.ofNullable(remoteObject.icon)
                    .map(icon -> icon.url16x16)
                    .filter(Predicate.not(String::isBlank));
                if (iconUrl.isPresent()) {
                    stringBuilder.append(
                        "{html}<img src=\"").append(iconUrl.get()).append("\" width=\"16\" height=\"16\" />{html} [");
                } else {
                    stringBuilder.append('[');
                }
                stringBuilder.append(remoteObject.title).append('|').append(remoteObject.url).append("]\n");
            }
            return stringBuilder.toString();
        }

        private String emptyStringIfNull(@Nullable String string) {
            return string != null
                ? string
                : "";
        }

        private IssueUpdates collectIssueUpdates() {
            var issueUpdatesRoot = new IssueUpdates();
            for (var entry : youTrackIssuesMap.entrySet()) {
                String issueKey = entry.getKey();
                net.florianschoppmann.issuetracking.youtrack.rest.Issue originalYouTrackIssue = entry.getValue();
                List<RemoteIssueLink> remoteIssueLinks
                    = issueKeyToRemoteIssueLinkMap.getOrDefault(issueKey, Collections.emptyList());

                String originalDescription = emptyStringIfNull(originalYouTrackIssue.description);
                String newDescription
                    = appendRemoteIssueLinks(updateUserMentions(originalDescription, issueKey), remoteIssueLinks);

                if (!newDescription.isEmpty() && !newDescription.equals(originalDescription)) {
                    var updatedYouTrackIssue = new net.florianschoppmann.issuetracking.youtrack.rest.Issue();
                    updatedYouTrackIssue.description = newDescription;
                    var issueUpdate = new IssueUpdate();
                    issueUpdate.issueKey = issueKey;
                    issueUpdate.issue = updatedYouTrackIssue;
                    issueUpdatesRoot.issueUpdates.add(issueUpdate);
                }
            }
            return issueUpdatesRoot;
        }

        private CommentUpdates collectCommentUpdates() {
            var commentUpdatesRoot = new CommentUpdates();
            for (String issueKey : youTrackIssuesMap.keySet()) {
                List<IssueComment> issueComments
                    = youTrackClient.getIssueComments(issueKey, YOUTRACK_REQUESTED_FIELDS_FOR_COMMENTS);
                for (IssueComment issueComment : issueComments) {
                    String oldText = emptyStringIfNull(issueComment.text);
                    String newText = updateUserMentions(oldText, issueKey);
                    if (!newText.isEmpty() && !newText.equals(oldText)) {
                        var updatedComment = new IssueComment();
                        updatedComment.text = newText;
                        var commentUpdate = new CommentUpdate();
                        commentUpdate.issueKey = issueKey;
                        commentUpdate.commentId = issueComment.id;
                        commentUpdate.issueComment = updatedComment;
                        commentUpdatesRoot.commentUpdates.add(commentUpdate);
                    }
                }
            }
            return commentUpdatesRoot;
        }

        private void collectWarnings() {
            for (Map.Entry<String, SortedSet<String>> entry : missingUsers.entrySet()) {
                var missingUser = new MissingUser();
                missingUser.userId = entry.getKey();
                for (String issueKey : entry.getValue()) {
                    var referencingTask = new ReferencingIssue();
                    referencingTask.sourceIssueKey = issueKey;
                    missingUser.referencingIssues.add(referencingTask);
                }
                conversionWarnings.missingUsers.add(missingUser);
            }
        }

        Result youTrackFromJiraProject() {
            SortedSet<String> missingIssueKeys = new TreeSet<>(jiraIssuesMap.keySet());
            missingIssueKeys.removeAll(youTrackIssuesMap.keySet());
            conversionWarnings.missingIssueKeys.addAll(missingIssueKeys);
            for (Map.Entry<String, Issue> entry : jiraIssuesMap.entrySet()) {
                String issueKey = entry.getKey();
                Issue jiraIssue = entry.getValue();

                net.florianschoppmann.issuetracking.youtrack.rest.Issue youTrackIssue = youTrackIssuesMap.get(issueKey);
                ensureSameSummary(jiraIssue, youTrackIssue);
            }

            net.florianschoppmann.issuetracking.youtrack.restold.List links = collectLinks(jiraIssuesMap.values());
            IssueUpdates issueUpdates = collectIssueUpdates();
            CommentUpdates commentUpdates = collectCommentUpdates();
            collectWarnings();
            return new Result(links, issueUpdates, commentUpdates, conversionWarnings);
        }
    }

    public static final class Result {
        private final net.florianschoppmann.issuetracking.youtrack.restold.List links;
        private final IssueUpdates issueUpdates;
        private final CommentUpdates commentUpdates;
        private final ConversionWarnings conversionWarnings;

        private Result(net.florianschoppmann.issuetracking.youtrack.restold.List links, IssueUpdates issueUpdates,
                CommentUpdates commentUpdates, ConversionWarnings conversionWarnings) {
            this.links = links;
            this.issueUpdates = issueUpdates;
            this.commentUpdates = commentUpdates;
            this.conversionWarnings = conversionWarnings;
        }

        public net.florianschoppmann.issuetracking.youtrack.restold.List getLinks() {
            return links;
        }

        public IssueUpdates getIssueUpdates() {
            return issueUpdates;
        }

        public CommentUpdates getCommentUpdates() {
            return commentUpdates;
        }

        public ConversionWarnings getConversionWarnings() {
            return conversionWarnings;
        }
    }
}
