/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.architecture;

import org.tavall.dependency.DependencyAccess;
import org.tavall.dependency.IDependencyAccess;
import org.tavall.dependency.annotations.CompositionBoundary;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.dependency.annotations.ExplicitNonDi;

import java.lang.reflect.Modifier;
import java.util.Set;

/**
 * Reusable DI architecture semantics, classifications, and invariants shared between
 * tavall-di runtime and Tavall-Architecture-Tests enforcement.
 */
public final class DiArchitectureSemantics {

    private static final Set<String> APPROVED_BOUNDARY_SUFFIXES = Set.of(
            "Bootstrap",
            "CompositionRoot",
            "Factory"
    );

    private static final Set<String> DATA_VALUE_SUFFIXES = Set.of(
            "Data",
            "State",
            "Request",
            "Result",
            "MetaData",
            "Config",
            "Configuration",
            "Event",
            "Model",
            "DTO",
            "Dto"
    );

    private static final Set<String> BEHAVIORAL_ROLE_SUFFIXES = Set.of(
            "Service",
            "Handler",
            "DataHandler",
            "MetaDataHandler",
            "Orchestrator",
            "Router",
            "Resolver",
            "Registry",
            "Cache",
            "Provider",
            "Controller",
            "Gateway",
            "Adapter",
            "Listener",
            "Consumer",
            "Publisher",
            "Coordinator",
            "Scheduler",
            "Manager"
    );

    private DiArchitectureSemantics() {
    }

    /**
     * Determines whether a type is DI-managed.
     *
     * @param type the candidate class
     * @return {@code true} if the type is a concrete class managed by tavall-di
     */
    public static boolean isDiManaged(Class<?> type) {
        if (type == null || type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            return false;
        }
        if (type.isAnnotationPresent(DelegatesTo.class)) {
            return true;
        }
        return DependencyAccess.class.isAssignableFrom(type)
                || IDependencyAccess.class.isAssignableFrom(type);
    }

    /**
     * Determines whether a type or location is an approved composition boundary.
     *
     * @param type the candidate class
     * @return {@code true} if direct construction or map registration is authorized
     */
    public static boolean isApprovedCompositionBoundary(Class<?> type) {
        if (type == null) {
            return false;
        }
        if (type.isAnnotationPresent(CompositionBoundary.class)) {
            return true;
        }
        Package pkg = type.getPackage();
        if (pkg != null && pkg.isAnnotationPresent(CompositionBoundary.class)) {
            return true;
        }
        String className = type.getName();
        if (className.startsWith("org.tavall.dependency.")) {
            return true;
        }
        if (className.contains(".bootstrap.") || className.contains(".composition.")) {
            return true;
        }
        for (String suffix : APPROVED_BOUNDARY_SUFFIXES) {
            if (className.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether a type is an explicit exception to DI participation.
     *
     * @param type the candidate class
     * @return {@code true} if the class is an authorized non-DI type
     */
    public static boolean isExplicitDiException(Class<?> type) {
        if (type == null) {
            return false;
        }
        if (type.isRecord() || type.isEnum() || Throwable.class.isAssignableFrom(type)) {
            return true;
        }
        if (type.isAnnotationPresent(ExplicitNonDi.class)) {
            return true;
        }
        String simpleName = type.getSimpleName();
        if (simpleName.endsWith("Builder")) {
            return true;
        }
        for (String suffix : DATA_VALUE_SUFFIXES) {
            if (simpleName.endsWith(suffix)) {
                return true;
            }
        }
        return isApprovedCompositionBoundary(type);
    }

    /**
     * Determines whether a type represents a Tavall-owned behavioral component.
     *
     * @param type the candidate class
     * @return {@code true} if the class owns application/domain behavior
     */
    public static boolean isBehavioralComponent(Class<?> type) {
        if (type == null || type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            return false;
        }
        if (isExplicitDiException(type)) {
            return false;
        }
        String simpleName = type.getSimpleName();
        for (String suffix : BEHAVIORAL_ROLE_SUFFIXES) {
            if (simpleName.endsWith(suffix)) {
                return true;
            }
        }
        return isDiManaged(type);
    }

    /**
     * Determines whether direct map reference is prohibited for a production type.
     *
     * @param callerType the consuming class
     * @return {@code true} if the class is an ordinary consumer and must not use IDependencyMap / DependencyMap directly
     */
    public static boolean isDirectMapAccessProhibited(Class<?> callerType) {
        if (callerType == null) {
            return true;
        }
        return !isApprovedCompositionBoundary(callerType);
    }
}
