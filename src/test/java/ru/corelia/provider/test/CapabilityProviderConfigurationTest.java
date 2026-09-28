package ru.corelia.provider.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import ru.corelia.config.CoreliaRuntimeConfig;

/** Проверяет независимый выбор provider для capability во время миграции. */
class CapabilityProviderConfigurationTest {
    @Test
    void usesCapabilityPropertyBeforeLegacyProvider() {
        var environment = new MockEnvironment()
                .withProperty("corelia.provider", "platform-v")
                .withProperty("corelia.provider.binary-storage", "s3");

        var config = new CoreliaRuntimeConfig(environment);

        assertEquals("platform-v", config.provider("documents"));
        assertEquals("s3", config.provider("binary-storage"));
    }

    @Test
    void environmentCapabilitySettingHasHighestPriority() {
        var environment = new MockEnvironment()
                .withProperty("corelia.provider.workflow", "platform-v")
                .withProperty("CORELIA_PROVIDER_WORKFLOW", "flowable");

        assertEquals("flowable", new CoreliaRuntimeConfig(environment).provider("workflow"));
    }
}
