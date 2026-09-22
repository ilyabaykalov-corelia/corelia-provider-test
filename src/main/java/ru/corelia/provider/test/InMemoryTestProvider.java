package ru.corelia.provider.test;

import static ru.corelia.support.Json.object;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ru.corelia.auth.AuthContext;
import ru.corelia.http.ApiException;
import ru.corelia.provider.AttachmentCatalog;
import ru.corelia.provider.BinaryStorage;
import ru.corelia.provider.DocumentStore;
import ru.corelia.provider.DocumentTypeProvider;
import ru.corelia.provider.DocumentVersionStore;
import ru.corelia.provider.PermissionProvider;
import ru.corelia.provider.TaskProvider;
import ru.corelia.provider.WorkflowProvider;
import ru.corelia.provider.model.AttachmentMetadata;
import ru.corelia.provider.model.AvailableDocumentType;
import ru.corelia.provider.model.BinaryStoreRequest;
import ru.corelia.provider.model.DocumentMutation;
import ru.corelia.provider.model.DocumentSearchRequest;
import ru.corelia.provider.model.DocumentSearchResult;
import ru.corelia.provider.model.DocumentSnapshot;
import ru.corelia.provider.model.DocumentVersion;
import ru.corelia.provider.model.DocumentVersionState;
import ru.corelia.provider.model.IdempotencyReceipt;
import ru.corelia.provider.model.ProcessInstance;
import ru.corelia.provider.model.StorageReference;
import ru.corelia.provider.model.StoredFile;
import ru.corelia.provider.model.TaskSearchRequest;
import ru.corelia.provider.model.WorkflowContext;
import ru.corelia.provider.model.WorkflowTask;
import tools.jackson.databind.JsonNode;

/** Небольшой in-memory provider для contract и startup-проверок без внешней платформы. */
public final class InMemoryTestProvider implements DocumentStore, DocumentVersionStore,
        DocumentTypeProvider, WorkflowProvider, TaskProvider, BinaryStorage, AttachmentCatalog, PermissionProvider {
    private final Map<String, DocumentSnapshot> documents = new LinkedHashMap<>();
    private final Map<String, List<DocumentVersion>> versions = new LinkedHashMap<>();
    private final Map<String, AttachmentMetadata> attachments = new LinkedHashMap<>();
    private final Map<String, List<AttachmentMetadata>> attachmentVersions = new LinkedHashMap<>();
    private final Map<String, IdempotencyReceipt> receipts = new LinkedHashMap<>();
    private final Map<String, byte[]> files = new LinkedHashMap<>();
    private final Map<String, ProcessInstance> processes = new LinkedHashMap<>();
    private final Map<String, WorkflowTask> tasks = new LinkedHashMap<>();

    public synchronized void seed(DocumentSnapshot document, DocumentVersion version) {
        documents.put(document.id(), document);
        versions.put(document.id(), new ArrayList<>(List.of(version)));
    }

    public synchronized void seed(WorkflowTask task) { tasks.put(task.id(), task); }

    @Override public synchronized DocumentSearchResult search(DocumentSearchRequest request, AuthContext auth) {
        var matching = documents.values().stream().filter(item -> request.typeCode() == null || request.typeCode().isBlank()
                || request.typeCode().equals(item.typeCode())).toList();
        int from = Math.min(Math.max(request.offset(), 0), matching.size());
        int to = Math.min(from + Math.max(request.limit(), 0), matching.size());
        return new DocumentSearchResult(matching.subList(from, to), matching.size());
    }

    @Override public synchronized DocumentSnapshot get(String typeCode, String documentId, AuthContext auth) {
        var document = requiredDocument(documentId);
        if (!typeCode.equals(document.typeCode())) throw new ApiException(404, "Документ не найден");
        return document;
    }

    @Override public synchronized String documentType(String documentId, AuthContext auth) {
        return requiredDocument(documentId).typeCode();
    }

    @Override public synchronized List<DocumentVersion> documentVersions(String documentId, AuthContext auth) {
        return List.copyOf(versions.getOrDefault(documentId, List.of()));
    }

    @Override public synchronized List<AttachmentMetadata> attachments(String documentId, AuthContext auth) {
        return attachments.values().stream().filter(item -> documentId.equals(item.documentId())).toList();
    }

    @Override public synchronized DocumentVersionState state(String documentType, String documentId, AuthContext auth) {
        var document = get(documentType, documentId, auth);
        var history = documentVersions(documentId, auth);
        var current = history.stream().max(Comparator.comparingInt(DocumentVersion::number))
                .orElseThrow(() -> new ApiException(404, "Версия документа не найдена"));
        return new DocumentVersionState(document, current, history, attachments(documentId, auth));
    }

    @Override public synchronized IdempotencyReceipt receipt(String idempotencyKey, AuthContext auth) {
        return receipts.get(idempotencyKey);
    }

    @Override public synchronized void commit(DocumentMutation mutation, AuthContext auth) {
        var prior = receipts.get(mutation.idempotencyKey());
        if (prior != null) {
            if (!prior.requestHash().equals(mutation.requestHash())) throw new ApiException(409, "Ключ идемпотентности использован для другой команды");
            return;
        }
        var old = documents.get(mutation.documentId());
        if (old != null && old.currentVersion() != mutation.expectedVersion()) throw new ApiException(409, "Документ был изменен конкурентно");
        int currentVersion = mutation.createdVersion() == null ? (old == null ? 0 : old.currentVersion()) : mutation.createdVersion().number();
        String token = "test-" + currentVersion;
        var snapshot = new DocumentSnapshot(mutation.documentId(), mutation.documentType(),
                old == null ? "DRAFT" : old.status(), currentVersion, mutation.attributes(),
                old == null ? auth.id() : old.createdBy(), old == null ? Instant.now() : old.createdAt(), token);
        documents.put(snapshot.id(), snapshot);
        if (mutation.closedVersion() != null) replaceVersion(mutation.closedVersion());
        if (mutation.createdVersion() != null) versions.computeIfAbsent(snapshot.id(), ignored -> new ArrayList<>()).add(mutation.createdVersion());
        if (mutation.retiredAttachment() != null) attachments.remove(mutation.retiredAttachment().id());
        if (mutation.createdAttachment() != null) putAttachment(mutation.createdAttachment());
        if (mutation.idempotencyKey() != null && !mutation.idempotencyKey().isBlank())
            receipts.put(mutation.idempotencyKey(), new IdempotencyReceipt(mutation.requestHash(), mutation.response()));
    }

    @Override public List<AvailableDocumentType> available(AuthContext auth) {
        return List.of(new AvailableDocumentType("TEST", "Тестовый документ"));
    }

    @Override public synchronized ProcessInstance start(WorkflowContext context, AuthContext auth) {
        String id = "process-" + UUID.randomUUID();
        var process = new ProcessInstance(id, context.documentId(), "STARTED");
        processes.put(id, process);
        return process;
    }

    @Override public synchronized ProcessInstance process(String processInstanceId, AuthContext auth) {
        var process = processes.get(processInstanceId);
        if (process == null) throw new ApiException(404, "Процесс не найден");
        return process;
    }

    @Override public synchronized List<WorkflowTask> search(TaskSearchRequest request, AuthContext auth) {
        return tasks.values().stream().filter(task -> request.statuses().isEmpty() || request.statuses().contains(task.status())).toList();
    }

    @Override public synchronized List<WorkflowTask> findByDocument(String documentId, AuthContext auth) {
        return tasks.values().stream().filter(task -> documentId.equals(task.documentId())).toList();
    }

    @Override public synchronized WorkflowTask task(String taskId, AuthContext auth) {
        var task = tasks.get(taskId);
        if (task == null) throw new ApiException(404, "Задача не найдена");
        return task;
    }

    @Override public String roleLabel(String role, AuthContext auth) { return role; }
    @Override public synchronized void start(String taskId, AuthContext auth) { replaceTask(taskId, "STARTED"); }
    @Override public synchronized void complete(String taskId, Map<String, JsonNode> parameters, AuthContext auth) { replaceTask(taskId, "COMPLETED"); }

    @Override public synchronized StoredFile store(BinaryStoreRequest request, InputStream content, AuthContext auth) {
        try {
            String value = "test://" + request.documentId() + "/" + request.attachmentId() + "/" + UUID.randomUUID();
            byte[] data = content.readAllBytes();
            files.put(value, data);
            return new StoredFile(new StorageReference(value), request.checksum(), data.length, request.contentType());
        } catch (IOException error) {
            throw new ApiException(502, "Не удалось сохранить вложение test provider");
        }
    }

    @Override public synchronized InputStream read(StorageReference reference, AuthContext auth) {
        var data = files.get(reference.value());
        if (data == null) throw new ApiException(404, "Файл не найден");
        return new ByteArrayInputStream(data);
    }

    @Override public synchronized AttachmentMetadata find(String attachmentId, AuthContext auth) {
        var item = attachments.get(attachmentId);
        if (item == null) throw new ApiException(404, "Вложение не найдено");
        return item;
    }

    @Override public synchronized List<AttachmentMetadata> attachmentVersions(String attachmentId, AuthContext auth) {
        var item = find(attachmentId, auth);
        return List.copyOf(attachmentVersions.getOrDefault(item.logicalId(), List.of(item)));
    }

    @Override public void require(String permission, AuthContext auth) {
        if (auth.roles().contains("DENY")) throw new ApiException(403, "Недостаточно прав для выполнения действия");
    }

    private DocumentSnapshot requiredDocument(String id) {
        var document = documents.get(id);
        if (document == null) throw new ApiException(404, "Документ не найден");
        return document;
    }

    private void replaceVersion(DocumentVersion next) {
        var history = versions.computeIfAbsent(next.documentId(), ignored -> new ArrayList<>());
        history.removeIf(item -> item.id().equals(next.id()));
        history.add(next);
    }

    private void putAttachment(AttachmentMetadata item) {
        attachments.put(item.id(), item);
        var history = attachmentVersions.computeIfAbsent(item.logicalId(), ignored -> new ArrayList<>());
        history.removeIf(current -> current.id().equals(item.id()));
        history.add(item);
    }

    private void replaceTask(String taskId, String status) {
        var task = task(taskId, null);
        tasks.put(taskId, new WorkflowTask(task.id(), task.documentId(), task.documentType(), status,
                task.assignee(), task.assigneeName(), task.assigneeRole(), task.title(), task.description(),
                task.attributes(), task.actions()));
    }
}
