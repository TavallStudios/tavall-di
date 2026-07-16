package org.tavall.dependency.fixtures.contracts.interfaces;

public interface IRedis extends org.tavall.dependency.IDependencyInjectableInterface {

    default void connectToRedis() {
    }

    default void disconnectFromRedis() {
    }

    default void publishRedisUpdate(String message) {
    }
}
