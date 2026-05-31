package org.tavall.dependency.access.fixtures;

import org.tavall.dependency.annotations.DepAccess;
import org.tavall.dependency.annotations.DelegatesToInterface;
import org.tavall.dependency.annotations.GrantDependencyAccess;

@GrantDependencyAccess
@DelegatesToInterface(value = ISomeClass.class)
public final class SomeClass<SomeOtherTypeParameterValue>
        implements DepAccess<
                DependencyOneValue,
                DependencyTwoValue,
                DependencyThreeValue,
                DependencyFourValue
                > {
}

interface DependencyOneValue {
}

interface DependencyTwoValue {
}

interface DependencyThreeValue {
}

interface DependencyFourValue {
}

interface ISomeClass {
}
