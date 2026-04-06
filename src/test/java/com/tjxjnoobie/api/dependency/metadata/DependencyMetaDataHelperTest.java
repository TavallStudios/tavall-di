package com.tjxjnoobie.api.dependency.metadata;

import com.tjxjnoobie.api.dependency.injection.helpers.fixtures.DelegatingRedisService;
import com.tjxjnoobie.api.dependency.metadata.wrappers.DependencyInstance;
import com.tjxjnoobie.api.dependency.metadata.wrappers.DependencyInterface;
import com.tjxjnoobie.api.interfaces.IRedis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DependencyMetaDataHelperTest {

    @BeforeEach
    void resetFixture() {
        DelegatingRedisService.reset();
    }

    @Test
    void populatesMetadataAndDispatchesToRealProjectInterfaceOverride() {
        DependencyMetaData<IRedis, DelegatingRedisService> metaData = new DependencyMetaData<>();
        DependencyMetaDataHelper<IRedis, DelegatingRedisService> helper = new DependencyMetaDataHelper<>();

        helper.populateMetaData(
                metaData,
                new DependencyInterface<>(IRedis.class),
                new DependencyInstance<>(DelegatingRedisService.class));

        assertSame(IRedis.class, metaData.getPrimaryInterfaceType());
        assertSame(DelegatingRedisService.class, metaData.getConcreteType());
        assertSame(metaData.getDependencyInstance(), metaData.getDependencyInterface());

        metaData.getDependencyInterface().connectToRedis();

        assertEquals(1, DelegatingRedisService.getConnectCalls());
    }
}
