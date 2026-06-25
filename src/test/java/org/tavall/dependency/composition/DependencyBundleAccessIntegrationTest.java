package org.tavall.dependency.composition;

import org.tavall.dependency.DependencyLoader;
import org.tavall.dependency.DependencyLoaderAccess;
import org.tavall.dependency.composition.fixtures.ITestAuditLogger;
import org.tavall.dependency.composition.fixtures.ITestEconomyService;
import org.tavall.dependency.composition.fixtures.ITestPlayerData;
import org.tavall.dependency.composition.fixtures.RawDependencyBundleAccessService;
import org.tavall.dependency.composition.fixtures.TestAuditLogger;
import org.tavall.dependency.composition.fixtures.TestEconomyService;
import org.tavall.dependency.composition.fixtures.TestNonRecordBundleService;
import org.tavall.dependency.composition.fixtures.TestPlayerData;
import org.tavall.dependency.composition.fixtures.TestPlayerRewardBundleService;
import org.tavall.dependency.composition.fixtures.TestPlayerRewardDependencies;
import org.tavall.dependency.maps.DependencyMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyBundleAccessIntegrationTest {

    @AfterEach
    void clearDependencyState() {
        DependencyMap.getDependencyMap().clear();
        DependencyLoader.clearNamedLoaders();
    }

    @Test
    void getDependenciesHydratesTheRecordFromRegisteredMetadata() {
        TestPlayerData playerData = new TestPlayerData();
        TestEconomyService economyService = new TestEconomyService();
        TestAuditLogger auditLogger = new TestAuditLogger();

        DependencyLoaderAccess.registerInstance(ITestPlayerData.class, playerData);
        DependencyLoaderAccess.registerInstance(ITestEconomyService.class, economyService);
        DependencyLoaderAccess.registerInstance(ITestAuditLogger.class, auditLogger);

        TestPlayerRewardBundleService service = new TestPlayerRewardBundleService();
        IDependencyBundleAccess<TestPlayerRewardDependencies> access = service;

        assertEquals(TestPlayerRewardDependencies.class, access.getDependencyBundleTypeParam());

        TestPlayerRewardDependencies dependencies = access.getDependencies();

        assertNotNull(dependencies);
        assertSame(playerData, dependencies.playerData());
        assertSame(economyService, dependencies.economyService());
        assertSame(auditLogger, dependencies.auditLogger());

        service.rewardPlayer(500L);

        assertEquals(500L, playerData.coins());
        assertEquals(1L, economyService.transactionCount());
        assertEquals(500L, economyService.totalRecordedAmount());
        assertEquals(1, auditLogger.auditCount());
        assertEquals("Rewarded player with 500 coins.", auditLogger.lastAuditMessage());
    }

    @Test
    void rejectsNonRecordAndRawBundleTypes() {
        TestNonRecordBundleService nonRecordService = new TestNonRecordBundleService();
        RawDependencyBundleAccessService rawService = new RawDependencyBundleAccessService();

        RuntimeException nonRecordException = assertThrows(RuntimeException.class, nonRecordService::getDependencies);
        RuntimeException rawException = assertThrows(RuntimeException.class, rawService::getDependencies);

        assertNotNull(nonRecordException.getMessage());
        assertNotNull(rawException.getMessage());
        assertTrue(nonRecordException.getMessage().toLowerCase().contains("record")
                || nonRecordException.getMessage().toLowerCase().contains("bundle"));
        assertTrue(rawException.getMessage().toLowerCase().contains("bundle")
                || rawException.getMessage().toLowerCase().contains("generic"));
    }
}
