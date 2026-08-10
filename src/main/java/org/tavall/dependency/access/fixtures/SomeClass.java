package org.tavall.dependency.access.fixtures;

import org.tavall.dependency.DependencyAccess;
import org.tavall.dependency.annotations.DelegatesTo;

@DelegatesTo(ISomeClass.class)
public final class SomeClass<SomeOtherTypeParameterValue>
        implements ISomeClass, DependencyAccess<ISomeClass> {

    public ISomeClass lookupDelegatedInterface() {
        return getInstance();
    }
}
