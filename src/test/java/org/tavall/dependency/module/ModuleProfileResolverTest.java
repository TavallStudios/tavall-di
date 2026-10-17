package org.tavall.dependency.module;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.dependency.IDependencyInjectableConcrete;
import org.tavall.dependency.IDependencyInjectableInterface;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.dependency.injection.helpers.DependencyInjectorHelper;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.util.CustomEnum;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleProfileResolverTest {

    // General test fixtures
    private static final ModuleId CORE = ModuleId.of("tavall-paper-core");
    private static final ModuleId UI = ModuleId.of("tavall-ui");
    private static final ModuleId LOBBY = ModuleId.of("tavall-lobby");
    private static final ModuleId MISSING = ModuleId.of("missing");
    private static final ProfileId LOBBY_PROFILE = ProfileId.of("lobby");

    // Pay fixtures
    private static final ModuleId PAY_SYSTEM = ModuleId.of("tavall-pay");
    private static final ModuleId PAY_STRIPE = ModuleId.of("tavall-pay-stripe");
    private static final ModuleId PAY_PAYPAL = ModuleId.of("tavall-pay-paypal");
    private static final RuntimeTargetId COMMERCE_RUNTIME = RuntimeTargetId.of("web-commerce");
    private static final ProfileId PAY_STRIPE_PROFILE = ProfileId.of("pay-stripe-enabled");
    private static final ProfileId PAY_PAYPAL_PROFILE = ProfileId.of("pay-paypal-enabled");

    // Content Auto Post fixtures (7 features from universal architecture)
    private static final ModuleId AUTO_POST_SYSTEM = ModuleId.of("tavall-content-auto-post-system");
    private static final ModuleId CONTENT_DISCOVERY = ModuleId.of("content-discovery");
    private static final ModuleId CONTENT_VIDEO_WATCH = ModuleId.of("content-video-watch");
    private static final ModuleId CONTENT_CREATIVE = ModuleId.of("content-creative");
    private static final ModuleId CONTENT_ENGAGEMENT = ModuleId.of("content-engagement");
    private static final ModuleId CONTENT_INBOUND = ModuleId.of("content-inbound");
    private static final ModuleId CONTENT_PUBLISHING = ModuleId.of("content-publishing");
    private static final ModuleId CONTENT_REPORTING = ModuleId.of("content-reporting");
    private static final RuntimeTargetId CONTENT_NODE = RuntimeTargetId.of("content-autopost-node");
    private static final ProfileId CONTENT_PROFILE_A = ProfileId.of("content-deployment-a");
    private static final ProfileId CONTENT_PROFILE_B = ProfileId.of("content-deployment-b");

    @BeforeEach
    @AfterEach
    void resetDependencyMap() {
        DependencyMap.getDependencyMap().clear();
    }

    @Test
    void resolvesRequiredDependencyClosureInDependencyFirstOrder() {
        ModuleProfile<ModuleId, ProfileId> profile = ModuleProfile.of(LOBBY_PROFILE, LOBBY);
        Map<ModuleId, List<ModuleId>> required = Map.of(
                LOBBY, List.of(UI),
                UI, List.of(CORE),
                CORE, List.of()
        );

        EffectiveModuleProfile<ModuleId, ProfileId> effective = new ModuleProfileResolver<ModuleId, ProfileId>()
                .resolve(profile, Set.of(CORE, UI, LOBBY), module -> required.getOrDefault(module, List.of()));

        assertEquals(Set.of(LOBBY), effective.requestedModules());
        assertEquals(List.of(CORE, UI, LOBBY), effective.loadOrder());
        assertEquals(Set.of(CORE, UI, LOBBY), effective.modules());
    }

    @Test
    void deduplicatesSharedRequiredDependencies() {
        ModuleId achievements = ModuleId.of("tavall-achievements");
        ModuleProfile<ModuleId, ProfileId> profile = ModuleProfile.of(LOBBY_PROFILE, LOBBY, achievements);
        Map<ModuleId, List<ModuleId>> required = Map.of(
                LOBBY, List.of(CORE),
                achievements, List.of(CORE),
                CORE, List.of()
        );

        EffectiveModuleProfile<ModuleId, ProfileId> effective = new ModuleProfileResolver<ModuleId, ProfileId>()
                .resolve(profile, Set.of(CORE, LOBBY, achievements), module -> required.getOrDefault(module, List.of()));

        assertEquals(List.of(CORE, LOBBY, achievements), effective.loadOrder());
    }

    @Test
    void rejectsUnknownRequiredModules() {
        ModuleProfile<ModuleId, ProfileId> profile = ModuleProfile.of(LOBBY_PROFILE, LOBBY);

        assertThrows(IllegalArgumentException.class, () -> new ModuleProfileResolver<ModuleId, ProfileId>()
                .resolve(profile, Set.of(LOBBY), module -> module.equals(LOBBY) ? List.of(MISSING) : List.of()));
    }

    @Test
    void rejectsRequiredDependencyCycles() {
        ModuleProfile<ModuleId, ProfileId> profile = ModuleProfile.of(LOBBY_PROFILE, LOBBY);

        assertThrows(IllegalStateException.class, () -> new ModuleProfileResolver<ModuleId, ProfileId>()
                .resolve(profile, Set.of(LOBBY, UI), module -> module.equals(LOBBY) ? List.of(UI) : List.of(LOBBY)));
    }

    @Test
    void payStripeEnabledPayPalDisabled() {
        // Stripe enabled, PayPal disabled
        DeploymentModuleProfile<ModuleId, ProfileId, RuntimeTargetId> profile = DeploymentModuleProfile.<ModuleId, ProfileId, RuntimeTargetId>builder()
                .schemaVersion(1L)
                .profileId(PAY_STRIPE_PROFILE)
                .runtimeTarget(COMMERCE_RUNTIME)
                .configurationGeneration(10L)
                .enable(PAY_SYSTEM, PAY_STRIPE)
                .disable(PAY_PAYPAL)
                .build();

        Map<ModuleId, List<ModuleId>> dependencies = Map.of(
                PAY_SYSTEM, List.of(),
                PAY_STRIPE, List.of(PAY_SYSTEM),
                PAY_PAYPAL, List.of(PAY_SYSTEM)
        );

        IModuleProfileResolver<ModuleId, ProfileId> resolver = new ModuleProfileResolver<>();
        EffectiveDeploymentModuleProfile<ModuleId, ProfileId, RuntimeTargetId> effective = resolver.resolve(
                profile,
                Set.of(PAY_SYSTEM, PAY_STRIPE, PAY_PAYPAL),
                m -> dependencies.getOrDefault(m, List.of())
        );

        assertEquals(1L, effective.schemaVersion());
        assertEquals(PAY_STRIPE_PROFILE, effective.profileId());
        assertEquals(PAY_STRIPE_PROFILE, effective.id());
        assertEquals(COMMERCE_RUNTIME, effective.runtimeTarget());
        assertEquals(10L, effective.configurationGeneration());
        assertEquals(Set.of(PAY_SYSTEM, PAY_STRIPE), effective.requestedModules());
        assertEquals(Set.of(PAY_PAYPAL), effective.disabledModules());

        // Dependency-first: PAY_SYSTEM loaded before PAY_STRIPE
        assertEquals(List.of(PAY_SYSTEM, PAY_STRIPE), effective.loadOrder());
        assertEquals(Set.of(PAY_SYSTEM, PAY_STRIPE), effective.modules());
        assertFalse(effective.modules().contains(PAY_PAYPAL));
    }

    @Test
    void payPayPalEnabledStripeDisabledReverse() {
        // Reverse: PayPal enabled, Stripe disabled
        DeploymentModuleProfile<ModuleId, ProfileId, RuntimeTargetId> profile = DeploymentModuleProfile.<ModuleId, ProfileId, RuntimeTargetId>builder()
                .schemaVersion(1L)
                .profileId(PAY_PAYPAL_PROFILE)
                .runtimeTarget(COMMERCE_RUNTIME)
                .configurationGeneration(11L)
                .enable(PAY_SYSTEM, PAY_PAYPAL)
                .disable(PAY_STRIPE)
                .build();

        Map<ModuleId, List<ModuleId>> dependencies = Map.of(
                PAY_SYSTEM, List.of(),
                PAY_STRIPE, List.of(PAY_SYSTEM),
                PAY_PAYPAL, List.of(PAY_SYSTEM)
        );

        IModuleProfileResolver<ModuleId, ProfileId> resolver = new ModuleProfileResolver<>();
        EffectiveDeploymentModuleProfile<ModuleId, ProfileId, RuntimeTargetId> effective = resolver.resolve(
                profile,
                Set.of(PAY_SYSTEM, PAY_STRIPE, PAY_PAYPAL),
                m -> dependencies.getOrDefault(m, List.of())
        );

        assertEquals(List.of(PAY_SYSTEM, PAY_PAYPAL), effective.loadOrder());
        assertEquals(Set.of(PAY_SYSTEM, PAY_PAYPAL), effective.modules());
        assertFalse(effective.modules().contains(PAY_STRIPE));
        assertTrue(effective.disabledModules().contains(PAY_STRIPE));
    }

    @Test
    void contentDeploymentA() {
        // Content deployment A: Auto Post SYSTEM, Discovery, Video Watch, Creative, Publishing, Reporting enabled; Engagement, Inbound disabled
        DeploymentModuleProfile<ModuleId, ProfileId, RuntimeTargetId> profile = DeploymentModuleProfile.<ModuleId, ProfileId, RuntimeTargetId>builder()
                .schemaVersion(1L)
                .profileId(CONTENT_PROFILE_A)
                .runtimeTarget(CONTENT_NODE)
                .configurationGeneration(1L)
                .enable(AUTO_POST_SYSTEM, CONTENT_DISCOVERY, CONTENT_VIDEO_WATCH, CONTENT_CREATIVE, CONTENT_PUBLISHING, CONTENT_REPORTING)
                .disable(CONTENT_ENGAGEMENT, CONTENT_INBOUND)
                .build();

        Map<ModuleId, List<ModuleId>> dependencies = Map.of(
                AUTO_POST_SYSTEM, List.of(),
                CONTENT_DISCOVERY, List.of(AUTO_POST_SYSTEM),
                CONTENT_VIDEO_WATCH, List.of(AUTO_POST_SYSTEM),
                CONTENT_CREATIVE, List.of(AUTO_POST_SYSTEM),
                CONTENT_ENGAGEMENT, List.of(AUTO_POST_SYSTEM),
                CONTENT_INBOUND, List.of(AUTO_POST_SYSTEM),
                CONTENT_PUBLISHING, List.of(AUTO_POST_SYSTEM),
                CONTENT_REPORTING, List.of(AUTO_POST_SYSTEM)
        );

        Set<ModuleId> available = Set.of(
                AUTO_POST_SYSTEM, CONTENT_DISCOVERY, CONTENT_VIDEO_WATCH, CONTENT_CREATIVE,
                CONTENT_ENGAGEMENT, CONTENT_INBOUND, CONTENT_PUBLISHING, CONTENT_REPORTING
        );

        IModuleProfileResolver<ModuleId, ProfileId> resolver = new ModuleProfileResolver<>();
        EffectiveDeploymentModuleProfile<ModuleId, ProfileId, RuntimeTargetId> effective = resolver.resolve(
                profile,
                available,
                m -> dependencies.getOrDefault(m, List.of())
        );

        assertEquals(CONTENT_PROFILE_A, effective.profileId());
        assertEquals(CONTENT_NODE, effective.runtimeTarget());
        // Parent system is first in load order
        assertEquals(AUTO_POST_SYSTEM, effective.loadOrder().get(0));
        assertEquals(6, effective.loadOrder().size());
        assertTrue(effective.modules().contains(CONTENT_DISCOVERY));
        assertTrue(effective.modules().contains(CONTENT_VIDEO_WATCH));
        assertTrue(effective.modules().contains(CONTENT_CREATIVE));
        assertTrue(effective.modules().contains(CONTENT_PUBLISHING));
        assertTrue(effective.modules().contains(CONTENT_REPORTING));
        assertFalse(effective.modules().contains(CONTENT_ENGAGEMENT));
        assertFalse(effective.modules().contains(CONTENT_INBOUND));
    }

    @Test
    void contentDeploymentB() {
        // Content deployment B: Auto Post SYSTEM, Creative, Reporting enabled; Discovery, Video Watch, Engagement, Inbound, Publishing disabled
        DeploymentModuleProfile<ModuleId, ProfileId, RuntimeTargetId> profile = DeploymentModuleProfile.<ModuleId, ProfileId, RuntimeTargetId>builder()
                .schemaVersion(1L)
                .profileId(CONTENT_PROFILE_B)
                .runtimeTarget(CONTENT_NODE)
                .configurationGeneration(2L)
                .enable(AUTO_POST_SYSTEM, CONTENT_CREATIVE, CONTENT_REPORTING)
                .disable(CONTENT_DISCOVERY, CONTENT_VIDEO_WATCH, CONTENT_ENGAGEMENT, CONTENT_INBOUND, CONTENT_PUBLISHING)
                .build();

        Map<ModuleId, List<ModuleId>> dependencies = Map.of(
                AUTO_POST_SYSTEM, List.of(),
                CONTENT_DISCOVERY, List.of(AUTO_POST_SYSTEM),
                CONTENT_VIDEO_WATCH, List.of(AUTO_POST_SYSTEM),
                CONTENT_CREATIVE, List.of(AUTO_POST_SYSTEM),
                CONTENT_ENGAGEMENT, List.of(AUTO_POST_SYSTEM),
                CONTENT_INBOUND, List.of(AUTO_POST_SYSTEM),
                CONTENT_PUBLISHING, List.of(AUTO_POST_SYSTEM),
                CONTENT_REPORTING, List.of(AUTO_POST_SYSTEM)
        );

        Set<ModuleId> available = Set.of(
                AUTO_POST_SYSTEM, CONTENT_DISCOVERY, CONTENT_VIDEO_WATCH, CONTENT_CREATIVE,
                CONTENT_ENGAGEMENT, CONTENT_INBOUND, CONTENT_PUBLISHING, CONTENT_REPORTING
        );

        IModuleProfileResolver<ModuleId, ProfileId> resolver = new ModuleProfileResolver<>();
        EffectiveDeploymentModuleProfile<ModuleId, ProfileId, RuntimeTargetId> effective = resolver.resolve(
                profile,
                available,
                m -> dependencies.getOrDefault(m, List.of())
        );

        assertEquals(CONTENT_PROFILE_B, effective.profileId());
        assertEquals(List.of(AUTO_POST_SYSTEM, CONTENT_CREATIVE, CONTENT_REPORTING), effective.loadOrder());
        assertEquals(Set.of(AUTO_POST_SYSTEM, CONTENT_CREATIVE, CONTENT_REPORTING), effective.modules());
        assertEquals(5, effective.disabledModules().size());
        assertFalse(effective.modules().contains(CONTENT_DISCOVERY));
        assertFalse(effective.modules().contains(CONTENT_PUBLISHING));
    }

    @Test
    void rejectsEnabledDisabledConflict() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> DeploymentModuleProfile.of(
                        1L,
                        PAY_STRIPE_PROFILE,
                        COMMERCE_RUNTIME,
                        0L,
                        Set.of(PAY_STRIPE),
                        Set.of(PAY_STRIPE) // Conflict!
                )
        );
        assertTrue(error.getMessage().contains("Module cannot be both enabled and disabled"));
    }

    @Test
    void rejectsDisabledRequiredDependencyWithoutSilentlyEnablingIt() {
        // Module requires a dependency that is explicitly disabled -> MUST fail closed!
        DeploymentModuleProfile<ModuleId, ProfileId, RuntimeTargetId> profile = DeploymentModuleProfile.<ModuleId, ProfileId, RuntimeTargetId>builder()
                .schemaVersion(1L)
                .profileId(PAY_STRIPE_PROFILE)
                .runtimeTarget(COMMERCE_RUNTIME)
                .configurationGeneration(1L)
                .enable(PAY_STRIPE)
                .disable(PAY_SYSTEM) // Required by PAY_STRIPE!
                .build();

        Map<ModuleId, List<ModuleId>> dependencies = Map.of(
                PAY_STRIPE, List.of(PAY_SYSTEM),
                PAY_SYSTEM, List.of()
        );

        IModuleProfileResolver<ModuleId, ProfileId> resolver = new ModuleProfileResolver<>();
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> resolver.resolve(
                        profile,
                        Set.of(PAY_STRIPE, PAY_SYSTEM),
                        m -> dependencies.getOrDefault(m, List.of())
                )
        );

        assertTrue(error.getMessage().contains("Required dependency is explicitly disabled"));
        assertTrue(error.getMessage().contains(PAY_SYSTEM.toString()));
    }

    @Test
    void rejectsUnsupportedSchemaVersion() {
        DeploymentModuleProfile<ModuleId, ProfileId, RuntimeTargetId> profile = DeploymentModuleProfile.<ModuleId, ProfileId, RuntimeTargetId>builder()
                .schemaVersion(2L) // Unsupported schema version!
                .profileId(PAY_STRIPE_PROFILE)
                .runtimeTarget(COMMERCE_RUNTIME)
                .configurationGeneration(0L)
                .enable(PAY_SYSTEM, PAY_STRIPE)
                .disable(PAY_PAYPAL)
                .build();

        IModuleProfileResolver<ModuleId, ProfileId> resolver = new ModuleProfileResolver<>();
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve(
                        profile,
                        Set.of(PAY_SYSTEM, PAY_STRIPE, PAY_PAYPAL),
                        m -> List.of()
                )
        );
        assertTrue(error.getMessage().contains("Unsupported deployment profile schema version: 2"));
    }

    @Test
    void rejectsUnknownDisabledModuleNotInAvailableCatalog() {
        ModuleId misspelledDisabled = ModuleId.of("tavall-pay-paypall"); // Typo/misconfiguration not in catalog!
        DeploymentModuleProfile<ModuleId, ProfileId, RuntimeTargetId> profile = DeploymentModuleProfile.<ModuleId, ProfileId, RuntimeTargetId>builder()
                .schemaVersion(1L)
                .profileId(PAY_STRIPE_PROFILE)
                .runtimeTarget(COMMERCE_RUNTIME)
                .configurationGeneration(0L)
                .enable(PAY_SYSTEM, PAY_STRIPE)
                .disable(misspelledDisabled)
                .build();

        IModuleProfileResolver<ModuleId, ProfileId> resolver = new ModuleProfileResolver<>();
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve(
                        profile,
                        Set.of(PAY_SYSTEM, PAY_STRIPE, PAY_PAYPAL), // misspelledDisabled is not in catalog!
                        m -> List.of()
                )
        );
        assertTrue(error.getMessage().contains("Unknown disabled module in deployment profile"));
        assertTrue(error.getMessage().contains("tavall-pay-paypall"));
        assertTrue(error.getMessage().contains("not present in available catalog"));
    }

    @Test
    void rejectsConfigurationFences() {
        // Non-positive schema version
        assertThrows(
                IllegalArgumentException.class,
                () -> DeploymentModuleProfile.of(
                        0L,
                        PAY_STRIPE_PROFILE,
                        COMMERCE_RUNTIME,
                        0L,
                        Set.of(PAY_SYSTEM),
                        Set.of()
                )
        );

        // Negative configuration generation
        assertThrows(
                IllegalArgumentException.class,
                () -> DeploymentModuleProfile.of(
                        1L,
                        PAY_STRIPE_PROFILE,
                        COMMERCE_RUNTIME,
                        -1L,
                        Set.of(PAY_SYSTEM),
                        Set.of()
                )
        );

        // Null profileId
        assertThrows(
                NullPointerException.class,
                () -> DeploymentModuleProfile.of(
                        1L,
                        null,
                        COMMERCE_RUNTIME,
                        0L,
                        Set.of(PAY_SYSTEM),
                        Set.of()
                )
        );

        // Null runtimeTarget
        assertThrows(
                NullPointerException.class,
                () -> DeploymentModuleProfile.of(
                        1L,
                        PAY_STRIPE_PROFILE,
                        null,
                        0L,
                        Set.of(PAY_SYSTEM),
                        Set.of()
                )
        );

        // Null module collection
        assertThrows(
                NullPointerException.class,
                () -> DeploymentModuleProfile.<ModuleId, ProfileId, RuntimeTargetId>of(
                        1L,
                        PAY_STRIPE_PROFILE,
                        COMMERCE_RUNTIME,
                        0L,
                        null,
                        Set.of()
                )
        );
    }

    @Test
    void verifiesCanonicalIdentityEqualityAndPrivateConstructors() throws Exception {
        // Canonical CustomEnum identity equality: same registered name returns identical object reference
        ModuleId id1 = ModuleId.of("canonical-test-module");
        ModuleId id2 = ModuleId.of("canonical-test-module");
        assertSame(id1, id2);
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());

        ProfileId pid1 = ProfileId.of("canonical-test-profile");
        ProfileId pid2 = ProfileId.of("canonical-test-profile");
        assertSame(pid1, pid2);

        RuntimeTargetId r1 = RuntimeTargetId.of("canonical-target");
        RuntimeTargetId r2 = RuntimeTargetId.of("canonical-target");
        assertSame(r1, r2);

        // Verify test domain constructors are private
        for (Class<?> clazz : List.of(ModuleId.class, ProfileId.class, RuntimeTargetId.class)) {
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                assertTrue(Modifier.isPrivate(constructor.getModifiers()),
                        "Constructor must be private for " + clazz.getSimpleName());
            }
        }
    }

    @Test
    void interfaceFirstDiManagementContract() {
        // Verify interfaces and delegates
        assertTrue(IDependencyInjectableInterface.class.isAssignableFrom(IModuleProfileResolver.class));
        assertTrue(IDependencyInjectableConcrete.class.isAssignableFrom(ModuleProfileResolver.class));
        assertTrue(IModuleProfileResolver.class.isAssignableFrom(ModuleProfileResolver.class));

        DelegatesTo annotation = ModuleProfileResolver.class.getAnnotation(DelegatesTo.class);
        assertNotNull(annotation);
        assertEquals(1, annotation.value().length);
        assertEquals(IModuleProfileResolver.class, annotation.value()[0]);

        // Verify DI registration via DependencyInjectorHelper
        TestableInjector helper = new TestableInjector();
        helper.scanPackage("org.tavall.dependency.module", getClass().getClassLoader());
        helper.registerDependenciesViaAnnotation();

        IModuleProfileResolver<?, ?> resolvedFromDi = DependencyMap.getDependencyMap()
                .getInstance(IModuleProfileResolver.class);
        assertNotNull(resolvedFromDi);
        assertTrue(resolvedFromDi instanceof ModuleProfileResolver);
    }

    private static final class TestableInjector extends DependencyInjectorHelper<Object, Object> {
    }

    // --- Canonical CustomEnum test domain classes with private constructors and register factories ---

    private static final class ModuleId extends CustomEnum<ModuleId> {
        private ModuleId(String name) {
            super(name);
        }

        private static ModuleId of(String name) {
            return register(ModuleId.class, name, ModuleId::new);
        }
    }

    private static final class ProfileId extends CustomEnum<ProfileId> {
        private ProfileId(String name) {
            super(name);
        }

        private static ProfileId of(String name) {
            return register(ProfileId.class, name, ProfileId::new);
        }
    }

    private static final class RuntimeTargetId extends CustomEnum<RuntimeTargetId> {
        private RuntimeTargetId(String name) {
            super(name);
        }

        private static RuntimeTargetId of(String name) {
            return register(RuntimeTargetId.class, name, RuntimeTargetId::new);
        }
    }
}
