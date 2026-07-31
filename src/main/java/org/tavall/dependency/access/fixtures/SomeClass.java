package org.tavall.dependency.access.fixtures;

import org.tavall.dependency.DependencyAccess;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.dependency.annotations.GrantDependencyAccess;

@GrantDependencyAccess
@DelegatesTo(ISomeClass.class)
public final class SomeClass<SomeOtherTypeParameterValue>
        implements DependencyAccess<
                DependencyOneValue,
                DependencyTwoValue,
                DependencyThreeValue,
                DependencyFourValue
        > {

    public ISomeClass lookupDelegatedInterface() {
        return findInstance(ISomeClass.class);
    }
}

interface DependencyOneValue {
}

interface DependencyTwoValue {
}

interface DependencyThreeValue {
}

interface DependencyFourValue {
}
