package com.tjxjnoobie.api.platform.velocity.startup.interfaces;

public interface IVelocityMain extends com.tjxjnoobie.api.dependency.IDependencyInjectableInterface {

    default void onProxyInitialization(Object event) throws Throwable {
    }
}
