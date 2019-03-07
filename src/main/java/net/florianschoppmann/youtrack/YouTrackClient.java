package net.florianschoppmann.youtrack;

import net.florianschoppmann.youtrack.Attachments.Attachment;
import net.florianschoppmann.youtrack.rest.Issue;
import net.florianschoppmann.youtrack.rest.IssueTag;
import net.florianschoppmann.youtrack.rest.User;
import net.florianschoppmann.youtrack.rest.UserGroup;
import net.florianschoppmann.youtrack.restold.Error;
import net.florianschoppmann.youtrack.restold.ImportReport;
import net.florianschoppmann.youtrack.restold.Issues;
import net.florianschoppmann.youtrack.restold.Search;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

public final class YouTrackClient {
    private static final int BATCH_SIZE = 5;

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

    private <T> ImportReport batch(URI targetUri, boolean dryRun, int size,
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
        return batch(targetUri, dryRun, issues.getIssue().size(), (start, end) -> {
            var subIssues = new Issues();
            subIssues.getIssue().addAll(issues.getIssue().subList(start, end));
            return subIssues;
        });
    }

    public ImportReport importLinks(net.florianschoppmann.youtrack.restold.List links, boolean dryRun) {
        URI targetUri = baseUri.resolve(URI.create("rest/import/links"));
        return batch(targetUri, dryRun, links.getLink().size(), (start, end) -> {
            var subLinks = new net.florianschoppmann.youtrack.restold.List();
            subLinks.getLink().addAll(links.getLink().subList(start, end));
            return subLinks;
        });
    }

    /**
     * Imports the given attachment.
     *
     * @param attachment the attachment
     * @param projectAbbrev abbreviation of project
     * @param basePath Base path that {@link Attachment#getPath()} will be resolved against, if it is a relative path.
     *      This parameter has no effect if that function returns an absolute path.
     * @return the import report
     */
    public ImportReport importAttachment(Attachment attachment, String projectAbbrev, Path basePath, boolean dryRun) {
        @Nullable String attachmentCreator = attachment.getAuthorLogin();
        @Nullable String attachmentName = attachment.getName();
        @Nullable String attachmentPath = attachment.getPath();
        if (attachmentCreator == null || attachmentName == null || attachmentPath == null) {
            // The assumption is this has been dealt with before (e.g., by appending the attachment link to the
            // issue description).
            return new ImportReport();
        }

        String issue = projectAbbrev + '-' + attachment.getTaskNumberInProject();
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
                .queryParam("created", attachment.getCreated())
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

    public Issue updateIssue(Issue issue) {
        URI targetUri = baseUri.resolve(UriBuilder.fromPath("api/issues/{issueID}")
            .resolveTemplate("issueID", issue.getId())
            .build());
        return jaxrsClient.target(targetUri)
            .queryParam("fields", "id,tags")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .buildPost(Entity.json(issue))
            .invoke(Issue.class);
    }

    public User getMe(String fields) {
        URI targetUri = baseUri.resolve(URI.create("api/admin/users/me"));
        return jaxrsClient.target(targetUri)
            .queryParam("fields", fields)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .buildGet()
            .invoke(User.class);
    }

    public List<User> getUsers(String fields) {
        URI targetUri = baseUri.resolve(URI.create("api/admin/users"));
        // In theory, the GenericType type argument could be inferred, but there is a compiler bug in javac (which
        // still exists as of JDK 11.0.2+9):
        // https://bugs.openjdk.java.net/browse/JDK-8203195
        return jaxrsClient.target(targetUri)
            .queryParam("fields", fields)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .buildGet()
            .invoke(new GenericType<List<User>>() { });
    }

    public List<UserGroup> getUserGroups(String fields) {
        URI targetUri = baseUri.resolve(URI.create("api/admin/groups"));
        // In theory, the GenericType type argument could be inferred, but there is a compiler bug in javac (which
        // still exists as of JDK 11.0.2+9):
        // https://bugs.openjdk.java.net/browse/JDK-8203195
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
        String userFields = "id,email,name,tags(id,name,visibleFor(id,name))";
        User myself = getMe(userFields);
        List<User> users = getUsers(userFields);
        SortedMap<String, IssueTag> availableTags = new TreeMap<>();
        var issueTagsCollector = Collectors.toMap(
            (IssueTag issueTag) -> Objects.requireNonNull(issueTag.getName()),
            (IssueTag issueTag) -> issueTag
        );
        availableTags.putAll(
            users.stream()
                .flatMap(user -> user.getTags().stream())
                .filter(issueTag -> !issueTag.getVisibleFor().isEmpty())
                .collect(issueTagsCollector)
        );
        // Note that the current user's tags will override others with possibly the same name. This is on purpose.
        availableTags.putAll(myself.getTags().stream().collect(issueTagsCollector));
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
            .resolveTemplate("tagName", tag.getName())
            .build());
        try {
            jaxrsClient.target(targetUri)
                .queryParam("visibleForGroup", tag.getVisibleForGroup())
                .queryParam("updatableByGroup", tag.getUntagOnResolve())
                .queryParam("untagOnResolve", tag.getUntagOnResolve())
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
