package org.tavall.identity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomEnumTest {

    @Test
    void valuesAreEqualOnlyInsideTheSameIdentifierDomain() {
        ModuleId first = ModuleId.of("tavall-lobby");
        ModuleId second = ModuleId.of("tavall-lobby");
        ProfileId unrelated = ProfileId.of("tavall-lobby");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, unrelated);
        assertEquals("tavall-lobby", first.value());
        assertEquals("tavall-lobby", first.toString());
    }

    @Test
    void rejectsBlankValuesAndCanonicalizesBoundaryWhitespace() {
        assertThrows(IllegalArgumentException.class, () -> ModuleId.of("   "));
        assertEquals("tavall-ffa", ModuleId.of("  tavall-ffa  ").value());
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
