package org.tavall.dependency.access;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyAccessSourceLowererTest {

    @Test
    void lowersSingleAccessInterfaceAndPreservesClassGenericsAndDelegatesAnnotation() {
        DependencyAccessSourceLowerer lowerer = new DependencyAccessSourceLowerer();
        Map<String, String> loweredSources = lowerer.lowerSources(buildSingleAccessSources());

        String loweredHandlerSource = loweredSources.get("org.example.SingleAccessHandler");

        assertTrue(loweredHandlerSource.contains("@org.tavall.dependency.annotations.GrantedDependencyAccess("));
        assertTrue(loweredHandlerSource.contains("accessType = PlayerAccess.class"));
        assertTrue(loweredHandlerSource.contains("dependencyTypes = {"));
        assertTrue(loweredHandlerSource.contains("IPlayerRegistry.class"));
        assertTrue(loweredHandlerSource.contains("IPlayerDataRepository.class"));
        assertTrue(loweredHandlerSource.contains("@DelegatesToInterface(value = IRewardHandler.class)"));
        assertTrue(loweredHandlerSource.contains("public final class SingleAccessHandler<RewardValue>"));
        assertTrue(loweredHandlerSource.contains("implements PlayerAccess"));
        assertTrue(loweredHandlerSource.contains("implements PlayerAccess"));
    }

    @Test
    void lowersMultipleAccessInterfacesAndPreservesDuplicateAcrossDomains() {
        DependencyAccessSourceLowerer lowerer = new DependencyAccessSourceLowerer();
        Map<String, String> loweredSources = lowerer.lowerSources(buildMultipleAccessSources());

        String loweredHandlerSource = loweredSources.get("org.example.MultiAccessHandler");

        assertTrue(loweredHandlerSource.contains("accessType = PlayerAccess.class"));
        assertTrue(loweredHandlerSource.contains("accessType = EconomyAccess.class"));
        assertTrue(loweredHandlerSource.contains("accessType = MessageAccess.class"));
        assertTrue(loweredHandlerSource.contains("IPlayerRegistry.class"));
        assertTrue(loweredHandlerSource.contains("IWalletRegistry.class"));
        assertTrue(loweredHandlerSource.contains("IMessageRegistry.class"));
    }

    @Test
    void rejectsMissingVariableTypeArgumentsAnnotation() {
        DependencyAccessSourceLowerer lowerer = new DependencyAccessSourceLowerer();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> lowerer.lowerSources(buildMissingVariableTypeArgumentSources()));

        assertTrue(exception.getMessage().contains("@VariableTypeArguments"));
    }

    @Test
    void rejectsTypeVariableDependencyArgument() {
        DependencyAccessSourceLowerer lowerer = new DependencyAccessSourceLowerer();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> lowerer.lowerSources(buildTypeVariableDependencySources()));

        assertTrue(exception.getMessage().contains("type variable dependency argument"));
    }

    @Test
    void rejectsWildcardDependencyArgument() {
        DependencyAccessSourceLowerer lowerer = new DependencyAccessSourceLowerer();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> lowerer.lowerSources(buildWildcardDependencySources()));

        assertTrue(exception.getMessage().contains("wildcard dependency argument"));
    }

    @Test
    void rejectsDuplicateDependencyTypeWithinOneAccessInterface() {
        DependencyAccessSourceLowerer lowerer = new DependencyAccessSourceLowerer();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> lowerer.lowerSources(buildDuplicateDependencySources()));

        assertTrue(exception.getMessage().contains("duplicate dependency type"));
    }

    @Test
    void allowsDuplicateDependencyTypeAcrossDifferentAccessInterfaces() {
        DependencyAccessSourceLowerer lowerer = new DependencyAccessSourceLowerer();

        assertDoesNotThrow(() -> lowerer.lowerSources(buildDuplicateAcrossAccessSources()));
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

    private Map<String, String> buildMissingVariableTypeArgumentSources() {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("org.example.PlayerAccess", """
                package org.example;

                import org.tavall.dependency.DependencyAccess;

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
        sources.put("org.example.BrokenHandler", """
                package org.example;

                import org.tavall.dependency.annotations.GrantDependencyAccess;

                @GrantDependencyAccess
                public final class BrokenHandler
                        implements PlayerAccess<IPlayerRegistry, IPlayerDataRepository> {
                }
                """);
        return sources;
    }

    private Map<String, String> buildTypeVariableDependencySources() {
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
        sources.put("org.example.IRewardHandler", """
                package org.example;

                public interface IRewardHandler {
                }
                """);
        sources.put("org.example.TypeVariableHandler", """
                package org.example;

                import org.tavall.dependency.annotations.GrantDependencyAccess;

                @GrantDependencyAccess
                public final class TypeVariableHandler<RewardValue>
                        implements PlayerAccess<RewardValue> {
                }
                """);
        return sources;
    }

    private Map<String, String> buildWildcardDependencySources() {
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
        sources.put("org.example.IRewardHandler", """
                package org.example;

                public interface IRewardHandler {
                }
                """);
        sources.put("org.example.WildcardHandler", """
                package org.example;

                import org.tavall.dependency.annotations.GrantDependencyAccess;

                @GrantDependencyAccess
                public final class WildcardHandler
                        implements PlayerAccess<? extends IPlayerRegistry> {
                }
                """);
        return sources;
    }

    private Map<String, String> buildDuplicateDependencySources() {
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
        sources.put("org.example.IRewardHandler", """
                package org.example;

                public interface IRewardHandler {
                }
                """);
        sources.put("org.example.DuplicateHandler", """
                package org.example;

                import org.tavall.dependency.annotations.GrantDependencyAccess;

                @GrantDependencyAccess
                public final class DuplicateHandler
                        implements PlayerAccess<IPlayerRegistry, IPlayerRegistry> {
                }
                """);
        return sources;
    }

    private Map<String, String> buildDuplicateAcrossAccessSources() {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("org.example.PlayerAccess", """
                package org.example;

                import org.tavall.dependency.annotations.VariableTypeArguments;
                import org.tavall.dependency.DependencyAccess;

                @VariableTypeArguments
                public interface PlayerAccess extends DependencyAccess {
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
        sources.put("org.example.IRewardHandler", """
                package org.example;

                public interface IRewardHandler {
                }
                """);
        sources.put("org.example.DuplicateAcrossHandler", """
                package org.example;

                import org.tavall.dependency.annotations.GrantDependencyAccess;

                @GrantDependencyAccess
                public final class DuplicateAcrossHandler
                        implements PlayerAccess<IPlayerRegistry>,
                        MessageAccess<IPlayerRegistry> {
                }
                """);
        return sources;
    }
}
