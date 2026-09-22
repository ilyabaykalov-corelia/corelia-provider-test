package ru.corelia.provider.test;

import java.lang.reflect.Proxy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import ru.corelia.auth.PermissionChecker;
import ru.corelia.provider.AttachmentCatalog;
import ru.corelia.provider.BinaryStorage;
import ru.corelia.provider.DocumentStore;
import ru.corelia.provider.DocumentTypeProvider;
import ru.corelia.provider.DocumentVersionStore;
import ru.corelia.provider.PermissionProvider;
import ru.corelia.provider.TaskProvider;
import ru.corelia.provider.WorkflowProvider;

/** Минимальный provider для проверки независимого старта Corelia. */
@AutoConfiguration
@ConditionalOnProperty(name = "corelia.provider", havingValue = "test")
public class TestProviderConfiguration {
    @Bean DocumentStore documentStore() { return unsupported(DocumentStore.class); }
    @Bean DocumentVersionStore documentVersionStore() { return unsupported(DocumentVersionStore.class); }
    @Bean DocumentTypeProvider documentTypeProvider() { return unsupported(DocumentTypeProvider.class); }
    @Bean WorkflowProvider workflowProvider() { return unsupported(WorkflowProvider.class); }
    @Bean TaskProvider taskProvider() { return unsupported(TaskProvider.class); }
    @Bean BinaryStorage binaryStorage() { return unsupported(BinaryStorage.class); }
    @Bean AttachmentCatalog attachmentCatalog() { return unsupported(AttachmentCatalog.class); }
    @Bean PermissionProvider permissionProvider() { return unsupported(PermissionProvider.class); }
    @Bean PermissionChecker permissionChecker(PermissionProvider provider) { return provider::require; }
    @SuppressWarnings("unchecked") private static <T> T unsupported(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) return method.getName().equals("toString") ? "test-provider" : null;
            throw new UnsupportedOperationException("Test provider не выполняет " + method.getName());
        });
    }
}
