package org.tavall.dependency.maps;

import org.tavall.dependency.DependencyLoaderAccess;
import org.tavall.dependency.fixtures.ConstructorBoundUtilsService;
import org.tavall.dependency.injection.helpers.fixtures.DelegatingUtilsService;
import org.tavall.dependency.metadata.DependencyMetaData;
import org.tavall.dependency.metadata.wrappers.DependencyInstance;
import org.tavall.dependency.metadata.wrappers.DependencyInterface;
import org.tavall.dependency.fixtures.contracts.interfaces.IUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    void registersConcreteKeysDuringRegistration() {
        DependencyMetaData<IUtils, DelegatingUtilsService> metaData = new DependencyMetaData<>();
        metaData.populateMetaData(
                IUtils.class,
                DelegatingUtilsService.class,
                new DependencyInterface<>(IUtils.class),
                new DependencyInstance<>(DelegatingUtilsService.class));

        DependencyMap.getDependencyMap().registerDependency((Class) DelegatingUtilsService.class, metaData);

        assertTrue(DependencyMap.getDependencyMap().isInstanceRegistered(DelegatingUtilsService.class));
        assertSame(metaData, DependencyMap.getDependencyMap().findMetaData(DelegatingUtilsService.class));
        assertSame(metaData.getDependencyInstance(), DependencyMap.getDependencyMap().findInstance(DelegatingUtilsService.class));
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
    void replacesDuplicateRegistrationsForTheSameInterfaceToken() {
        ConstructorBoundUtilsService first = new ConstructorBoundUtilsService("first");
        ConstructorBoundUtilsService second = new ConstructorBoundUtilsService("second");

        DependencyMap.getDependencyMap().registerInstance(IUtils.class, first);
        DependencyMap.getDependencyMap().registerInstance(IUtils.class, second);

        assertNotNull(DependencyMap.getDependencyMap().findInstance(IUtils.class));
        assertSame(second, DependencyLoaderAccess.findInstance(IUtils.class));
        assertSame(second, DependencyMap.getDependencyMap().findInstance(IUtils.class));
    }
}
