package org.tavall.dependency.access;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tavall.dependency.DependencyLoader;
import org.tavall.dependency.DependencyLoaderAccess;
import org.tavall.dependency.access.fixtures.ISomeClass;
import org.tavall.dependency.access.fixtures.SomeClass;
import org.tavall.dependency.annotations.DelegatesToInterface;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class DependencyAccessProductionIntegrationTest {

    @AfterEach
    void clearDependencyState() {
        DependencyLoaderAccess.clear();
        DependencyLoader.clearNamedLoaders();
    }

    @Test
    void resolvesGrantMetadataAndARealDelegatedDependencyLookupFromTheProductionClass() {
        ISomeClass delegatedInstance = new ISomeClass() {
        };
        DependencyLoaderAccess.registerInstance(ISomeClass.class, delegatedInstance);

        SomeClass<String> productionClass = new SomeClass<>();
        Class<?> productionType = productionClass.getClass();

        DelegatesToInterface delegatesToInterface = productionType.getAnnotation(DelegatesToInterface.class);
        assertNotNull(delegatesToInterface);
        assertSame(ISomeClass.class, delegatesToInterface.value());

        ISomeClass resolvedInstance = productionClass.lookupDelegatedInterface();
        assertSame(delegatedInstance, resolvedInstance);

        DependencyAccessGrantHandler grantHandler = new DependencyAccessGrantHandler();
        List<DependencyAccessGrantDescriptor> descriptors = grantHandler.findGrantedDependencyAccesses(productionType);

        assertEquals(1, descriptors.size());
        DependencyAccessGrantDescriptor descriptor = descriptors.get(0);
        assertEquals("org.tavall.dependency.DependencyAccess", descriptor.accessType().getName());
        assertEquals(List.of(
                "org.tavall.dependency.access.fixtures.DependencyOneValue",
                "org.tavall.dependency.access.fixtures.DependencyTwoValue",
                "org.tavall.dependency.access.fixtures.DependencyThreeValue",
                "org.tavall.dependency.access.fixtures.DependencyFourValue"),
                descriptor.dependencyTypes().stream().map(Class::getName).toList());
    }
}
