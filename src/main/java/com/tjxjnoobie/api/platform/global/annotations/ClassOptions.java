package com.tjxjnoobie.api.platform.global.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to configure per-class options for DI and logging behavior.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ClassOptions {
    /**
     * Enable exception wrapping for all methods of this class (if it's an interface).
     */
    boolean exceptionWrapping() default false;
    
    /**
     * Enable verbose logging for injection events involving this class.
     */
    boolean logEligibleInjection() default false;
    
    /**
     * Tags for categorizing or filtering classes.
     */
    String[] tags() default {};
    
    /**
     * Log verbosity level (1=normal, 2=verbose, 3=debug).
     */
    int logVerbosityLevel() default 1;
    
    /**
     * Enable caching of instances.
     */
    boolean cacheInstances() default true;
    
    /**
     * Enable lazy initialization.
     */
    boolean lazyInitialization() default false;
    
    /**
     * Treat as singleton scope.
     */
    boolean singletonScope() default false;
    
    /**
     * Enable proxy creation.
     */
    boolean proxyCreationEnabled() default true;
    
    /**
     * Enable strict dependency validation.
     */
    boolean dependencyValidationStrict() default false;
    
    /**
     * Enable performance monitoring.
     */
    boolean performanceMonitoring() default false;
}