package org.tavall.dependency.composition.fixtures;

import org.tavall.dependency.IDependencyInjectableConcrete;
import org.tavall.dependency.annotations.DelegatesTo;

@DelegatesTo(ITestEconomyService.class)
public final class TestEconomyService implements ITestEconomyService, IDependencyInjectableConcrete {
    private long transactionCount;
    private long totalRecordedAmount;

    @Override
    public void recordTransaction(long amount) {
        transactionCount++;
        totalRecordedAmount += amount;
    }

    @Override
    public long transactionCount() {
        return transactionCount;
    }

    @Override
    public long totalRecordedAmount() {
        return totalRecordedAmount;
    }
}
