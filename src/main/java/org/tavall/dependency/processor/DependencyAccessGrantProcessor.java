/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.processor;

import org.tavall.dependency.access.DependencyAccessMetadataEmitter;
import org.tavall.dependency.access.DependencyAccessSourceGrantDescriptor;
import org.tavall.dependency.annotations.GrantDependencyAccess;
import org.tavall.dependency.annotations.GrantedDependencyAccess;
import org.tavall.dependency.annotations.GrantedDependencyAccesses;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Expands granted dependency-access metadata into companion source classes.
 */
public final class DependencyAccessGrantProcessor extends AbstractProcessor {
    private final DependencyAccessMetadataEmitter metadataEmitter = new DependencyAccessMetadataEmitter();
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(GrantDependencyAccess.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(GrantDependencyAccess.class)) {
            if (annotatedElement.getKind() != ElementKind.CLASS) {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "@GrantDependencyAccess may only be used on classes",
                        annotatedElement);
                continue;
            }

            TypeElement sourceElement = (TypeElement) annotatedElement;
            List<DependencyAccessSourceGrantDescriptor> descriptors = extractGrantDescriptors(sourceElement);

            if (descriptors.isEmpty()) {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "No @GrantedDependencyAccess metadata was found for " + sourceElement.getQualifiedName(),
                        sourceElement);
                continue;
            }

            List<DependencyAccessSourceGrantDescriptor> validatedDescriptors = new ArrayList<>(descriptors.size());
            for (DependencyAccessSourceGrantDescriptor descriptor : descriptors) {
                if (descriptor == null) {
                    continue;
                }
                validatedDescriptors.add(descriptor);
            }

            metadataEmitter.emitMetadata(filer, messager, sourceElement, validatedDescriptors, true);
        }

        return false;
    }

    private List<DependencyAccessSourceGrantDescriptor> extractGrantDescriptors(TypeElement sourceElement) {
        List<DependencyAccessSourceGrantDescriptor> descriptors = new ArrayList<>();
        for (AnnotationMirror annotationMirror : sourceElement.getAnnotationMirrors()) {
            String annotationTypeName = annotationMirror.getAnnotationType().toString();
            if (GrantedDependencyAccess.class.getCanonicalName().equals(annotationTypeName)) {
                DependencyAccessSourceGrantDescriptor descriptor = parseGrantDescriptor(annotationMirror);
                if (descriptor != null) {
                    descriptors.add(descriptor);
                }
                continue;
            }

            if (!GrantedDependencyAccesses.class.getCanonicalName().equals(annotationTypeName)) {
                continue;
            }

            for (var entry : annotationMirror.getElementValues().entrySet()) {
                ExecutableElement annotationMember = entry.getKey();
                String memberName = annotationMember.getSimpleName().toString();
                if (!"value".equals(memberName)) {
                    continue;
                }
                Object value = entry.getValue().getValue();
                if (!(value instanceof List<?> values)) {
                    continue;
                }

                for (Object rawValue : values) {
                    if (rawValue instanceof javax.lang.model.element.AnnotationValue nestedAnnotationValue) {
                        Object nestedValue = nestedAnnotationValue.getValue();
                        if (nestedValue instanceof AnnotationMirror nestedAnnotationMirror) {
                            DependencyAccessSourceGrantDescriptor descriptor = parseGrantDescriptor(nestedAnnotationMirror);
                            if (descriptor != null) {
                                descriptors.add(descriptor);
                            }
                        }
                    }
                }
            }
        }
        return descriptors;
    }

    private DependencyAccessSourceGrantDescriptor parseGrantDescriptor(AnnotationMirror annotationMirror) {
        String accessType = null;
        List<String> dependencyTypes = new ArrayList<>();
        for (var entry : annotationMirror.getElementValues().entrySet()) {
            ExecutableElement annotationMember = entry.getKey();
            String memberName = annotationMember.getSimpleName().toString();
            if ("accessType".equals(memberName)) {
                accessType = toTypeName(entry.getValue().getValue());
                continue;
            }
            if ("dependencyTypes".equals(memberName)) {
                dependencyTypes.addAll(toTypeNames(entry.getValue().getValue()));
            }
        }

        if (accessType == null || dependencyTypes.isEmpty()) {
            return null;
        }
        return new DependencyAccessSourceGrantDescriptor(accessType, dependencyTypes);
    }

    private List<String> toTypeNames(Object annotationValue) {
        List<String> typeNames = new ArrayList<>();
        if (!(annotationValue instanceof List<?> values)) {
            return typeNames;
        }

        for (Object rawValue : values) {
            if (rawValue instanceof javax.lang.model.element.AnnotationValue annotationValueEntry) {
                Object value = annotationValueEntry.getValue();
                String typeName = toTypeName(value);
                if (typeName != null && !typeName.isBlank()) {
                    typeNames.add(typeName);
                }
            }
        }
        return typeNames;
    }

    private String toTypeName(Object value) {
        if (value instanceof DeclaredType declaredType) {
            TypeMirror typeMirror = declaredType;
            return typeMirror.toString();
        }
        if (value instanceof TypeMirror typeMirror) {
            return typeMirror.toString();
        }
        return value == null ? null : value.toString();
    }
}
