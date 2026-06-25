package org.tavall.dependency.composition.fixtures;

import org.tavall.dependency.IDependencyInjectableInterface;

public interface ITestPlayerData extends IDependencyInjectableInterface {
    void addCoins(long amount);

    long coins();

    void resetCoins();
}
