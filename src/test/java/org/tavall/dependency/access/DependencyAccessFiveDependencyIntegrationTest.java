package org.tavall.dependency.access;

import org.junit.jupiter.api.Test;
import org.tavall.dependency.access.fixtures.FiveDependencyHandler;
import org.tavall.dependency.annotations.DelegatesTo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DependencyAccessFiveDependencyIntegrationTest {

    @Test
    void resolvesGrantMetadataFromAConcreteHandlerWithFiveDependencyTypeArguments() {
        FiveDependencyHandler<String> handler = new FiveDependencyHandler<>();
        Class<?> handlerType = handler.getClass();

        DelegatesTo delegatesTo = handlerType.getAnnotation(DelegatesTo.class);
        assertNotNull(delegatesTo);
        assertEquals("org.tavall.dependency.access.fixtures.IRewardHandler",
                delegatesTo.value()[0].getName());

        DependencyAccessGrantHandler grantHandler = new DependencyAccessGrantHandler();
        List<DependencyAccessGrantDescriptor> descriptors = grantHandler.findGrantedDependencyAccesses(handlerType);

        assertEquals(1, descriptors.size());
        DependencyAccessGrantDescriptor descriptor = descriptors.get(0);
        assertEquals("org.tavall.dependency.access.fixtures.PlayerAccess", descriptor.accessType().getName());
        assertEquals(List.of(
                "org.tavall.dependency.access.fixtures.IPlayerRegistry",
                "org.tavall.dependency.access.fixtures.IPlayerDataRepository",
                "org.tavall.dependency.access.fixtures.IWalletRegistry",
                "org.tavall.dependency.access.fixtures.ITransactionRepository",
                "org.tavall.dependency.access.fixtures.IMessageFormatter"),
                descriptor.dependencyTypes().stream().map(Class::getName).toList());
    }
}
