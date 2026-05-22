package org.tavall.dependency.composition.fixtures;

import org.tavall.dependency.composition.IDependencyBundleAccess;

public final class TestPlayerRewardBundleService implements IDependencyBundleAccess<TestPlayerRewardDependencies> {
    public TestPlayerRewardDependencies dependencies() {
        return getDependencies();
    }

    public void rewardPlayer(long amount) {
        dependencies().playerData().addCoins(amount);
        dependencies().economyService().recordTransaction(amount);
        dependencies().auditLogger().audit("Rewarded player with " + amount + " coins.");
    }
}
