package ru.corelia.provider.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import ru.corelia.provider.DocumentStore;

/** Проверяет запуск business services без Platform V в test runtime classpath. */
class TestProviderStartupTest {
    @Test
    void businessServicesStartWithTestProvider() {
        String configPath = java.nio.file.Path.of("..")
                .toAbsolutePath().normalize().resolve("../sber-npf-corelia-config").normalize().toString();
        for (Class<?> application : java.util.List.of(
                ru.corelia.documents.DocumentApplication.class,
                ru.corelia.workflow.WorkflowApplication.class,
                ru.corelia.attachments.AttachmentApplication.class)) {
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder(application)
                    .sources(TestProviderConfiguration.class)
                    .properties("spring.main.web-application-type=none", "corelia.provider=test", "CORELIA_CONFIG_PATH=" + configPath)
                    .run()) {
                assertNotNull(context.getBean(DocumentStore.class));
            }
        }
    }
}
