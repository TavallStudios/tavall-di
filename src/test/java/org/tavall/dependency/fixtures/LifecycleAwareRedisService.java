package org.tavall.dependency.fixtures;

import org.tavall.dependency.IDependencyInjectableConcrete;
import org.tavall.dependency.fixtures.contracts.interfaces.IRedis;
import org.tavall.dependency.fixtures.contracts.interfaces.IUtils;
import org.tavall.dependency.annotations.Inject;
import org.tavall.dependency.annotations.PostConstruct;
import org.tavall.dependency.annotations.PreConstruct;

import java.util.ArrayList;
import java.util.List;

public class LifecycleAwareRedisService implements IRedis, IDependencyInjectableConcrete {
    @Inject private IUtils utils;
    private final List<String> lifecycleEvents = new ArrayList<>();

    @PreConstruct(priority = 2)
    private void preConstructSecond() {
        lifecycleEvents.add("pre-2");
    }

    @PreConstruct(priority = 1)
    private void preConstructFirst() {
        lifecycleEvents.add("pre-1");
    }

    @PostConstruct
    private void postConstruct() {
        lifecycleEvents.add("post");
    }

    public IUtils getInjectedUtils() {
        return utils;
    }

    public List<String> getLifecycleEvents() {
        return lifecycleEvents;
    }
}
