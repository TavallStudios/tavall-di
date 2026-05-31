package org.tavall.dependency.access;

import org.tavall.dependency.annotations.DelegatesToInterface;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyAccessGrantProcessorTest {

    @Test
    void processorEmitsGrantMetadataForSingleAccessInterface() {
        DependencyAccessSourceLowerer lowerer = new DependencyAccessSourceLowerer();
        Map<String, String> loweredSources = lowerer.lowerSources(buildSingleAccessSources());
        Path outputDirectory = DependencyAccessTestCompiler.compileSources(loweredSources);

        Class<?> handlerClass = DependencyAccessTestCompiler.loadClass(outputDirectory, "org.example.SingleAccessHandler");
        DependencyAccessGrantHandler grantHandler = new DependencyAccessGrantHandler();
        List<DependencyAccessGrantDescriptor> descriptors = grantHandler.findGrantedDependencyAccesses(handlerClass);

        assertEquals(1, descriptors.size());
        DependencyAccessGrantDescriptor descriptor = descriptors.get(0);
        assertEquals("org.example.PlayerAccess", descriptor.accessType().getName());
        assertEquals(2, descriptor.dependencyTypes().size());
        assertEquals("org.example.IPlayerRegistry", descriptor.dependencyTypes().get(0).getName());
        assertEquals("org.example.IPlayerDataRepository", descriptor.dependencyTypes().get(1).getName());
    }

    @Test
    void processorEmitsGrantMetadataForMultipleAccessInterfacesAndPreservesDelegatesAnnotation() {
        DependencyAccessSourceLowerer lowerer = new DependencyAccessSourceLowerer();
        Map<String, String> loweredSources = lowerer.lowerSources(buildMultipleAccessSources());
        Path outputDirectory = DependencyAccessTestCompiler.compileSources(loweredSources);

        Class<?> handlerClass = DependencyAccessTestCompiler.loadClass(outputDirectory, "org.example.MultiAccessHandler");
        DelegatesToInterface delegatesToInterface = handlerClass.getAnnotation(DelegatesToInterface.class);

        assertNotNull(delegatesToInterface);
        assertEquals("org.example.IRewardHandler", delegatesToInterface.value().getName());

        DependencyAccessGrantHandler grantHandler = new DependencyAccessGrantHandler();
        List<DependencyAccessGrantDescriptor> descriptors = grantHandler.findGrantedDependencyAccesses(handlerClass);

        assertEquals(3, descriptors.size());
        assertTrue(descriptors.stream().anyMatch(descriptor ->
                "org.example.PlayerAccess".equals(descriptor.accessType().getName())
                        && descriptor.dependencyTypes().stream().map(Class::getName).toList()
                        .equals(List.of("org.example.IPlayerRegistry", "org.example.IPlayerDataRepository"))));
        assertTrue(descriptors.stream().anyMatch(descriptor ->
                "org.example.EconomyAccess".equals(descriptor.accessType().getName())
                        && descriptor.dependencyTypes().stream().map(Class::getName).toList()
                        .equals(List.of("org.example.IWalletRegistry", "org.example.ITransactionRepository"))));
        assertTrue(descriptors.stream().anyMatch(descriptor ->
                "org.example.MessageAccess".equals(descriptor.accessType().getName())
                        && descriptor.dependencyTypes().stream().map(Class::getName).toList()
                        .equals(List.of("org.example.IMessageRegistry", "org.example.IMessageFormatter"))));
    }

    private Map<String, String> buildSingleAccessSources() {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("org.example.PlayerAccess", """
                package org.example;

                import org.tavall.dependency.annotations.VariableTypeArguments;
                import org.tavall.dependency.DependencyAccess;

                @VariableTypeArguments
                public interface PlayerAccess extends DependencyAccess {
                }
                """);
        sources.put("org.example.IPlayerRegistry", """
                package org.example;

                public interface IPlayerRegistry {
                }
                """);
        sources.put("org.example.IPlayerDataRepository", """
                package org.example;

                public interface IPlayerDataRepository {
                }
                """);
        sources.put("org.example.IRewardHandler", """
                package org.example;

                public interface IRewardHandler {
                }
                """);
        sources.put("org.example.SingleAccessHandler", """
                package org.example;

                import org.tavall.dependency.annotations.DelegatesToInterface;
                import org.tavall.dependency.annotations.GrantDependencyAccess;

                @GrantDependencyAccess
                @DelegatesToInterface(value = IRewardHandler.class)
                public final class SingleAccessHandler<RewardValue>
                        implements PlayerAccess<IPlayerRegistry, IPlayerDataRepository> {
                }
                """);
        return sources;
    }

    private Map<String, String> buildMultipleAccessSources() {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("org.example.PlayerAccess", """
                package org.example;

                import org.tavall.dependency.annotations.VariableTypeArguments;
                import org.tavall.dependency.DependencyAccess;

                @VariableTypeArguments
                public interface PlayerAccess extends DependencyAccess {
                }
                """);
        sources.put("org.example.EconomyAccess", """
                package org.example;

                import org.tavall.dependency.annotations.VariableTypeArguments;
                import org.tavall.dependency.DependencyAccess;

                @VariableTypeArguments
                public interface EconomyAccess extends DependencyAccess {
                }
                """);
        sources.put("org.example.MessageAccess", """
                package org.example;

                import org.tavall.dependency.annotations.VariableTypeArguments;
                import org.tavall.dependency.DependencyAccess;

                @VariableTypeArguments
                public interface MessageAccess extends DependencyAccess {
                }
                """);
        sources.put("org.example.IPlayerRegistry", """
                package org.example;

                public interface IPlayerRegistry {
                }
                """);
        sources.put("org.example.IPlayerDataRepository", """
                package org.example;

                public interface IPlayerDataRepository {
                }
                """);
        sources.put("org.example.IWalletRegistry", """
                package org.example;

                public interface IWalletRegistry {
                }
                """);
        sources.put("org.example.ITransactionRepository", """
                package org.example;

                public interface ITransactionRepository {
                }
                """);
        sources.put("org.example.IMessageRegistry", """
                package org.example;

                public interface IMessageRegistry {
                }
                """);
        sources.put("org.example.IMessageFormatter", """
                package org.example;

                public interface IMessageFormatter {
                }
                """);
        sources.put("org.example.IRewardHandler", """
                package org.example;

                public interface IRewardHandler {
                }
                """);
        sources.put("org.example.MultiAccessHandler", """
                package org.example;

                import org.tavall.dependency.annotations.DelegatesToInterface;
                import org.tavall.dependency.annotations.GrantDependencyAccess;

                @GrantDependencyAccess
                @DelegatesToInterface(value = IRewardHandler.class)
                public final class MultiAccessHandler<RewardValue>
                        implements PlayerAccess<IPlayerRegistry, IPlayerDataRepository>,
                        EconomyAccess<IWalletRegistry, ITransactionRepository>,
                        MessageAccess<IMessageRegistry, IMessageFormatter> {
                }
                """);
        return sources;
    }
}
