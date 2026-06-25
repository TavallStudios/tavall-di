package org.tavall.dependency.composition.fixtures;

import org.tavall.dependency.composition.IDependencyBundle;

public record TestPlayerRewardDependencies(
        ITestPlayerData playerData,
        ITestEconomyService economyService,
        ITestAuditLogger auditLogger
) implements IDependencyBundle {
}
