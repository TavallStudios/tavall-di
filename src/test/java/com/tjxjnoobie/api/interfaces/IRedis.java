package com.tjxjnoobie.api.interfaces;

import com.tjxjnoobie.api.dependency.annotations.ComposesToInterface;
import com.tjxjnoobie.api.dependency.composition.domains.IInfrastructureDomain;

@ComposesToInterface(IInfrastructureDomain.class)
public interface IRedis extends com.tjxjnoobie.api.dependency.IDependencyInjectableInterface {

    default void connectToRedis() {
    }

    default void disconnectFromRedis() {
    }

    default void publishRedisUpdate(String message) {
    }
}
