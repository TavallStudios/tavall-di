package org.tavall.identity;

import java.util.Objects;

/**
 * Base type for enum-like identifiers whose set of values can grow without changing a Java enum.
 *
 * <p>Subclasses define the identifier domain. Equality requires both the same concrete domain type
 * and the same canonical value, so values from unrelated domains cannot accidentally compare equal.
 * Raw strings are intentionally exposed only through {@link #value()} for serialization and other
 * boundary code.</p>
 *
 * @param <T> concrete identifier domain
 */
public abstract class CustomEnum<T extends CustomEnum<T>> implements Comparable<T> {
    private final String value;

    protected CustomEnum(String value) {
        this.value = requireValue(value);
    }

    public final String value() {
        return value;
    }

    @Override
    public final int compareTo(T other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value());
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        CustomEnum<?> that = (CustomEnum<?>) other;
        return value.equals(that.value);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), value);
    }

    @Override
    public final String toString() {
        return value;
    }

    private static String requireValue(String value) {
        String canonical = Objects.requireNonNull(value, "value").trim();
        if (canonical.isEmpty()) {
            throw new IllegalArgumentException("Custom enum value must not be blank");
        }
        return canonical;
    }
}
