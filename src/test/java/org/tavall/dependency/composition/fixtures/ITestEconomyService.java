package org.tavall.dependency.composition.fixtures;

import org.tavall.dependency.IDependencyInjectableInterface;

public interface ITestEconomyService extends IDependencyInjectableInterface {
    void recordTransaction(long amount);

    long transactionCount();

    long totalRecordedAmount();
}
