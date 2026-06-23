package org.tavall.dependency.composition;

import org.tavall.dependency.DependencyLoader;
import org.tavall.dependency.DependencyLoaderAccess;
import org.tavall.dependency.composition.fixtures.ITestAuditLogger;
import org.tavall.dependency.composition.fixtures.ITestEconomyService;
import org.tavall.dependency.composition.fixtures.ITestPlayerData;
import org.tavall.dependency.composition.fixtures.TestAuditLogger;
import org.tavall.dependency.composition.fixtures.TestEconomyService;
import org.tavall.dependency.composition.fixtures.TestPlayerData;
import org.tavall.dependency.composition.fixtures.TestPlayerRewardAliasService;
import org.tavall.dependency.composition.fixtures.TestPlayerRewardDependencies;
import org.tavall.dependency.maps.DependencyMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DependencyBundleAliasAccessIntegrationTest {

    @AfterEach
    void clearDependencyState() {
        DependencyMap.getDependencyMap().clear();
        DependencyLoader.clearNamedLoaders();
    }

    @Test
    void dependenciesMiniContextSuppliesBundleAccess() {
        TestPlayerData playerData = new TestPlayerData();
        TestEconomyService economyService = new TestEconomyService();
        TestAuditLogger auditLogger = new TestAuditLogger();

        DependencyLoaderAccess.registerInstance(ITestPlayerData.class, playerData);
        DependencyLoaderAccess.registerInstance(ITestEconomyService.class, economyService);
        DependencyLoaderAccess.registerInstance(ITestAuditLogger.class, auditLogger);

        IDependencyBundleAccess<TestPlayerRewardDependencies> access = new TestPlayerRewardAliasService();
        TestPlayerRewardAliasService service = (TestPlayerRewardAliasService) access;

        assertEquals(TestPlayerRewardDependencies.class, access.getDependencyBundleTypeParam());
        assertSame(playerData, service.dependencies().playerData());
        assertSame(economyService, service.dependencies().economyService());
        assertSame(auditLogger, service.dependencies().auditLogger());

        service.rewardPlayer(250L);

        assertEquals(250L, playerData.coins());
        assertEquals(1L, economyService.transactionCount());
        assertEquals(250L, economyService.totalRecordedAmount());
        assertEquals(1, auditLogger.auditCount());
        assertEquals("Rewarded player with 250 coins.", auditLogger.lastAuditMessage());
    }
}
