package com.tjxjnoobie.api.interfaces;

public interface IRedis extends com.tjxjnoobie.api.dependency.IDependencyInjectableInterface {

    default void connectToRedis() {
    }

    default void disconnectFromRedis() {
    }

    default void publishRedisUpdate(String message) {
    }
}
