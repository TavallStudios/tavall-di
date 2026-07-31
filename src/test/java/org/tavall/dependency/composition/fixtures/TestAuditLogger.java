package org.tavall.dependency.composition.fixtures;

import org.tavall.dependency.IDependencyInjectableConcrete;
import org.tavall.dependency.annotations.DelegatesTo;

@DelegatesTo(ITestAuditLogger.class)
public final class TestAuditLogger implements ITestAuditLogger, IDependencyInjectableConcrete {
    private int auditCount;
    private String lastAuditMessage;

    @Override
    public void audit(String message) {
        auditCount++;
        lastAuditMessage = message;
    }

    @Override
    public int auditCount() {
        return auditCount;
    }

    @Override
    public String lastAuditMessage() {
        return lastAuditMessage;
    }
}
