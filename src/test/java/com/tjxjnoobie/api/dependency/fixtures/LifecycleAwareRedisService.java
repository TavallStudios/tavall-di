package com.tjxjnoobie.api.dependency.fixtures;

import com.tjxjnoobie.api.dependency.IDependencyInjectableConcrete;
import com.tjxjnoobie.api.interfaces.IRedis;
import com.tjxjnoobie.api.interfaces.IUtils;
import com.tjxjnoobie.api.platform.global.annotations.Inject;
import com.tjxjnoobie.api.platform.global.annotations.PostConstruct;
import com.tjxjnoobie.api.platform.global.annotations.PreConstruct;

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
