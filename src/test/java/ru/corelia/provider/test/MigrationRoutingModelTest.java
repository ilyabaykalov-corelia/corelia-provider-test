package ru.corelia.provider.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import ru.corelia.provider.model.ProcessInstance;
import ru.corelia.provider.model.StorageReference;

/** Проверяет provider-neutral поля, необходимые для маршрутизации legacy данных. */
class MigrationRoutingModelTest {
    @Test
    void preservesExplicitProcessEngine() {
        assertEquals("flowable", new ProcessInstance("process-1", "document-1", "ACTIVE", "flowable").engine());
    }

    @Test
    void extractsOpaqueStorageReferenceScheme() {
        assertEquals("corelia-blob", new StorageReference("corelia-blob://blob-1").scheme());
    }
}
