package org.tavall.dependency.composition.fixtures;

import org.tavall.dependency.IDependencyInjectableInterface;

public interface ITestPlayerWallet extends IDependencyInjectableInterface {
    void spendCoins(long amount);

    long walletCoins();
}
