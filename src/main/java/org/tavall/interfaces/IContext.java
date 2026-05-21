package org.tavall.interfaces;

import org.tavall.dependency.IDependencyInjectableInterface;

import java.util.List;

/**
 * Base interface for context management providing dependency injection capabilities.
 *
 * @param <T> the type of context implementation
 */
public interface IContext<T> extends IDependencyInjectableInterface {

    /**
     * Gets the context instance itself.
     *
     * @return the context instance
     */
    T getContext();

    /**
     * Returns a list of all registered context instances.
     *
     * @return list of all context instances, never null
     */
    List<IContext<?>> getAllContexts();
}
