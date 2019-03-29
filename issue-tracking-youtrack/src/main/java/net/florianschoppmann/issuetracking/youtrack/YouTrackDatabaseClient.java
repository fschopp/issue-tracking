package net.florianschoppmann.issuetracking.youtrack;

import jetbrains.exodus.ExodusException;
import jetbrains.exodus.entitystore.Entity;
import jetbrains.exodus.entitystore.EntityIterable;
import jetbrains.exodus.entitystore.PersistentEntityStore;
import jetbrains.exodus.entitystore.PersistentEntityStores;
import jetbrains.exodus.entitystore.StoreTransaction;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import net.florianschoppmann.issuetracking.youtrack.Events.CustomFieldEvent;
import net.florianschoppmann.issuetracking.youtrack.Events.Event;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.file.Path;
import java.util.function.Consumer;

public class YouTrackDatabaseClient {
    /**
     * Argument that needs to be passed to {@link PersistentEntityStores#newInstance(Environment, String)}.
     *
     * This is basically a magic constant. If the wrong name is passed to the previous method, the entity store will
     * appear empty.
     */
    private static final String YOUTRACK_STORE_NAME = "teamsysstore";

    private final Path databasePath;

    public YouTrackDatabaseClient(Path databasePath) {
        this.databasePath = databasePath;
    }

    public void importEvents(Events events, String projectAbbrev) throws YouTrackDatabaseClientException {
        try (
            Environment environment = Environments.newInstance(databasePath.toFile());
            PersistentEntityStore entityStore = PersistentEntityStores.newInstance(environment, YOUTRACK_STORE_NAME)
        ) {
            entityStore.executeInExclusiveTransaction(txn -> new ImportEvents(events, projectAbbrev, txn).run());
        } catch (ExodusException exception) {
            throw new YouTrackDatabaseClientException(
                "Importing events failed because of a problem reported by the Xodus database.", exception);
        } catch (InternalException exception) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw exception.getCause();
        }
    }

    private static class ImportEvents {
        private final Events events;
        private final String projectAbbrev;
        private final StoreTransaction txn;

        private ImportEvents(Events events, String projectAbbrev, StoreTransaction txn) {
            this.events = events;
            this.projectAbbrev = projectAbbrev;
            this.txn = txn;
        }

        private void run() {
            Entity project = find("Project", "shortName", projectAbbrev);
            Entity eventType = find("EventType", "__ENUM_CONST_NAME__", "MODIFY_LINK");
            EntityIterable projectCustomFields = project.getLinks("fields");
            EntityIterable customFieldPrototypes = projectCustomFields.selectDistinct("prototype");
            EntityIterable projectIssues = txn.findLinks("Issue", project, "project");
            for (Event event : events.events) {
                if (!hasRequireFields(event)) {
                    throw new InternalException(String.format("Incomplete event: %s", event));
                } else if (event instanceof CustomFieldEvent) {
                    CustomFieldEvent customFieldEvent = (CustomFieldEvent) event;
                    // Static code analysis does not infer the post-condition of hasRequireFields(), so again:
                    assert customFieldEvent.timestamp != null && customFieldEvent.numberInProject != null
                        && customFieldEvent.field != null && customFieldEvent.author != null
                        && (customFieldEvent.added != null || customFieldEvent.removed != null);

                    Entity issue = findWithin(
                        projectIssues, "Issue", "numberInProject", customFieldEvent.numberInProject,
                        String.format("issue '%s-%s'", projectAbbrev, customFieldEvent.numberInProject)
                    );

                    Entity author = find("User", "login", customFieldEvent.author);

                    Entity customFieldPrototype = findWithin(
                        customFieldPrototypes, "CustomFieldPrototype", "name", customFieldEvent.field,
                        String.format("custom field '%s' in project '%s'", customFieldEvent.field, projectAbbrev)
                    );
                    @Nullable Entity @NonNull [] addedAndRemoved
                        = findAddedAndRemovedEntities(customFieldPrototype, customFieldEvent);

                    var customFieldPrototypeId = (String) customFieldPrototype.getProperty("id");
                    assert customFieldPrototypeId != null : "Entity always has property 'id'.";
                    storePersistentEvent(customFieldPrototypeId, customFieldEvent.timestamp, author, eventType,
                        issue, addedAndRemoved);
                } else {
                    throw new InternalException(String.format(
                        "Unsupported event type: %s", event.getClass().getName()));
                }
            }
        }

        private static boolean hasRequireFields(Event event) {
            boolean[] hasRequiredFields = {true};
            Consumer<Object> test = value -> {
                if (value == null) {
                    hasRequiredFields[0] = false;
                }
            };
            test.accept(event.timestamp);
            if (event instanceof CustomFieldEvent) {
                CustomFieldEvent customFieldEvent = (CustomFieldEvent) event;
                test.accept(customFieldEvent.numberInProject);
                test.accept(customFieldEvent.field);
                test.accept(customFieldEvent.author);
                if (customFieldEvent.added == null && customFieldEvent.removed == null) {
                    hasRequiredFields[0] = false;
                }
            }
            return hasRequiredFields[0];
        }

        private Entity find(String entityType, String propertyName, Comparable<?> value) {
            @Nullable Entity entity = txn.find(entityType, propertyName, value).getFirst();
            if (entity == null) {
                throw new InternalException(String.format(
                    "Could not find entity type '%s' with '%s' = %s", entityType, propertyName, value
                ));
            }
            return entity;
        }

        private Entity findWithin(EntityIterable entities, String entityType, String propertyName, Comparable<?> value,
                String error) {
            @Nullable Entity entity = entities.intersect(txn.find(entityType, propertyName, value)).getFirst();
            if (entity == null) {
                throw new InternalException("Could not find " + error + '.');
            }
            return entity;
        }

        private @Nullable Entity @NonNull [] findAddedAndRemovedEntities(Entity customFieldPrototype,
                CustomFieldEvent event) {
            @Nullable Entity bundleProjectCustomField = txn.findLinks(
                "BundleProjectCustomField", customFieldPrototype, "prototype").getFirst();
            @Nullable Entity userProjectCustomField = null;
            if (bundleProjectCustomField == null) {
                userProjectCustomField = txn.findLinks(
                    "UserProjectCustomField", customFieldPrototype, "prototype").getFirst();
            }
            if (bundleProjectCustomField == null && userProjectCustomField == null) {
                throw new InternalException(String.format("Custom field '%s' in project '%s' "
                    + "is not of type user or of a bundle type (enum, state, ...).",
                    event.field, projectAbbrev));
            }

            @Nullable Entity addedEntity = null;
            @Nullable Entity removedEntity = null;
            if (bundleProjectCustomField != null) {
                Entity bundle = bundleProjectCustomField.getLink("bundle");
                assert bundle != null : "Entity of type 'BundleProjectCustomField' always has property 'bundle'.";
                EntityIterable bundleElements = bundle.getLinks("children");
                for (Entity bundleElement: bundleElements) {
                    var name = (String) bundleElement.getProperty("name");
                    assert name != null : "Every bundle element has a name.";
                    if (name.equals(event.added)) {
                        addedEntity = bundleElement;
                    } else if (name.equals(event.removed)) {
                        removedEntity = bundleElement;
                    }
                }
            } else {
                if (event.added != null) {
                    addedEntity = find("User", "login", event.added);
                }
                if (event.removed != null) {
                    removedEntity = find("User", "login", event.removed);
                }
            }
            return new Entity[]{addedEntity, removedEntity};
        }

        private void storePersistentEvent(String memberName, long timestamp, Entity author, Entity type,
                Entity targetIssue, @Nullable Entity @NonNull [] addedAndRemoved) {
            Entity persistentEvent = txn.newEntity("PersistentEvent");
            persistentEvent.setProperty("memberName", memberName);
            persistentEvent.setProperty("timestamp", timestamp);
            persistentEvent.setLink("author", author);
            persistentEvent.setLink("type", type);
            persistentEvent.setLink("target_Issue", targetIssue);
            if (addedAndRemoved[0] != null) {
                persistentEvent.setLink("added_" + memberName, addedAndRemoved[0]);
            }
            if (addedAndRemoved[1] != null) {
                persistentEvent.setLink("removed_" + memberName, addedAndRemoved[1]);
            }
        }
    }

    private static class InternalException extends RuntimeException {
        private static final long serialVersionUID = -448459732241905957L;

        private InternalException(String message) {
            super(new YouTrackDatabaseClientException(message));
        }

        @Override
        public synchronized YouTrackDatabaseClientException getCause() {
            return (YouTrackDatabaseClientException) super.getCause();
        }
    }
}
