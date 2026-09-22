package ru.corelia.provider.test;

import java.util.List;
import ru.corelia.auth.AuthContext;
import ru.corelia.provider.tck.ProviderContractTest;
import ru.corelia.provider.tck.ProviderFixture;
import ru.corelia.provider.model.DocumentSnapshot;
import ru.corelia.provider.model.DocumentVersion;
import ru.corelia.provider.model.WorkflowTask;

/** Применяет общий SPI-контракт к встроенному test provider. */
class TestProviderContractTest extends ProviderContractTest {
    @Override protected ProviderFixture fixture() {
        var provider = new InMemoryTestProvider();
        var allowed = new AuthContext("token", "user", "user", "Пользователь", "", List.of("EDITOR"), "user");
        var denied = new AuthContext("token", "denied", "denied", "Нет доступа", "", List.of("DENY"), "denied");
        return new ProviderFixture() {
            public ru.corelia.provider.DocumentStore documents() { return provider; }
            public ru.corelia.provider.DocumentVersionStore versions() { return provider; }
            public ru.corelia.provider.DocumentTypeProvider documentTypes() { return provider; }
            public ru.corelia.provider.BinaryStorage storage() { return provider; }
            public ru.corelia.provider.AttachmentCatalog attachments() { return provider; }
            public ru.corelia.provider.WorkflowProvider workflows() { return provider; }
            public ru.corelia.provider.TaskProvider tasks() { return provider; }
            public ru.corelia.provider.PermissionProvider permissions() { return provider; }
            public AuthContext allowedAuth() { return allowed; }
            public AuthContext deniedAuth() { return denied; }
            public void seed(DocumentSnapshot document, DocumentVersion version) { provider.seed(document, version); }
            public void seed(WorkflowTask task) { provider.seed(task); }
        };
    }
}
