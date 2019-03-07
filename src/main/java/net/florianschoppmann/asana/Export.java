package net.florianschoppmann.asana;

import com.asana.Client;
import com.asana.models.Attachment;
import com.asana.models.Project;
import com.asana.models.Story;
import com.asana.models.Task;
import com.asana.models.User;
import com.asana.requests.CollectionRequest;
import net.florianschoppmann.asana.ExportWarnings.MissingId;
import net.florianschoppmann.asana.ExportWarnings.ReferencingTask;
import net.florianschoppmann.asana.ExportWarnings.TaskWithIncompleteAttachments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public final class Export {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Client asanaClient;
    private final AttachmentDownloader attachmentDownloader;

    /**
     * Pattern to match Asana attachment IDs in story of resource_subtype "attachment_added".
     *
     * As of 2019-03-03, the story text will contain a URL of form
     * {@code https://app.asana.com/app/asana/-/get_asset?asset_id=<ASSET_ID>}, where {@code <ASSET_ID>} is a number.
     */
    private static final Pattern ATTACHMENT_ID_PATTERN = Pattern.compile("asset_id=(\\d+)");

    public Export(Client asanaClient, AttachmentDownloader attachmentDownloader) {
        this.asanaClient = Objects.requireNonNull(asanaClient);
        this.attachmentDownloader = Objects.requireNonNull(attachmentDownloader);
    }

    private final class ExportRequest {
        private final String projectId;
        private final Path attachmentsBasePath;
        private final String taskOptFields;
        private final String storyOptFields;
        private final String attachmentOptFields;
        private final SortedMap<String, WrappedTask> idToTaskMap = new TreeMap<>();
        private final SortedMap<String, WrappedUser> idToUserMap = new TreeMap<>();
        private final SortedMap<String, SortedSet<WrappedTask>> userIdToOccurrenceMap = new TreeMap<>();
        private final SortedMap<String, SortedSet<WrappedTask>> taskIdToOccurrenceMap = new TreeMap<>();
        private final List<CompletableFuture<Path>> downloads = new ArrayList<>();

        private ExportRequest(String projectId, Path attachmentsBasePath, Options options) {
            this.projectId = projectId;
            this.attachmentsBasePath = attachmentsBasePath;

            SortedSet<String> taskOptFieldsSet = new TreeSet<>(options.requestedTaskFields);
            taskOptFieldsSet.addAll(Arrays.asList("created_at", "assignee.email", "followers.email", "html_notes",
                "likes.user.email", "name", "parent", "resource_type", "resource_subtype"));
            taskOptFields = String.join(",", taskOptFieldsSet);

            SortedSet<String> storyOptFieldsSet = new TreeSet<>(options.requestedStoryFields);
            storyOptFieldsSet.addAll(Arrays.asList("created_at", "created_by.email", "html_text", "likes.user.email",
                "resource_type", "resource_subtype", "text"));
            storyOptFields = String.join(",", storyOptFieldsSet);

            SortedSet<String> attachmentFieldsSet = new TreeSet<>(Arrays.asList("created_at", "download_url", "host",
                "name", "view_url"));
            attachmentOptFields = String.join(",", attachmentFieldsSet);
        }

        CollectionRequest<Task> queryTasks(CollectionRequest<Task> request) {
            return request.query("opt_fields", taskOptFields);
        }

        CollectionRequest<Story> queryStories(CollectionRequest<Story> request) {
            return request.query("opt_fields", storyOptFields);
        }

        CollectionRequest<Attachment> queryAttachments(CollectionRequest<Attachment> request) {
            return request.query("opt_fields", attachmentOptFields);
        }

        private void addPotentiallyMissing(SortedMap<String, SortedSet<WrappedTask>> map, String id,
                WrappedTask referencesByTask) {
            map.computeIfAbsent(id, ignoredUserId -> new TreeSet<>()).add(referencesByTask);
        }

        private void addUser(User user, WrappedTask referencedByTask) {
            idToUserMap.computeIfAbsent(user.id, ignoredUserId -> new WrappedUser(user));
            addPotentiallyMissing(userIdToOccurrenceMap, user.id, referencedByTask);
        }

        private WrappedTask createWrappedTaskAndTrackReferences(Task task, WrappedTask sectionTitle) {
            var wrappedTask = new WrappedTask(task, sectionTitle);
            if (task.parent != null) {
                addPotentiallyMissing(taskIdToOccurrenceMap, task.parent.id, wrappedTask);
            }
            // If we were to import all projects, we should iterate through task.memberships and extract all sections.
            // However, we deliberately ignore memberships in other projects for now!
            if (task.assignee != null) {
                addUser(task.assignee, wrappedTask);
            }
            task.likes.stream().map(like -> like.user).forEach(user -> addUser(user, wrappedTask));
            task.followers.forEach(user -> addUser(user, wrappedTask));
            return wrappedTask;
        }

        private WrappedComment createWrappedCommentAndTrackReferences(Story story, WrappedTask wrappedTask) {
            var wrappedComment = new WrappedComment(story);
            addUser(story.createdBy, wrappedTask);
            // https://asana.com/developers/api-reference/stories says:
            // "Note: This property only exists for stories that provide likes."
            if (story.likes != null) {
                story.likes.stream().map(like -> like.user).forEach(user -> addUser(user, wrappedTask));
            }
            return wrappedComment;
        }

        private WrappedAttachment createWrappedAttachment(Attachment attachment, WrappedTask wrappedTask) {
            var wrappedAttachment = new WrappedAttachment(attachment);
            String taskId = wrappedTask.getTaskId();
            // https://asana.com/developers/api-reference/attachments says:
            // "Note: [download_url] May be null if the attachment is hosted by box. ..."
            if (attachment.downloadUrl != null) {
                Path downloadPath = attachmentDownloader.downloadPath(attachmentsBasePath, taskId, attachment.name);
                CompletableFuture<Path> download = attachmentDownloader.download(
                    attachmentsBasePath, taskId, attachment.name, attachment.downloadUrl);
                downloads.add(download);
                wrappedAttachment.setDownload(downloadPath, download);
            }
            return wrappedAttachment;
        }

        private void assignStoryAndAttachmentNumbers(WrappedTask wrappedTask) {
            int nextCommentNumberInTask = 1;
            for (WrappedComment story : wrappedTask.getComments()) {
                story.setNumberInTask(nextCommentNumberInTask);
                ++nextCommentNumberInTask;
            }
            int nextAttachmentNumberInTask = 1;
            for (WrappedAttachment attachment : wrappedTask.getAttachments()) {
                attachment.setNumberInTask(nextAttachmentNumberInTask);
                ++nextAttachmentNumberInTask;
            }
        }

        private void processStorySubtype(WrappedComment wrappedComment, ReferenceFactory referenceFactory,
                SortedMap<String, WrappedAttachment> idToAttachmentMap) {
            Story story = wrappedComment.getStory();
            if ("comment_added".equals(story.resourceSubtype)) {
                wrappedComment.setMarkdownText(
                    MarkdownBuilder.htmlToMarkdown(story.htmlText, referenceFactory));
            } else if ("attachment_added".equals(story.resourceSubtype)) {
                Matcher matcher = ATTACHMENT_ID_PATTERN.matcher(story.text);
                boolean foundId = matcher.find();
                if (!foundId) {
                    log.warn("Could not extract attachment ID from story of subtype 'attachment_added'. Text is: {}",
                        story.text);
                    return;
                }

                // The following may legitimately return null. E.g., if an attachment was later deleted.
                @Nullable WrappedAttachment attachment = idToAttachmentMap.get(matcher.group(1));
                if (attachment != null) {
                    attachment.setCreatedBy(idToUserMap.get(story.createdBy.id));
                }
            }
        }

        private void processTasks(CollectionRequest<Task> request, int level) throws IOException {
            @Nullable WrappedTask sectionTitle = null;
            for (Task task : CollectionRequestIterable.of(request)) {
                if (level == 0 && task.parent != null) {
                    // A project can also contain subtasks. However, the canonical location is the one defined by the
                    // parent relationship. We therefore ignore all subtasks here (that have a parent task).
                    continue;
                }

                WrappedTask wrappedTask = createWrappedTaskAndTrackReferences(task, sectionTitle);
                if (task.name.endsWith(":")) {
                    sectionTitle = wrappedTask;
                }
                ReferenceFactory referenceFactory = (String href, String type, @Nullable String gid) -> {
                    switch (type) {
                        case "task": addPotentiallyMissing(taskIdToOccurrenceMap, gid, wrappedTask); break;
                        case "user": addPotentiallyMissing(userIdToOccurrenceMap, gid, wrappedTask); break;
                    }
                    return new AsanaReference(href, type, gid);
                };

                wrappedTask.setMarkdownDescription(MarkdownBuilder.htmlToMarkdown(task.htmlNotes, referenceFactory));
                idToTaskMap.put(task.id, wrappedTask);

                SortedMap<String, WrappedAttachment> idToAttachmentMap = new TreeMap<>();
                for (Attachment attachment :
                        CollectionRequestIterable.of(queryAttachments(asanaClient.attachments.findByTask(task.id)))) {
                    WrappedAttachment wrappedAttachment = createWrappedAttachment(attachment, wrappedTask);
                    wrappedTask.addAttachment(wrappedAttachment);
                    idToAttachmentMap.put(attachment.id, wrappedAttachment);
                }

                for (Story story :
                        CollectionRequestIterable.of(queryStories(asanaClient.stories.findByTask(task.id)))) {
                    WrappedComment wrappedComment = createWrappedCommentAndTrackReferences(story, wrappedTask);
                    wrappedTask.addComment(wrappedComment);
                    processStorySubtype(wrappedComment, referenceFactory, idToAttachmentMap);
                }

                assignStoryAndAttachmentNumbers(wrappedTask);

                // Recursion happens here...
                processTasks(queryTasks(asanaClient.tasks.subtasks(wrappedTask.getTaskId())), level + 1);
                log.info("Finished \"{}\" (ID {}).", task.name, wrappedTask.getTaskId());
            }
        }

        private void verifyEmpty(SortedMap<String, SortedSet<WrappedTask>> map, List<MissingId> missingIds) {
            for (Map.Entry<String, SortedSet<WrappedTask>> entry : map.entrySet()) {
                MissingId missingId = new MissingId();
                missingId.setAsanaId(entry.getKey());
                List<ReferencingTask> referencingTasks = missingId.getReferencingTask();
                for (WrappedTask task : entry.getValue()) {
                    var referencingTask = new ReferencingTask();
                    referencingTask.setName(task.getTask().name);
                    referencingTask.setAsanaId(task.getTaskId());
                    referencingTasks.add(referencingTask);
                }
                missingIds.add(missingId);
            }
        }

        private void verifyAttachmentsHaveCreators(Collection<WrappedTask> tasks,
                List<TaskWithIncompleteAttachments> tasksWithIncompleteAttachments) {
            for (WrappedTask task : tasks) {
                var taskWithIncompleteAttachments = new TaskWithIncompleteAttachments();
                taskWithIncompleteAttachments.setTaskNumberInProject(task.getNumberInProject());
                taskWithIncompleteAttachments.setAsanaTaskId(task.getTaskId());
                taskWithIncompleteAttachments.setName(task.getTask().name);
                for (WrappedAttachment attachment : task.getAttachments()) {
                    if (attachment.getCreatedBy() == null) {
                        taskWithIncompleteAttachments.getAttachmentWithoutCreator().add(attachment.getAttachment().name);
                    }
                }
                if (!taskWithIncompleteAttachments.getAttachmentWithoutCreator().isEmpty()) {
                    tasksWithIncompleteAttachments.add(taskWithIncompleteAttachments);
                }
            }
        }

        private Result getTasks() throws IOException {
            // We explicitly ask Asana for all users because rich-text references to users only have the user id, and no
            // email. That is, we may miss certain referenced users otherwise.
            // See: https://asana.com/developers/documentation/getting-started/rich-text
            // Note that as of 2019-03-02, Asana returns an empty list for the "Personal Projects" workspace
            Project project = asanaClient.projects.findById(projectId).query("opt_fields", "workspace").execute();
            for (User user : CollectionRequestIterable.of(asanaClient.users.findByWorkspace(project.workspace.id)
                    .query("opt_fields", "email"))) {
                idToUserMap.put(user.id, new WrappedUser(user));
            }

            processTasks(queryTasks(asanaClient.tasks.findByProject(projectId)), 0);

            // Finally, prepare the result.
            List<WrappedTask> sortedTasks = new ArrayList<>(idToTaskMap.values());
            Collections.sort(sortedTasks);
            int nextNumberInProject = 1;
            for (WrappedTask task : sortedTasks) {
                task.setNumberInProject(nextNumberInProject);
                ++nextNumberInProject;
            }

            SortedMap<String, SortedSet<WrappedTask>> emailToOccurrenceMap = new TreeMap<>();
            for (var entry : userIdToOccurrenceMap.entrySet()) {
                @Nullable WrappedUser user = idToUserMap.get(entry.getKey());
                assert user != null : "Invariant: userIdToOccurrenceMap.keySet() is a subset of idToUserMap.keySet()";
                emailToOccurrenceMap.put(user.getUser().email, entry.getValue());
            }

            ExportWarnings exportWarnings = new ExportWarnings();
            taskIdToOccurrenceMap.keySet().removeAll(idToTaskMap.keySet());
            verifyEmpty(taskIdToOccurrenceMap, exportWarnings.getMissingTask());
            userIdToOccurrenceMap.keySet().removeAll(idToUserMap.keySet());
            verifyEmpty(userIdToOccurrenceMap, exportWarnings.getMissingUser());
            verifyAttachmentsHaveCreators(sortedTasks, exportWarnings.getTaskWithIncompleteAttachments());

            return new Result(sortedTasks, idToTaskMap, idToUserMap, emailToOccurrenceMap, exportWarnings, downloads);
        }
    }

    public Result getTasks(String projectId, Path attachmentsBasePath, Options options) throws IOException {
        ExportRequest exportRequest = new ExportRequest(projectId, attachmentsBasePath, options);
        return exportRequest.getTasks();
    }

    public static final class Options {
        private final TreeSet<String> requestedTaskFields;
        private final TreeSet<String> requestedStoryFields;

        public Options(Collection<String> requestedTaskFields, Collection<String> requestedStoryFields) {
            this.requestedTaskFields = new TreeSet<>(requestedTaskFields);
            this.requestedStoryFields = new TreeSet<>(requestedStoryFields);
        }
    }

    public static final class Result {
        private final List<WrappedTask> tasks;
        private final SortedMap<String, SortedSet<WrappedTask>> emailToOccurrenceMap;
        private final SortedMap<String, WrappedTask> idToTaskMap;
        private final SortedMap<String, WrappedUser> idToUserMap;
        private final ExportWarnings exportWarnings;
        private final List<CompletableFuture<Path>> downloads;

        private Result(List<WrappedTask> tasks, SortedMap<String, WrappedTask> idToTaskMap,
                SortedMap<String, WrappedUser> idToUserMap, SortedMap<String,
                SortedSet<WrappedTask>> emailToOccurrenceMap, ExportWarnings exportWarnings,
                List<CompletableFuture<Path>> downloads) {
            this.tasks = tasks;
            this.idToUserMap = idToUserMap;
            this.idToTaskMap = idToTaskMap;
            this.emailToOccurrenceMap = emailToOccurrenceMap;
            this.exportWarnings = exportWarnings;
            this.downloads = downloads;
        }

        public List<WrappedTask> getTasks() {
            return Collections.unmodifiableList(tasks);
        }

        public SortedMap<String, SortedSet<WrappedTask>> emailToOccurrenceMap() {
            return Collections.unmodifiableSortedMap(emailToOccurrenceMap);
        }

        public ExportWarnings getExportWarnings() {
            return exportWarnings;
        }

        public SortedMap<String, WrappedTask> getIdToTaskMap() {
            return Collections.unmodifiableSortedMap(idToTaskMap);
        }

        public SortedMap<String, WrappedUser> getIdToUserMap() {
            return Collections.unmodifiableSortedMap(idToUserMap);
        }

        public List<CompletableFuture<Path>> getDownloads() {
            return Collections.unmodifiableList(downloads);
        }
    }
}
