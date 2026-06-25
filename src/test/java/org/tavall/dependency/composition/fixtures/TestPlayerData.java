package org.tavall.dependency.composition.fixtures;

import org.tavall.dependency.IDependencyInjectableConcrete;
import org.tavall.dependency.annotations.DelegatesToInterface;

@DelegatesToInterface(
        getLinkedInterface = ITestPlayerData.class,
        getLinkedInterfaces = {ITestPlayerWallet.class})
public final class TestPlayerData implements ITestPlayerData, ITestPlayerWallet, IDependencyInjectableConcrete {
    private long coins;

    @Override
    public void addCoins(long amount) {
        coins += amount;
    }

    @Override
    public long coins() {
        return coins;
    }

    @Override
    public void resetCoins() {
        coins = 0L;
    }

    @Override
    public void spendCoins(long amount) {
        coins -= amount;
    }

    @Override
    public long walletCoins() {
        return coins;
    }
}
