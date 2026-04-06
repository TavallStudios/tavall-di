package com.tjxjnoobie.api.platform.global.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface, class, or abstract class as eligible for dependency injection.
 * Only types marked with this annotation (or in configured packages) will be injected.
 * This prevents accidental injection of third-party library types.
 * 
 * Can be applied to:
 * - Interfaces
 * - Concrete classes
 * - Abstract classes
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Injectable {
    /**
     * Optional description of what this injectable provides
     */
    String value() default "";
}
