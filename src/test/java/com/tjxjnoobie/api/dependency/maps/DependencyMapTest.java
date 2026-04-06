package com.tjxjnoobie.api.dependency.maps;

import com.tjxjnoobie.api.dependency.DependencyLoaderAccess;
import com.tjxjnoobie.api.dependency.fixtures.ConstructorBoundUtilsService;
import com.tjxjnoobie.api.dependency.injection.helpers.fixtures.DelegatingUtilsService;
import com.tjxjnoobie.api.dependency.metadata.DependencyMetaData;
import com.tjxjnoobie.api.dependency.metadata.wrappers.DependencyInstance;
import com.tjxjnoobie.api.dependency.metadata.wrappers.DependencyInterface;
import com.tjxjnoobie.api.interfaces.IUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyMapTest {

    @AfterEach
    void clearDependencyMap() {
        DependencyMap.getDependencyMap().clear();
    }

    @Test
    void resolvesRegisteredMetadataByRealProjectInterfaceToken() {
        DependencyMetaData<IUtils, DelegatingUtilsService> metaData = new DependencyMetaData<>();
        metaData.populateMetaData(
                IUtils.class,
                DelegatingUtilsService.class,
                new DependencyInterface<>(IUtils.class),
                new DependencyInstance<>(DelegatingUtilsService.class));

        DependencyMap.getDependencyMap().registerDependency(IUtils.class, metaData);

        IUtils utils = DependencyMap.getDependencyMap().findInstance(IUtils.class);
        utils.createServerID();

        assertSame(metaData, DependencyMap.getDependencyMap().findMetaData(IUtils.class));
        assertSame(metaData.getDependencyInstance(), utils);
        assertEquals("server-generated", utils.getServerID());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void rejectsConcreteKeysDuringRegistration() {
        DependencyMetaData<IUtils, DelegatingUtilsService> metaData = new DependencyMetaData<>();
        metaData.populateMetaData(
                IUtils.class,
                DelegatingUtilsService.class,
                new DependencyInterface<>(IUtils.class),
                new DependencyInstance<>(DelegatingUtilsService.class));

        assertThrows(IllegalArgumentException.class,
                () -> DependencyMap.getDependencyMap().registerDependency((Class) DelegatingUtilsService.class, metaData));
    }

    @Test
    void replacesRegisteredInstanceThroughLoaderVocabulary() {
        DependencyMetaData<IUtils, DelegatingUtilsService> metaData = new DependencyMetaData<>();
        metaData.populateMetaData(
                IUtils.class,
                DelegatingUtilsService.class,
                new DependencyInterface<>(IUtils.class),
                new DependencyInstance<>(DelegatingUtilsService.class));

        DependencyMap.getDependencyMap().registerDependency(IUtils.class, metaData);

        IUtils original = DependencyLoaderAccess.findInstance(IUtils.class);
        IUtils replacement = DependencyMap.getDependencyMap().replaceInstance(IUtils.class, DelegatingUtilsService::new);

        assertTrue(DependencyMap.getDependencyMap().isInstanceRegistered(IUtils.class));
        assertFalse(DependencyMap.getDependencyMap().isDependencyMapEmpty());
        assertNotSame(original, replacement);
        assertSame(replacement, metaData.getDependencyInstance());
        assertSame(replacement, DependencyLoaderAccess.findInstance(IUtils.class));
    }

    @Test
    void rejectsDuplicateRegistrationsForTheSameInterfaceToken() {
        ConstructorBoundUtilsService first = new ConstructorBoundUtilsService("first");
        ConstructorBoundUtilsService second = new ConstructorBoundUtilsService("second");

        DependencyMap.getDependencyMap().registerInstance(IUtils.class, first);

        assertThrows(IllegalStateException.class,
                () -> DependencyMap.getDependencyMap().registerInstance(IUtils.class, second));
    }
}
