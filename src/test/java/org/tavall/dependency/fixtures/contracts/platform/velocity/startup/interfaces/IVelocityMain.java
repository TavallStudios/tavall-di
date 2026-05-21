package org.tavall.dependency.fixtures.contracts.platform.velocity.startup.interfaces;

public interface IVelocityMain extends org.tavall.dependency.IDependencyInjectableInterface {

    default void onProxyInitialization(Object event) throws Throwable {
    }
}
