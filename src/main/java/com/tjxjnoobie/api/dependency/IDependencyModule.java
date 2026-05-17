package com.tjxjnoobie.api.dependency;

/**
 * Contract for project-specific dependency registration modules.
 */
public interface IDependencyModule extends IDependencyInjectableConcrete {
    void registerDependencies();
}
