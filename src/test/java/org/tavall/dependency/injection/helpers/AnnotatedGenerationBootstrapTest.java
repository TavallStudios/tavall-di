package org.tavall.dependency.injection.helpers;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.dependency.DependencyLoaderAccess;
import org.tavall.dependency.injection.helpers.generationfixtures.GenerationProbeData;
import org.tavall.dependency.injection.helpers.generationfixtures.GenerationReader;
import org.tavall.dependency.injection.helpers.generationfixtures.IGenerationReader;
import org.tavall.dependency.maps.DependencyMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AnnotatedGenerationBootstrapTest {
    private final DependencyMap map = DependencyMap.getDependencyMap();
    private final GenerationProbeData probe = new GenerationProbeData();

    @BeforeEach
    void composeInputs() {
        map.clear();
        DependencyLoaderAccess.clear();
        map.registerInstance(GenerationProbeData.class, probe);
    }

    @AfterEach
    void clear() {
        map.clear();
        DependencyLoaderAccess.clear();
    }

    @Test
    void bindsBeforeLifecycleAndRepeatedSetupKeepsOneMetadataOwner() {
        DependencyInjectorHelper<Object, Object> bootstrap = bootstrap();
        bootstrap.setupDISystem(getClass().getClassLoader());
        IGenerationReader reader = map.getInstance(IGenerationReader.class);
        assertEquals("first", reader.read());
        assertEquals(List.of("first"), probe.initializedValues());
        assertSame(map.findMetaData(GenerationReader.class), map.findMetaData(IGenerationReader.class));
        bootstrap.setupDISystem(getClass().getClassLoader());
        assertSame(reader, map.getInstance(IGenerationReader.class));
        assertEquals(1, probe.initializedValues().size());
    }

    @Test
    void reloadRetiresTheOldConsumerAndInitializesItsReplacement() {
        DependencyInjectorHelper<Object, Object> bootstrap = bootstrap();
        bootstrap.setupDISystem(getClass().getClassLoader());
        IGenerationReader original = map.getInstance(IGenerationReader.class);
        probe.value("reloaded");
        bootstrap.reloadDISystem(getClass().getClassLoader());
        IGenerationReader replacement = map.getInstance(IGenerationReader.class);
        assertNotSame(original, replacement);
        assertThrows(IllegalStateException.class, original::read);
        assertEquals("reloaded", replacement.read());
        assertEquals(List.of("first", "reloaded"), probe.initializedValues());
    }

    @Test
    void clearPreventsOldConsumerAccessAfterFreshBootstrap() {
        DependencyInjectorHelper<Object, Object> bootstrap = bootstrap();
        bootstrap.setupDISystem(getClass().getClassLoader());
        IGenerationReader retired = map.getInstance(IGenerationReader.class);
        map.clear();
        probe.value("replacement");
        map.registerInstance(GenerationProbeData.class, probe);
        bootstrap.setupDISystem(getClass().getClassLoader());
        assertThrows(IllegalStateException.class, retired::read);
        assertEquals("replacement", map.getInstance(IGenerationReader.class).read());
    }

    @Test
    void failedLifecycleRollsBackAndRetiresTheNewOwnedConsumer() {
        probe.failInitialization(true);
        DependencyInjectorHelper<Object, Object> bootstrap = bootstrap();
        assertThrows(IllegalStateException.class, () -> bootstrap.setupDISystem(getClass().getClassLoader()));
        GenerationReader failed = probe.consumers().getLast();
        assertTrue(failed.isDependencyGenerationRetired());
        assertThrows(IllegalStateException.class, failed::read);
        assertNull(map.findInstance(IGenerationReader.class));
        assertNull(map.findInstance(GenerationReader.class));
        assertSame(probe, map.findInstance(GenerationProbeData.class));
        probe.failInitialization(false);
        bootstrap.setupDISystem(getClass().getClassLoader());
        assertEquals("first", map.getInstance(IGenerationReader.class).read());
    }

    private DependencyInjectorHelper<Object, Object> bootstrap() {
        DependencyInjectorHelper<Object, Object> bootstrap = new DependencyInjectorHelper<>();
        bootstrap.setBasePackage("org.tavall.dependency.injection.helpers.generationfixtures");
        return bootstrap;
    }
}
