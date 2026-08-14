package org.tavall.dependency.module;

import org.junit.jupiter.api.Test;
import org.tavall.identity.CustomEnum;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModuleProfileResolverTest {
    private static final ModuleId CORE = ModuleId.of("tavall-paper-core");
    private static final ModuleId UI = ModuleId.of("tavall-ui");
    private static final ModuleId LOBBY = ModuleId.of("tavall-lobby");
    private static final ModuleId MISSING = ModuleId.of("missing");
    private static final ProfileId LOBBY_PROFILE = ProfileId.of("lobby");

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

    private static final class ModuleId extends CustomEnum<ModuleId> {
        private ModuleId(String value) {
            super(value);
        }

        private static ModuleId of(String value) {
            return new ModuleId(value);
        }
    }

    private static final class ProfileId extends CustomEnum<ProfileId> {
        private ProfileId(String value) {
            super(value);
        }

        private static ProfileId of(String value) {
            return new ProfileId(value);
        }
    }
}
