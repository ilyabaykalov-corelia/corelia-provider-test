package ru.corelia.provider.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import ru.corelia.config.CoreliaRuntimeConfig;
import ru.corelia.provider.ProviderCapability;
import ru.corelia.provider.ProviderDescriptor;
import ru.corelia.provider.ProviderRegistry;
import ru.corelia.provider.ProviderStartupValidator;

/** Проверяет независимое разрешение implementation по capability. */
class ProviderRegistryTest {
    @Test
    void resolvesDifferentProvidersForIndependentCapabilities() {
        var config = new CoreliaRuntimeConfig(new MockEnvironment()
                .withProperty("corelia.provider", "platform-v")
                .withProperty("corelia.provider.binary-storage", "s3"));
        var platform = descriptor("platform-v", EnumSet.allOf(ProviderCapability.class));
        var s3 = descriptor("s3", EnumSet.of(ProviderCapability.BINARY_STORAGE));

        var registry = new ProviderRegistry(config, List.of(platform, s3));

        assertEquals("platform-v", registry.provider(ProviderCapability.DOCUMENTS).id());
        assertEquals("s3", registry.provider(ProviderCapability.BINARY_STORAGE).id());
    }

    @Test
    void rejectsMissingCapabilityImplementation() {
        var config = new CoreliaRuntimeConfig(new MockEnvironment().withProperty("corelia.provider", "platform-v"));
        var platform = descriptor("platform-v", EnumSet.of(ProviderCapability.DOCUMENTS));

        assertThrows(IllegalStateException.class, () -> new ProviderRegistry(config, List.of(platform)));
    }

    @Test
    void resolvesOnlyCapabilitiesRequiredByTheService() {
        var config = new CoreliaRuntimeConfig(new MockEnvironment()
                .withProperty("corelia.provider", "platform-v")
                .withProperty("CORELIA_PROVIDER_CAPABILITIES", "documents"));
        var documents = descriptor("platform-v", EnumSet.of(ProviderCapability.DOCUMENTS));

        var registry = new ProviderRegistry(config, List.of(documents));

        assertEquals("platform-v", registry.provider(ProviderCapability.DOCUMENTS).id());
        assertEquals(null, registry.provider(ProviderCapability.BINARY_STORAGE));
        assertDoesNotThrow(() -> new ProviderStartupValidator(registry));
    }

    private static ProviderDescriptor descriptor(String id, java.util.Set<ProviderCapability> capabilities) {
        return new ProviderDescriptor() {
            public String id() { return id; }
            public java.util.Set<ProviderCapability> capabilities() { return capabilities; }
        };
    }
}
