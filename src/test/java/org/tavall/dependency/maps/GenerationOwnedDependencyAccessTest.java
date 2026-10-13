package org.tavall.dependency.maps;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.tavall.dependency.DependencyAccess;
import org.tavall.dependency.GenerationOwnedDependencyAccess;
import org.tavall.dependency.annotations.DelegatesTo;

final class GenerationOwnedDependencyAccessTest {
    interface Value { String value(); }
    interface Consumer { String read(); }

    @DelegatesTo(Consumer.class)
    public static final class BoundConsumer extends GenerationOwnedDependencyAccess
            implements Consumer, DependencyAccess<Value> {
        @Override public String read() { return getInstance().value(); }
    }

    @Test
    void metadataAliasesShareOneOwnerAndNeverUseTheGlobalCompatibilityMap() {
        DependencyMap first = new DependencyMap();
        DependencyMap second = new DependencyMap();
        first.registerInstance(Value.class, () -> "first");
        second.registerInstance(Value.class, () -> "second");
        BoundConsumer a = first.registerInstance(BoundConsumer.class, new BoundConsumer());
        BoundConsumer b = second.registerInstance(BoundConsumer.class, new BoundConsumer());
        assertEquals("first", a.read());
        assertEquals("second", b.read());
        assertSame(first.findMetaData(BoundConsumer.class), first.findMetaData(Consumer.class));
        assertThrows(IllegalStateException.class, () -> second.registerInstance(BoundConsumer.class, a));
        first.clear();
        assertThrows(IllegalStateException.class, a::read);
        assertEquals("second", b.read());
    }

    @Test
    void supplierRegistrationAndReplacementBindBeforeConsumptionAndRetireOldReferences() {
        DependencyMap map = new DependencyMap();
        map.registerInstance(Value.class, () -> "value");
        BoundConsumer original = map.registerInstance(BoundConsumer.class, (java.util.function.Supplier<BoundConsumer>) BoundConsumer::new);
        assertEquals("value", original.read());
        BoundConsumer replacement = map.replaceInstance(BoundConsumer.class, (java.util.function.Supplier<BoundConsumer>) BoundConsumer::new);
        assertSame(replacement, map.getInstance(Consumer.class));
        assertThrows(IllegalStateException.class, original::read);
        assertEquals("value", replacement.read());
        map.removeDependency(Consumer.class);
        assertFalse(map.isInstanceRegistered(BoundConsumer.class));
        assertThrows(IllegalStateException.class, replacement::read);
    }
    @Test
    void childReferencesNeverBecomeAnotherOwnerOrMutationAuthority() {
        DependencyMap parent = new DependencyMap();
        parent.registerInstance(Value.class, () -> "parent");
        BoundConsumer consumer = parent.registerInstance(BoundConsumer.class, new BoundConsumer());
        DependencyMap child = new DependencyMap();
        child.registerDependency(Consumer.class, parent.findMetaData(Consumer.class));
        assertEquals("parent", child.getInstance(Consumer.class).read());
        assertThrows(IllegalStateException.class, () -> child.replaceInstance(Consumer.class,
                (java.util.function.Supplier<Consumer>) BoundConsumer::new));
        child.clear();
        assertEquals("parent", consumer.read());
        parent.clear();
        assertThrows(IllegalStateException.class, consumer::read);
    }
}
