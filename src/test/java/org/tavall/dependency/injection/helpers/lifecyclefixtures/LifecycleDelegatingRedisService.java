package org.tavall.dependency.injection.helpers.lifecyclefixtures;

import org.tavall.dependency.IDependencyInjectableConcrete;
import org.tavall.dependency.annotations.DelegatesToInterface;
import org.tavall.dependency.fixtures.contracts.interfaces.IRedis;
import org.tavall.dependency.fixtures.contracts.interfaces.IUtils;
import org.tavall.dependency.annotations.Inject;
import org.tavall.dependency.annotations.PostConstruct;

@DelegatesToInterface(getLinkedInterface = IRedis.class)
public class LifecycleDelegatingRedisService implements IRedis, IDependencyInjectableConcrete {
    private static boolean postConstructCalled;

    @Inject private IUtils utils;

    public static void reset() {
        postConstructCalled = false;
    }

    public static boolean isPostConstructCalled() {
        return postConstructCalled;
    }

    public IUtils getInjectedUtils() {
        return utils;
    }

    @PostConstruct
    private void postConstruct() {
        postConstructCalled = true;
    }
}
