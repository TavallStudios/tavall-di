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

    @Test
    void retainedHandleAfterRemoveClearOrReplacementCannotResurrectBySameObjectRegistration() {
        DependencyMap map = new DependencyMap();
        map.registerInstance(Value.class, () -> "val");

        // 1. After removeDependency
        BoundConsumer first = map.registerInstance(BoundConsumer.class, new BoundConsumer());
        assertEquals("val", first.read());
        map.removeDependency(BoundConsumer.class);
        assertTrue(first.isDependencyGenerationRetired());
        assertThrows(IllegalStateException.class, first::read);
        assertThrows(IllegalStateException.class, () -> map.registerInstance(BoundConsumer.class, first));
        assertThrows(IllegalStateException.class, first::read);

        // 2. After clear
        BoundConsumer second = map.registerInstance(BoundConsumer.class, new BoundConsumer());
        assertEquals("val", second.read());
        map.clear();
        assertTrue(second.isDependencyGenerationRetired());
        assertThrows(IllegalStateException.class, second::read);
        assertThrows(IllegalStateException.class, () -> map.registerInstance(BoundConsumer.class, second));
        assertThrows(IllegalStateException.class, second::read);

        // 3. After replacement
        map.registerInstance(Value.class, () -> "val");
        BoundConsumer third = map.registerInstance(BoundConsumer.class, new BoundConsumer());
        assertEquals("val", third.read());
        BoundConsumer fourth = map.replaceInstance(BoundConsumer.class, (java.util.function.Supplier<BoundConsumer>) BoundConsumer::new);
        assertTrue(third.isDependencyGenerationRetired());
        assertThrows(IllegalStateException.class, third::read);
        assertThrows(IllegalStateException.class, () -> map.registerInstance(BoundConsumer.class, third));
        assertThrows(IllegalStateException.class, third::read);
        assertEquals("val", fourth.read());
        assertSame(fourth, map.getInstance(BoundConsumer.class));
    }

    @Test
    void replacementWithSameObjectRejectsWithoutCorruptingCurrentLiveMapping() {
        DependencyMap map = new DependencyMap();
        map.registerInstance(Value.class, () -> "active");
        BoundConsumer live = map.registerInstance(BoundConsumer.class, new BoundConsumer());
        assertEquals("active", live.read());
        assertFalse(live.isDependencyGenerationRetired());
        assertTrue(live.isDependencyGenerationOwnedBy(map));

        // Attempt replacing with the same live instance
        assertThrows(IllegalStateException.class, () -> map.replaceInstance(BoundConsumer.class, () -> live));

        // Live mapping must remain uncorrupted and fully operational
        assertFalse(live.isDependencyGenerationRetired());
        assertTrue(live.isDependencyGenerationOwnedBy(map));
        assertSame(live, map.getInstance(BoundConsumer.class));
        assertSame(live, map.getInstance(Consumer.class));
        assertEquals("active", live.read());

        // Attempt replacing with an already-retired instance from another map
        BoundConsumer retired = new BoundConsumer();
        DependencyMap temp = new DependencyMap();
        temp.registerInstance(Value.class, () -> "temp");
        temp.registerInstance(BoundConsumer.class, retired);
        temp.removeDependency(BoundConsumer.class);
        assertTrue(retired.isDependencyGenerationRetired());

        assertThrows(IllegalStateException.class, () -> map.replaceInstance(BoundConsumer.class, () -> retired));

        // Live mapping still untouched
        assertSame(live, map.getInstance(BoundConsumer.class));
        assertSame(live, map.getInstance(Consumer.class));
        assertEquals("active", live.read());
    }

    @Test
    void freshReplacementSucceedsAndRetiresOldReference() {
        DependencyMap map = new DependencyMap();
        map.registerInstance(Value.class, () -> "payload");
        BoundConsumer original = map.registerInstance(BoundConsumer.class, new BoundConsumer());
        assertEquals("payload", original.read());

        BoundConsumer fresh = map.replaceInstance(BoundConsumer.class, (java.util.function.Supplier<BoundConsumer>) BoundConsumer::new);
        assertNotSame(original, fresh);
        assertEquals("payload", fresh.read());
        assertSame(fresh, map.getInstance(BoundConsumer.class));
        assertSame(fresh, map.getInstance(Consumer.class));
        assertTrue(original.isDependencyGenerationRetired());
        assertThrows(IllegalStateException.class, original::read);
    }

    @Test
    void parentBorrowedReferencesRemainLiveAcrossChildRemoveAndClear() {
        DependencyMap parent = new DependencyMap();
        parent.registerInstance(Value.class, () -> "parent-val");
        BoundConsumer parentConsumer = parent.registerInstance(BoundConsumer.class, new BoundConsumer());

        DependencyMap child = new DependencyMap();
        child.registerDependency(Consumer.class, parent.findMetaData(Consumer.class));
        assertEquals("parent-val", child.getInstance(Consumer.class).read());

        // Child removeDependency must not detach or retire the parent-owned instance
        child.removeDependency(Consumer.class);
        assertFalse(parentConsumer.isDependencyGenerationRetired());
        assertTrue(parentConsumer.isDependencyGenerationOwnedBy(parent));
        assertEquals("parent-val", parentConsumer.read());

        // Re-borrow and test child clear
        child.registerDependency(Consumer.class, parent.findMetaData(Consumer.class));
        assertEquals("parent-val", child.getInstance(Consumer.class).read());
        child.clear();
        assertFalse(parentConsumer.isDependencyGenerationRetired());
        assertTrue(parentConsumer.isDependencyGenerationOwnedBy(parent));
        assertEquals("parent-val", parentConsumer.read());

        // Parent clear retires the instance
        parent.clear();
        assertTrue(parentConsumer.isDependencyGenerationRetired());
        assertThrows(IllegalStateException.class, parentConsumer::read);
    }

    @Test
    void idempotentLiveAliasBindingPreserved() {
        DependencyMap map = new DependencyMap();
        map.registerInstance(Value.class, () -> "live-val");
        BoundConsumer instance = map.registerInstance(BoundConsumer.class, new BoundConsumer());

        // Registering another declared alias for the live instance succeeds idempotently
        Consumer alias = map.registerInstance(Consumer.class, instance);
        assertSame(instance, alias);
        assertSame(map.findMetaData(BoundConsumer.class), map.findMetaData(Consumer.class));
        assertEquals("live-val", instance.read());
        assertFalse(instance.isDependencyGenerationRetired());
    }

    public static final class FailingInitConsumer extends GenerationOwnedDependencyAccess implements Consumer {
        @org.tavall.dependency.annotations.PostConstruct
        public void postConstruct() {
            throw new RuntimeException("Simulated initialization failure");
        }
        @Override public String read() { return "fail"; }
    }

    @Test
    void failedInitializationRollbackLeavesInstanceRetiredAndRestoresPreviousMapping() {
        DependencyMap map = new DependencyMap();
        map.registerInstance(Value.class, () -> "prev");
        BoundConsumer original = map.registerInstance(BoundConsumer.class, new BoundConsumer());
        assertEquals("prev", original.read());

        FailingInitConsumer failing = new FailingInitConsumer();
        assertThrows(RuntimeException.class, () -> map.registerInstance(Consumer.class, failing));

        // The failing instance must be retired and fail closed
        assertTrue(failing.isDependencyGenerationRetired());
        assertThrows(IllegalStateException.class, failing::getDependencyMap);
        assertThrows(IllegalStateException.class, () -> map.registerInstance(Consumer.class, failing));

        // Previous mapping was restored during rollback
        assertSame(original, map.getInstance(Consumer.class));
        assertEquals("prev", original.read());
    }
}
