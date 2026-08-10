package org.tavall.dependency.injection.helpers.legacyfixtures;

import org.tavall.dependency.IDependencyInjectableConcrete;
import org.tavall.dependency.annotations.DelegatesToInterface;
import org.tavall.dependency.fixtures.contracts.interfaces.IRedis;

@SuppressWarnings("deprecation")
@DelegatesToInterface(IRedis.class)
public final class LegacyDelegatingService implements IRedis, IDependencyInjectableConcrete {
    @Override
    public void connectToRedis() {
    }

    @Override
    public void disconnectFromRedis() {
    }

    @Override
    public void publishRedisUpdate(String message) {
    }
}
