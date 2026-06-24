package org.tavall.dependency.access;

import org.junit.jupiter.api.Test;
import org.tavall.dependency.access.fixtures.FiveDependencyHandler;
import org.tavall.dependency.annotations.DelegatesToInterface;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DependencyAccessFiveDependencyIntegrationTest {

    @Test
    void resolvesGrantMetadataFromAConcreteHandlerWithFiveDependencyTypeArguments() {
        FiveDependencyHandler<String> handler = new FiveDependencyHandler<>();
        Class<?> handlerType = handler.getClass();

        DelegatesToInterface delegatesToInterface = handlerType.getAnnotation(DelegatesToInterface.class);
        assertNotNull(delegatesToInterface);
        assertEquals("org.tavall.dependency.access.fixtures.IRewardHandler",
                delegatesToInterface.value().getName());

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
