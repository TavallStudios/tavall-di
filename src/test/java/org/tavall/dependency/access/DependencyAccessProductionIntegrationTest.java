package org.tavall.dependency.access;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tavall.dependency.access.fixtures.ISomeClass;
import org.tavall.dependency.access.fixtures.SomeClass;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.dependency.maps.DependencyMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class DependencyAccessProductionIntegrationTest {

    @AfterEach
    void clearDependencyState() {
        DependencyMap.getDependencyMap().clear();
    }

    @Test
    void resolvesSingleDependencyThroughTheAuthoritativeMap() {
        ISomeClass delegatedInstance = new ISomeClass() {
        };
        DependencyMap.getDependencyMap().registerInstance(ISomeClass.class, delegatedInstance);

        SomeClass<String> productionClass = new SomeClass<>();
        DelegatesTo delegatesTo = productionClass.getClass().getAnnotation(DelegatesTo.class);

        assertNotNull(delegatesTo);
        assertEquals(1, delegatesTo.value().length);
        assertSame(ISomeClass.class, delegatesTo.value()[0]);
        assertSame(ISomeClass.class, productionClass.getDependencyAccessType());
        assertSame(delegatedInstance, productionClass.lookupDelegatedInterface());
    }
}
