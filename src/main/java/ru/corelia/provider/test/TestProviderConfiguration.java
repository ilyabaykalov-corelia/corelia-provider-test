package ru.corelia.provider.test;

import java.lang.reflect.InvocationTargetException;
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
    private final InMemoryTestProvider provider = new InMemoryTestProvider();

    @Bean DocumentStore documentStore() { return capability(DocumentStore.class); }
    @Bean DocumentVersionStore documentVersionStore() { return capability(DocumentVersionStore.class); }
    @Bean DocumentTypeProvider documentTypeProvider() { return capability(DocumentTypeProvider.class); }
    @Bean WorkflowProvider workflowProvider() { return capability(WorkflowProvider.class); }
    @Bean TaskProvider taskProvider() { return capability(TaskProvider.class); }
    @Bean BinaryStorage binaryStorage() { return capability(BinaryStorage.class); }
    @Bean AttachmentCatalog attachmentCatalog() { return capability(AttachmentCatalog.class); }
    @Bean PermissionProvider permissionProvider() { return capability(PermissionProvider.class); }
    @Bean PermissionChecker permissionChecker(PermissionProvider provider) { return provider::require; }

    @SuppressWarnings("unchecked")
    private <T> T capability(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, arguments) -> {
            try {
                return method.invoke(provider, arguments);
            } catch (InvocationTargetException error) {
                throw error.getCause();
            }
        });
    }
}
