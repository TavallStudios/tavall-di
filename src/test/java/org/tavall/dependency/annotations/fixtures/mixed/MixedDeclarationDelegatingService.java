package org.tavall.dependency.annotations.fixtures.mixed;

import org.tavall.dependency.annotations.DelegatesToInterface;
import org.tavall.dependency.fixtures.contracts.interfaces.IRedis;
import org.tavall.dependency.fixtures.contracts.interfaces.IUtils;
import org.tavall.dependency.fixtures.contracts.platform.velocity.startup.interfaces.IVelocityMain;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@DelegatesToInterface(
        getLinkedInterface = IRedis.class,
        getLinkedInterfaces = {IRedis.class, IUtils.class, IVelocityMain.class}
)
public class MixedDeclarationDelegatingService implements IRedis, IUtils, org.tavall.dependency.IDependencyInjectableConcrete {
    private static final AtomicInteger CONNECT_CALLS = new AtomicInteger();

    private final Map<String, Object> configValues = new HashMap<>();

    public static void reset() {
        CONNECT_CALLS.set(0);
    }

    public static int getConnectCalls() {
        return CONNECT_CALLS.get();
    }

    @Override
    public void connectToRedis() {
        CONNECT_CALLS.incrementAndGet();
    }

    @Override
    public Map<String, Object> getConfigValues() {
        return configValues;
    }

    @Override
    public void setConfigValue(String key, Object value) {
        configValues.put(key, value);
    }
}
