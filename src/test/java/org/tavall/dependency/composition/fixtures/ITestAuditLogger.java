package org.tavall.dependency.composition.fixtures;

import org.tavall.dependency.IDependencyInjectableInterface;

public interface ITestAuditLogger extends IDependencyInjectableInterface {
    void audit(String message);

    int auditCount();

    String lastAuditMessage();
}
