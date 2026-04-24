package com.tjxjnoobie.api.dependency.injection.helpers.lifecyclefixtures;

import com.tjxjnoobie.api.dependency.IDependencyInjectableConcrete;
import com.tjxjnoobie.api.dependency.annotations.DelegatesToInterface;
import com.tjxjnoobie.api.interfaces.IRedis;
import com.tjxjnoobie.api.interfaces.IUtils;
import com.tjxjnoobie.api.platform.global.annotations.Inject;
import com.tjxjnoobie.api.platform.global.annotations.PostConstruct;

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
