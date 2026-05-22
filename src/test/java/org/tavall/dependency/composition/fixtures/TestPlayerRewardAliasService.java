package org.tavall.dependency.composition.fixtures;

import org.tavall.dependency.composition.IDependencyBundleAccess;

public final class TestPlayerRewardAliasService implements IDependencyBundleAccess<TestPlayerRewardDependencies> {
    public TestPlayerRewardDependencies dependencies() {
        return getDependencies();
    }

    public ITestPlayerData playerData() {
        return dependencies().playerData();
    }

    public ITestEconomyService economyService() {
        return dependencies().economyService();
    }

    public ITestAuditLogger auditLogger() {
        return dependencies().auditLogger();
    }

    public void rewardPlayer(long amount) {
        playerData().addCoins(amount);
        economyService().recordTransaction(amount);
        auditLogger().audit("Rewarded player with " + amount + " coins.");
    }
}
