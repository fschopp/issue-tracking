package net.florianschoppmann.issuetracking.jira;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Field;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.IssueFieldId;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.api.domain.User;
import net.florianschoppmann.issuetracking.jira.rest.RemoteIssueLink;
import org.codehaus.jettison.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

public final class JiraClient {
    /**
     * Fields to pass to {@code searchJql()} when searching for all issue links.
     *
     * {@link com.atlassian.jira.rest.client.api.SearchRestClient#searchJql(String, Integer, Integer, Set)} specifies:
     * "Note that the following fields: summary, issuetype, created, updated, project and status are required."
     * But instead of verifying this and throwing a proper exception, the code would wail only later when parsing
     * the JSON response fails...
     *
     * Our API is a bit more user friendly and adds these fields automatically.
     *
     * @see #getIssues(String, Set)
     */
    private static final Set<String> ISSUES_REQUIRED_SEARCH_FIELDS
        = Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(
            IssueFieldId.SUMMARY_FIELD.id,
            IssueFieldId.ISSUE_TYPE_FIELD.id,
            IssueFieldId.CREATED_FIELD.id,
            IssueFieldId.UPDATED_FIELD.id,
            IssueFieldId.PROJECT_FIELD.id,
            IssueFieldId.STATUS_FIELD.id
        )));

    private static final int BATCH_SIZE = 50;

    private final JiraRestClient jiraRestClient;
    private final Client jaxrsClient;
    private final URI baseUri;
    private final String credentials;

    public JiraClient(JiraRestClient jiraRestClient, Client jaxrsClient, URI baseUri, String username, String password) {
        this.jiraRestClient = Objects.requireNonNull(jiraRestClient);
        this.jaxrsClient = Objects.requireNonNull(jaxrsClient);
        this.baseUri = Objects.requireNonNull(baseUri);
        credentials = Base64.getEncoder().encodeToString((username + ':' + password).getBytes());
    }

    public List<Issue> getIssues(String projectAbbrev, Set<String> fields) {
        // See comments for ISSUES_REQUIRED_SEARCH_FIELDS
        var combinedFields = new TreeSet<>(ISSUES_REQUIRED_SEARCH_FIELDS);
        combinedFields.addAll(fields);
        List<Issue> issues = new ArrayList<>();
        int startAt = 0;
        while (true) {
            SearchResult result = jiraRestClient.getSearchClient()
                .searchJql(String.format("project = %s ORDER BY issuekey ASC", projectAbbrev), BATCH_SIZE, startAt,
                    combinedFields)
                .claim();
            for (Issue issue : result.getIssues()) {
                issues.add(issue);
                ++startAt;
            }
            if (startAt >= result.getTotal()) {
                break;
            }
        }
        return issues;
    }

    public Issue getIssue(String issueKey) {
        return jiraRestClient.getIssueClient().getIssue(issueKey).claim();
    }

    /**
     * Get remote links for a collection of issues.
     */
    public SortedMap<String, List<RemoteIssueLink>> getRemoteIssueLinks(Collection<String> issueKeys) {
        SortedMap<String, List<RemoteIssueLink>> issueKeyToRemoteIssueLinkMap = new TreeMap<>();
        for (String issueKey : issueKeys) {
            URI targetUri = baseUri.resolve(UriBuilder.fromPath("rest/api/3/issue/{issueIdOrKey}/remotelink")
                .resolveTemplate("issueIdOrKey", issueKey)
                .build());
            List<RemoteIssueLink> remoteIssueLinks = jaxrsClient.target(targetUri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                .buildGet()
                .invoke(new GenericType<List<RemoteIssueLink>>() { });
            issueKeyToRemoteIssueLinkMap.put(issueKey, remoteIssueLinks);
        }
        return issueKeyToRemoteIssueLinkMap;
    }

    public List<User> getUsers() {
        List<User> users = new ArrayList<>();
        int startAt = 0;
        while (true) {
            Iterable<User> iterable = jiraRestClient.getUserClient()
                // I could not find official confirmation that '%' is the correct way of doing things. See:
                // https://jira.atlassian.com/browse/JRASERVER-29069
                // https://community.atlassian.com/t5/Jira-questions/Any-way-to-get-all-users-list-using-JIRA-REST-API/qaq-p/518530
                .findUsers("%", startAt, BATCH_SIZE, true, true)
                .claim();
            int batchSize = 0;
            for (User user : iterable) {
                users.add(user);
                ++batchSize;

            }
            startAt += batchSize;
            if (batchSize < BATCH_SIZE) {
                break;
            }
        }
        return users;
    }

    public List<?> getFields() {
        List<Field> fields = new ArrayList<>();
        for (Field field : jiraRestClient.getMetadataClient().getFields().claim()) {
            fields.add(field);
        }
        return fields;
    }

    /**
     * Returns the parent issue for the given issue.
     *
     * Unfortunately, {@link Issue} does not allow access to its parent directly, but the REST API supports it.
     *
     * @see Issue <a href="https://ecosystem.atlassian.net/browse/">JRJC-91</a>
     */
    public static Optional<String> getParentIssueKey(Issue issue) {
        return Optional.ofNullable(issue.getField("parent"))
            .map(IssueField::getValue)
            .filter(JSONObject.class::isInstance)
            .map(object -> (JSONObject) object)
            .flatMap(jsonObject -> Optional.ofNullable(jsonObject.optString("key", null)));
    }
}
