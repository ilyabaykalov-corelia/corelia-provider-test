package ru.corelia.provider.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import ru.corelia.auth.PermissionChecker;
import ru.corelia.auth.AuthKeyProvider;
import ru.corelia.auth.AuthIdentityProvider;
import ru.corelia.provider.AttachmentCatalog;
import ru.corelia.provider.BinaryStorage;
import ru.corelia.provider.DocumentStore;
import ru.corelia.provider.DocumentTypeProvider;
import ru.corelia.provider.DocumentVersionStore;
import ru.corelia.provider.PermissionProvider;
import ru.corelia.provider.TaskProvider;
import ru.corelia.provider.WorkflowProvider;
import ru.corelia.provider.model.BinaryStoreRequest;
import ru.corelia.provider.model.StorageReference;
import ru.corelia.provider.model.StoredFile;
import ru.corelia.auth.AuthContext;
import ru.corelia.provider.ProviderCapability;
import ru.corelia.provider.ProviderDescriptor;

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
    @Bean BinaryStorage binaryStorage(@org.springframework.beans.factory.annotation.Qualifier("coreliaRuntimeConfig") ru.corelia.config.CoreliaRuntimeConfig config) {
        return "test-binary".equals(config.provider("binary-storage"))
                ? new TestBinaryStorage() : capability(BinaryStorage.class);
    }
    @Bean AttachmentCatalog attachmentCatalog() { return capability(AttachmentCatalog.class); }
    @Bean PermissionProvider permissionProvider() { return capability(PermissionProvider.class); }
    @Bean ProviderDescriptor providerDescriptor() { ProviderDescriptor descriptor = new ProviderDescriptor() {
        public String id() { return "test"; }
        public java.util.Set<ProviderCapability> capabilities() { return java.util.EnumSet.allOf(ProviderCapability.class); }
    }; return descriptor; }
    @Bean ProviderDescriptor testBinaryProviderDescriptor() { return new ProviderDescriptor() {
        public String id() { return "test-binary"; }
        public java.util.Set<ProviderCapability> capabilities() { return java.util.EnumSet.of(ProviderCapability.BINARY_STORAGE); }
    }; }
    @Bean AuthKeyProvider authKeyProvider() { return () -> ""; }
    @Bean AuthIdentityProvider authIdentityProvider() { return new AuthIdentityProvider() { public String issuer() { return "test"; } public java.util.Set<String> audiences() { return java.util.Set.of("corelia-web"); } }; }
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

    /** Отдельная fixture implementation для проверки mixed-provider binding. */
    private static final class TestBinaryStorage implements BinaryStorage {
        private final java.util.Map<String, byte[]> files = new java.util.LinkedHashMap<>();
        public StoredFile store(BinaryStoreRequest request, java.io.InputStream content, AuthContext auth) {
            try {
                String reference = "test-binary://" + request.attachmentId();
                byte[] bytes = content.readAllBytes();
                files.put(reference, bytes);
                return new StoredFile(new StorageReference(reference), request.checksum(), bytes.length, request.contentType());
            } catch (IOException error) { throw new IllegalStateException("Не удалось сохранить test binary", error); }
        }
        public java.io.InputStream read(StorageReference reference, AuthContext auth) {
            byte[] bytes = files.get(reference.value());
            if (bytes == null) throw new IllegalArgumentException("Test binary не найден");
            return new ByteArrayInputStream(bytes);
        }
    }
}
