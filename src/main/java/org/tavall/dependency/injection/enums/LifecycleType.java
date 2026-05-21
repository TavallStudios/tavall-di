/*
 * TJVD License (TJ ValentineÃ¢â‚¬â„¢s Discretionary License) Ã¢â‚¬â€ Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.injection.enums;

import org.tavall.dependency.annotations.PostConstruct;
import org.tavall.dependency.annotations.PreConstruct;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public enum LifecycleType {
    PRE_CONSTRUCT(PreConstruct.class),
    POST_CONSTRUCT(PostConstruct.class);

    private final Class<? extends Annotation> annotationType;

    LifecycleType(Class<? extends Annotation> annotationType) {
        this.annotationType = annotationType;
    }

    public boolean matches(Method method) {
        return method.isAnnotationPresent(annotationType) && method.getParameterCount() == 0;
    }

    public Optional<Method> findIn(Class<?> clazz) {
        List<Method> methods = findAllIn(clazz);
        return methods.isEmpty() ? Optional.empty() : Optional.of(methods.getFirst());
    }

    public List<Method> findAllIn(Class<?> clazz) {
        if (clazz == null) {
            return List.of();
        }

        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(this::matches)
                .sorted(Comparator
                        .comparingInt(this::priority)
                        .thenComparing(Method::getName))
                .toList();
    }

    private int priority(Method method) {
        if (annotationType == PreConstruct.class) {
            return method.getAnnotation(PreConstruct.class).priority();
        }
        return 0;
    }
}
