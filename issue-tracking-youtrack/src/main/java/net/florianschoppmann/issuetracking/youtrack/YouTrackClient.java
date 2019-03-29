package net.florianschoppmann.issuetracking.youtrack;

import static net.florianschoppmann.issuetracking.util.StringNode.node;
import static net.florianschoppmann.issuetracking.util.StringNode.nodeOfNodes;
import static net.florianschoppmann.issuetracking.util.StringNode.nodeOfStrings;
import static net.florianschoppmann.issuetracking.util.StringNode.rootOfNodes;

import net.florianschoppmann.issuetracking.util.StringNode;
import net.florianschoppmann.issuetracking.youtrack.Attachments.Attachment;
import net.florianschoppmann.issuetracking.youtrack.rest.Issue;
import net.florianschoppmann.issuetracking.youtrack.rest.IssueComment;
import net.florianschoppmann.issuetracking.youtrack.rest.IssueTag;
import net.florianschoppmann.issuetracking.youtrack.rest.Project;
import net.florianschoppmann.issuetracking.youtrack.rest.User;
import net.florianschoppmann.issuetracking.youtrack.rest.UserGroup;
import net.florianschoppmann.issuetracking.youtrack.restold.Error;
import net.florianschoppmann.issuetracking.youtrack.restold.ImportReport;
import net.florianschoppmann.issuetracking.youtrack.restold.Issues;
import net.florianschoppmann.issuetracking.youtrack.restold.Search;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

public final class YouTrackClient {
    private static final int BATCH_SIZE = 50;

    private final Client jaxrsClient;
    private final URI baseUri;
    private final String accessToken;

    public YouTrackClient(Client jaxrsClient, URI baseUri, String accessToken) {
        this.jaxrsClient = Objects.requireNonNull(jaxrsClient);
        this.baseUri = Objects.requireNonNull(baseUri);
        this.accessToken = Objects.requireNonNull(accessToken);
    }

    private static ImportReport invocationToImportReport(Invocation invocation) {
        try {
            return invocation.invoke(ImportReport.class);
        } catch (WebApplicationException exception) {
            Response response = exception.getResponse();
            if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                // YouTrack 2018.4 returns this status code together with a detailed import report
                return response.readEntity(ImportReport.class);
            } else {
                throw exception;
            }
        }
    }

    private <T> ImportReport batchImport(URI targetUri, boolean dryRun, int size,
            BiFunction<Integer, Integer, T> batchProvider) {
        ImportReport importReport = new ImportReport();
        int numBatches = (size - 1) / BATCH_SIZE + 1;
        for (int i = 0; i < numBatches; ++i) {
            int start = i * BATCH_SIZE;
            int end = Math.min(size, start + BATCH_SIZE);
            T batch = batchProvider.apply(start, end);
            Invocation invocation = jaxrsClient.target(targetUri)
                .queryParam("test", dryRun)
                .request(MediaType.APPLICATION_XML_TYPE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .buildPut(Entity.xml(batch));
            ImportReport batchImportReport = invocationToImportReport(invocation);
            importReport.getItem().addAll(batchImportReport.getItem());
        }
        return importReport;
    }

    public ImportReport importIssues(Issues issues, String projectAbbrev, boolean dryRun) {
        URI targetUri = baseUri.resolve(UriBuilder.fromPath("rest/import/{project}/issues")
            .resolveTemplate("project", projectAbbrev)
            .build());
        return batchImport(targetUri, dryRun, issues.getIssue().size(), (start, end) -> {
            var subIssues = new Issues();
            subIssues.getIssue().addAll(issues.getIssue().subList(start, end));
            return subIssues;
        });
    }

    public ImportReport importLinks(net.florianschoppmann.issuetracking.youtrack.restold.List links, boolean dryRun) {
        URI targetUri = baseUri.resolve(URI.create("rest/import/links"));
        return batchImport(targetUri, dryRun, links.getLink().size(), (start, end) -> {
            var subLinks = new net.florianschoppmann.issuetracking.youtrack.restold.List();
            subLinks.getLink().addAll(links.getLink().subList(start, end));
            return subLinks;
        });
    }

    /**
     * Imports the given attachment.
     *
     * @param attachment the attachment
     * @param projectAbbrev abbreviation of project
     * @param basePath Base path that {@link Attachment#path} will be resolved against, if it is a relative path.
     *      This parameter has no effect if that function returns an absolute path.
     * @return the import report
     */
    public ImportReport importAttachment(Attachment attachment, String projectAbbrev, Path basePath, boolean dryRun) {
        @Nullable String attachmentCreator = attachment.authorLogin;
        @Nullable String attachmentName = attachment.name;
        @Nullable String attachmentPath = attachment.path;
        if (attachmentCreator == null || attachmentName == null || attachmentPath == null) {
            // The assumption is this has been dealt with before (e.g., by appending the attachment link to the
            // issue description).
            return new ImportReport();
        }

        String issue = projectAbbrev + '-' + attachment.taskNumberInProject;
        try (MultiPart multiPart = new MultiPart(MediaType.MULTIPART_FORM_DATA_TYPE)) {
            Path sourcePath = Paths.get(attachmentPath);
            File sourceFile = sourcePath.isAbsolute()
                ? sourcePath.toFile()
                : basePath.resolve(sourcePath).toFile();
            multiPart.bodyPart(new FileDataBodyPart(attachmentName, sourceFile));
            URI targetUri = baseUri.resolve(UriBuilder.fromPath("rest/import/{issue}/attachment")
                .resolveTemplate("issue", issue).build());
            Invocation invocation = jaxrsClient.target(targetUri)
                .queryParam("authorLogin", attachmentCreator)
                .queryParam("created", attachment.created)
                .queryParam("test", dryRun)
                .request(MediaType.APPLICATION_XML_TYPE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .buildPost(Entity.entity(multiPart, multiPart.getMediaType()));
            ImportReport importReport = invocationToImportReport(invocation);
            // Unfortunately, for attachments, the import report returned by YouTrack is a bit scarce...
            if (importReport.getItem().size() == 1) {
                ImportReport.Item importReportItem = importReport.getItem().get(0);
                if (!importReportItem.isImported() && importReportItem.getId() == null) {
                    importReportItem.setId(issue + ':' + attachmentName);
                }
            }
            return importReport;
        } catch (IOException exception) {
            throw new ProcessingException(exception);
        }
    }

    /**
     * Subclass of {@link GenericType} in order to pass the generic type argument {@code List<Issue>}.
     *
     * Instead of passing {@link #INSTANCE} to functions like {@link Invocation#invoke(GenericType)}, we could also
     * write {@code new GenericType<>() { }}. In that expression, the {@link GenericType} type argument could be
     * inferred, but there is a compiler bug in javac that still exists as of JDK 11.0.2+9:
     * <a href="https://bugs.openjdk.java.net/browse/JDK-8203195">JDK-8203195</a>
     * The workaround is to either write {@code new GenericType<List<Issue>>() { }} or to use this class.
     *
     * This class will serve as a reminder of this problem...
     */
    private static class IssueListGenericType extends GenericType<List<Issue>> {
        private static final IssueListGenericType INSTANCE = new IssueListGenericType();
    }

    private <T> List<T> batchGet(URI targetUri, StringNode fields,
            Function<WebTarget, WebTarget> additionalWebTargetSettings, GenericType<List<T>> genericType) {
        List<T> items = new ArrayList<>();
        int startAt = 0;
        while (true) {
            WebTarget basicTarget = jaxrsClient.target(targetUri)
                .queryParam("fields", fields)
                .queryParam("$skip", startAt)
                .queryParam("$top", BATCH_SIZE);
            WebTarget finalTarget = additionalWebTargetSettings.apply(basicTarget);
            List<T> batchItems = finalTarget
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .buildGet()
                .invoke(genericType);
            items.addAll(batchItems);
            startAt += batchItems.size();
            if (batchItems.isEmpty()) {
                break;
            }
        }
        return items;
    }

    public List<Issue> getIssues(String projectAbbrev, StringNode fields) {
        URI targetUri = baseUri.resolve(URI.create("api/issues"));
        return batchGet(
            targetUri,
            fields,
            webTarget -> webTarget
                // javax.ws.rs.client.WebTarget#queryParam(String, Object...) says:
                // "Stringified values may contain URI template parameters."
                .queryParam("query", "order by: {orderBy} asc in: {project}")
                // Note that "{issue id}" is NOT a template parameter, but instead YouTrack query syntax.
                .resolveTemplate("orderBy", "{issue id}")
                .resolveTemplate("project", projectAbbrev),
            IssueListGenericType.INSTANCE
        );
    }

    public Issue getIssue(String issueKey, StringNode fields) {
        URI targetUri = baseUri.resolve(UriBuilder.fromPath("api/issues/{issueID}")
            .resolveTemplate("issueID", issueKey)
            .build());
        return jaxrsClient.target(targetUri)
            .queryParam("fields", fields)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .buildGet()
            // In theory, the GenericType type argument could be inferred, but there is a compiler bug in javac
            // (which still exists as of JDK 11.0.2+9): https://bugs.openjdk.java.net/browse/JDK-8203195
            .invoke(Issue.class);
    }

    public Issue updateIssue(String issueKey, Issue issue, StringNode fields) {
        URI targetUri = baseUri.resolve(UriBuilder.fromPath("api/issues/{issueID}")
            .resolveTemplate("issueID", issueKey)
            .build());
        return jaxrsClient.target(targetUri)
            .queryParam("fields", fields)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .buildPost(Entity.json(issue))
            .invoke(Issue.class);
    }

    public List<IssueComment> getIssueComments(String issueKey, StringNode fields) {
        URI targetUri = baseUri.resolve(UriBuilder.fromPath("api/issues/{issueID}/comments")
            .resolveTemplate("issueID", issueKey)
            .build());
        return jaxrsClient.target(targetUri)
            .queryParam("fields", fields)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .buildGet()
            // In theory, the GenericType type argument could be inferred, but there is a compiler bug in javac
            // (which still exists as of JDK 11.0.2+9): https://bugs.openjdk.java.net/browse/JDK-8203195
            .invoke(new GenericType<List<IssueComment>>() { });
    }

    public IssueComment updateIssueComment(String issueKey, String commentId, IssueComment issueComment,
            StringNode fields) {
        URI targetUri = baseUri.resolve(UriBuilder.fromPath("api/issues/{issueID}/comments/{commentID}")
            .resolveTemplate("issueID", issueKey)
            .resolveTemplate("commentID", commentId)
            .build());
        return jaxrsClient.target(targetUri)
            .queryParam("fields", fields)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .buildPost(Entity.json(issueComment))
            .invoke(IssueComment.class);
    }

    public List<Project> getProjects(StringNode fields) {
        URI targetUri = baseUri.resolve(URI.create("api/admin/projects"));
        return batchGet(
            targetUri,
            fields,
            Function.identity(),
            new GenericType<List<Project>>() { }
        );
    }

    public User getMe(StringNode fields) {
        URI targetUri = baseUri.resolve(URI.create("api/admin/users/me"));
        return jaxrsClient.target(targetUri)
            .queryParam("fields", fields)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .buildGet()
            .invoke(User.class);
    }

    public List<User> getUsers(StringNode fields) {
        URI targetUri = baseUri.resolve(URI.create("api/admin/users"));
        return jaxrsClient.target(targetUri)
            .queryParam("fields", fields)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .buildGet()
            .invoke(new GenericType<List<User>>() { });
    }

    public List<UserGroup> getUserGroups(StringNode fields) {
        URI targetUri = baseUri.resolve(URI.create("api/admin/groups"));
        return jaxrsClient.target(targetUri)
            .queryParam("fields", fields)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .buildGet()
            .invoke(new GenericType<List<UserGroup>>() { });
    }

    /**
     * Returns a map from tag name to {@link IssueTag} that contains all tags that are not other users' personal tags.
     */
    public SortedMap<String, IssueTag> getAllAvailableTags() {
        StringNode fields = rootOfNodes(node("email"),
            nodeOfNodes("tags", node("id"), node("name"), nodeOfStrings("visibleFor", "id", "name")));
        User myself = getMe(fields);
        List<User> users = getUsers(fields);
        SortedMap<String, IssueTag> availableTags = new TreeMap<>();
        var issueTagsCollector = Collectors.toMap(
            (IssueTag issueTag) -> Objects.requireNonNull(issueTag.name),
            (IssueTag issueTag) -> issueTag
        );
        availableTags.putAll(
            users.stream()
                .map(user -> user.tags)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(issueTag -> issueTag.visibleFor != null)
                .collect(issueTagsCollector)
        );
        // Note that the current user's tags will override others with possibly the same name. This is on purpose.
        availableTags.putAll(
            Optional.ofNullable(myself.tags).stream().flatMap(List::stream).collect(issueTagsCollector));
        return availableTags;
    }

    public Error addTag(Search tag) {
        // Using the old REST API. I cannot find documentation of how to do this with the new REST API, and my guess
        // is rejected by YouTrack 2018.4:
        // curl -H "Authorization: Bearer <token>" \
        //      -H 'Content-Type: application/json' \
        //      -d '{"name" : "foobar", "owner": { "id" : "1-1" }}' \
        //     'https://<youtrack-instance-name>.myjetbrains.com/youtrack/api/admin/users/me/tags?fields=id,name'
        // Response:
        // {
        //     "error": "Bad Request",
        //     "error_description": "Cannot find database entity without id"
        // }
        URI targetUri = baseUri.resolve(UriBuilder.fromPath("rest/user/tag/{tagName}")
            .resolveTemplate("tagName", tag.name)
            .build());
        try {
            jaxrsClient.target(targetUri)
                .queryParam("visibleForGroup", tag.visibleForGroup)
                .queryParam("updatableByGroup", tag.untagOnResolve)
                .queryParam("untagOnResolve", tag.untagOnResolve)
                .request(MediaType.APPLICATION_XML_TYPE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .buildPut(Entity.xml(""))
                .invoke(String.class);
        } catch (WebApplicationException exception) {
            Response response = exception.getResponse();
            if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                // YouTrack 2018.4 returns this status code together with an Error description
                return response.readEntity(Error.class);
            } else {
                throw exception;
            }
        }
        return new Error();
    }
}
