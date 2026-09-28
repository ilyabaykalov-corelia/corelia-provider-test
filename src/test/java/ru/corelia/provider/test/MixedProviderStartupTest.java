package ru.corelia.provider.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import ru.corelia.provider.BinaryStorage;
import ru.corelia.provider.ProviderCapability;
import ru.corelia.provider.ProviderRegistry;
import ru.corelia.provider.model.BinaryStoreRequest;

/** Проверяет startup с разными provider для documents и binary storage. */
class MixedProviderStartupTest {
    @Test
    void startsWithIndependentDocumentAndBinaryProviders() throws Exception {
        String configPath = java.nio.file.Path.of("..").toAbsolutePath().normalize()
                .resolve("../sber-npf-corelia-config").normalize().toString();
        try (var context = new SpringApplicationBuilder(ru.corelia.documents.DocumentApplication.class)
                .sources(TestProviderConfiguration.class)
                .properties("spring.main.web-application-type=none", "corelia.provider=test",
                        "corelia.provider.binary-storage=test-binary", "CORELIA_CONFIG_PATH=" + configPath)
                .run()) {
            var registry = context.getBean(ProviderRegistry.class);
            assertEquals("test", registry.provider(ProviderCapability.DOCUMENTS).id());
            assertEquals("test-binary", registry.provider(ProviderCapability.BINARY_STORAGE).id());
            var storage = context.getBean(BinaryStorage.class);
            var stored = storage.store(new BinaryStoreRequest("doc", "file", "file.txt", "text/plain", 4, "sum"),
                    new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)), null);
            assertEquals("data", new String(storage.read(stored.reference(), null).readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
