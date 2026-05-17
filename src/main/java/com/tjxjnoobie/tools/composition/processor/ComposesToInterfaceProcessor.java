package com.tjxjnoobie.tools.composition.processor;

import com.google.auto.service.AutoService;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@AutoService(Processor.class)
public class ComposesToInterfaceProcessor extends AbstractProcessor {
    public static final String ANNOTATION_FQCN = "com.tjxjnoobie.api.dependency.annotations.ComposesToInterface";

    private Elements elements;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elements = processingEnv.getElementUtils();
        this.messager = processingEnv.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(ANNOTATION_FQCN);
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

        CompositionIndex index = buildIndex(roundEnv);
        if (index.isEmpty()) {
            return false;
        }

        validateCycles(index);

        for (TargetDomain targetDomain : index.targetDomains().values()) {
            try {
                generateTargetBridge(targetDomain);
            } catch (IOException e) {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Failed to generate composed interface for " + targetDomain.targetQualifiedName() + ": " + e.getMessage());
            }
        }

        return false;
    }

    private CompositionIndex buildIndex(RoundEnvironment roundEnv) {
        Map<String, TargetDomain> targetDomains = new LinkedHashMap<>();
        DefaultDirectedGraph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(
                Objects.requireNonNull(elements.getTypeElement(ANNOTATION_FQCN)))) {
            if (annotatedElement.getKind() != ElementKind.INTERFACE) {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "@ComposesToInterface can only be used on interfaces",
                        annotatedElement);
                continue;
            }

            TypeElement sourceInterface = (TypeElement) annotatedElement;
            List<TypeElement> targetInterfaces = getTargetInterfaces(sourceInterface);
            if (targetInterfaces.isEmpty()) {
                messager.printMessage(
                        Diagnostic.Kind.WARNING,
                        "@ComposesToInterface does not declare any target interfaces",
                        sourceInterface);
                continue;
            }

            String sourceQualifiedName = sourceInterface.getQualifiedName().toString();
            graph.addVertex(sourceQualifiedName);

            for (TypeElement targetInterface : targetInterfaces) {
                String targetQualifiedName = targetInterface.getQualifiedName().toString();
                TargetDomain targetDomain = targetDomains.computeIfAbsent(
                        targetQualifiedName,
                        ignored -> new TargetDomain(targetInterface));
                targetDomain.addSourceInterface(sourceInterface);
                graph.addVertex(targetQualifiedName);
                if (!graph.containsEdge(sourceQualifiedName, targetQualifiedName)) {
                    graph.addEdge(sourceQualifiedName, targetQualifiedName);
                }
            }
        }

        return new CompositionIndex(targetDomains, graph);
    }

    private List<TypeElement> getTargetInterfaces(TypeElement sourceInterface) {
        return readAnnotationConfig(sourceInterface).targets();
    }

    private String getMethodPrefix(TypeElement sourceInterface) {
        return readAnnotationConfig(sourceInterface).methodPrefix();
    }

    private AnnotationConfig readAnnotationConfig(TypeElement sourceInterface) {
        List<TypeElement> targets = new ArrayList<>();
        String methodPrefix = "";
        for (AnnotationMirror annotationMirror : sourceInterface.getAnnotationMirrors()) {
            Element annotationElement = annotationMirror.getAnnotationType().asElement();
            if (!(annotationElement instanceof TypeElement annotationType)
                    || !ANNOTATION_FQCN.equals(annotationType.getQualifiedName().toString())) {
                continue;
            }

            Map<? extends ExecutableElement, ? extends AnnotationValue> values =
                    processingEnv.getElementUtils().getElementValuesWithDefaults(annotationMirror);

            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
                if ("value".contentEquals(entry.getKey().getSimpleName())) {
                    Object rawValue = entry.getValue().getValue();
                    if (!(rawValue instanceof Collection<?> collection)) {
                        continue;
                    }

                    for (Object value : collection) {
                        if (!(value instanceof AnnotationValue annotationValue)) {
                            continue;
                        }

                        Object memberValue = annotationValue.getValue();
                        if (!(memberValue instanceof DeclaredType declaredType)
                                || !(declaredType.asElement() instanceof TypeElement targetType)) {
                            continue;
                        }

                        targets.add(targetType);
                    }
                } else if ("methodPrefix".contentEquals(entry.getKey().getSimpleName())) {
                    Object rawValue = entry.getValue().getValue();
                    if (rawValue instanceof String prefix) {
                        methodPrefix = prefix;
                    }
                }
            }
        }
        return new AnnotationConfig(targets, methodPrefix);
    }

    private void validateCycles(CompositionIndex index) {
        for (Set<String> stronglyConnectedSet : new KosarajuStrongConnectivityInspector<>(index.graph()).stronglyConnectedSets()) {
            if (!isCycleComponent(index.graph(), stronglyConnectedSet)) {
                continue;
            }
            List<String> cyclePath = stronglyConnectedSet.stream().sorted().collect(Collectors.toCollection(ArrayList::new));
            cyclePath.add(cyclePath.get(0));
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Detected composed interface cycle: " + String.join(" -> ", cyclePath));
        }
    }

    private void generateTargetBridge(TargetDomain targetDomain) throws IOException {
        String targetQualifiedName = targetDomain.targetQualifiedName();
        String generatedQualifiedName = targetQualifiedName + "Generated";
        JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(generatedQualifiedName, targetDomain.targetInterface());

        Map<String, Integer> resolverNameCounts = new HashMap<>();
        List<SourceInterface> sourceInterfaces = new ArrayList<>();
        for (TypeElement sourceInterfaceElement : targetDomain.sourceInterfaces()) {
            String resolverName = resolverNameFor(sourceInterfaceElement, resolverNameCounts);
            sourceInterfaces.add(new SourceInterface(
                    sourceInterfaceElement,
                    resolverName,
                    normalizeMethodPrefix(getMethodPrefix(sourceInterfaceElement))));
        }

        Map<MethodSignature, List<SourceMethod>> methodsBySignature = new LinkedHashMap<>();
        Map<String, List<SourceMethod>> methodsByName = new LinkedHashMap<>();

        for (SourceInterface sourceInterface : sourceInterfaces) {
            for (ExecutableElement method : composeableMethods(sourceInterface.type())) {
                SourceMethod sourceMethod = new SourceMethod(
                        sourceInterface,
                        method,
                        generatedMethodName(sourceInterface.methodPrefix(), method.getSimpleName().toString()));
                methodsBySignature.computeIfAbsent(MethodSignature.from(sourceMethod), ignored -> new ArrayList<>()).add(sourceMethod);
                methodsByName.computeIfAbsent(sourceMethod.generatedMethodName(), ignored -> new ArrayList<>()).add(sourceMethod);
            }
        }

        for (Map.Entry<MethodSignature, List<SourceMethod>> entry : methodsBySignature.entrySet()) {
            if (entry.getValue().size() <= 1) {
                continue;
            }
            String owners = entry.getValue().stream()
                    .map(sourceMethod -> sourceMethod.sourceInterface().type().getQualifiedName().toString())
                    .collect(Collectors.joining(", "));
            messager.printMessage(
                    Diagnostic.Kind.WARNING,
                    "Skipping generated forwarding method '" + entry.getKey().displayName()
                            + "' for target " + targetQualifiedName + " because it is declared by multiple interfaces: " + owners,
                    targetDomain.targetInterface());
        }

        try (Writer writer = sourceFile.openWriter()) {
            String packageName = packageNameFor(targetDomain.targetInterface());
            if (!packageName.isBlank()) {
                writer.write("package " + packageName + ";\n\n");
            }

            writer.write("// Generated by ComposesToInterfaceProcessor. Do not edit.\n");
            writer.write("public interface " + targetDomain.generatedSimpleName() + " {\n");

            for (SourceInterface sourceInterface : sourceInterfaces) {
                writer.write("    default " + sourceInterface.type().getQualifiedName() + " "
                        + sourceInterface.resolverName() + "() {\n");
                writer.write("        return com.tjxjnoobie.api.dependency.DependencyLoaderAccess.findInstance("
                        + sourceInterface.type().getQualifiedName() + ".class);\n");
                writer.write("    }\n\n");
            }

            for (Map.Entry<String, List<SourceMethod>> entry : methodsByName.entrySet()) {
                List<SourceMethod> sameNamedMethods = entry.getValue();
                if (sameNamedMethods.size() > 1) {
                    writer.write("    // Multiple generated methods share the name '" + entry.getKey() + "': ");
                    writer.write(sameNamedMethods.stream()
                            .map(sourceMethod -> sourceMethod.sourceInterface().type().getQualifiedName().toString()
                                    + "#" + MethodSignature.from(sourceMethod).displayName())
                            .collect(Collectors.joining("; ")));
                    writer.write("\n");
                }
            }
            if (!methodsByName.isEmpty()) {
                writer.write("\n");
            }

            for (Map.Entry<MethodSignature, List<SourceMethod>> entry : methodsBySignature.entrySet()) {
                if (entry.getValue().size() != 1) {
                    continue;
                }
                writeForwardingMethod(writer, entry.getValue().getFirst());
            }

            writer.write("}\n");
        }
    }

    private void writeForwardingMethod(Writer writer, SourceMethod sourceMethod) throws IOException {
        ExecutableElement method = sourceMethod.method();
        String returnType = method.getReturnType().toString();
        String methodName = sourceMethod.generatedMethodName();
        String parameters = parameterDeclaration(method);
        String arguments = argumentList(method);
        String throwsClause = throwsClause(method);

        writer.write("    default " + returnType + " " + methodName + "(" + parameters + ")" + throwsClause + " {\n");
        String targetInvocation = sourceMethod.sourceInterface().resolverName() + "()."
                + sourceMethod.method().getSimpleName() + "(" + arguments + ")";
        if (method.getReturnType().getKind() == TypeKind.VOID) {
            writer.write("        " + targetInvocation + ";\n");
            writer.write("    }\n\n");
            return;
        }

        writer.write("        return " + targetInvocation + ";\n");
        writer.write("    }\n\n");
    }

    private List<ExecutableElement> composeableMethods(TypeElement sourceInterface) {
        List<ExecutableElement> methods = new ArrayList<>();
        Set<MethodSignature> seen = new LinkedHashSet<>();

        for (ExecutableElement method : ElementFilter.methodsIn(elements.getAllMembers(sourceInterface))) {
            if (method.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            if (!method.getModifiers().contains(Modifier.PUBLIC)) {
                continue;
            }
            if (method.getEnclosingElement() instanceof TypeElement owner
                    && owner.getQualifiedName().contentEquals("java.lang.Object")) {
                continue;
            }

            MethodSignature signature = new MethodSignature(
                    method.getSimpleName().toString(),
                    method.getParameters().stream()
                            .map(parameter -> parameter.asType().toString())
                            .toList(),
                    method.getReturnType().toString());
            if (seen.add(signature)) {
                methods.add(method);
            }
        }

        return methods;
    }

    private String packageNameFor(TypeElement typeElement) {
        Element current = typeElement;
        while (!(current instanceof PackageElement) && current != null) {
            current = current.getEnclosingElement();
        }
        return current instanceof PackageElement packageElement ? packageElement.getQualifiedName().toString() : "";
    }

    private String resolverNameFor(TypeElement sourceInterface, Map<String, Integer> resolverNameCounts) {
        String simpleName = sourceInterface.getSimpleName().toString();
        String strippedName = stripInterfacePrefix(simpleName);
        String baseName = "get" + strippedName;
        int count = resolverNameCounts.merge(baseName, 1, Integer::sum);
        return count == 1 ? baseName : baseName + count;
    }

    private String stripInterfacePrefix(String simpleName) {
        if (simpleName.length() > 1 && simpleName.startsWith("I") && Character.isUpperCase(simpleName.charAt(1))) {
            return simpleName.substring(1);
        }
        return simpleName;
    }

    private String lowerCamel(String name) {
        if (name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return name.toLowerCase(Locale.ROOT);
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private String normalizeMethodPrefix(String methodPrefix) {
        return methodPrefix == null ? "" : methodPrefix.trim();
    }

    private String generatedMethodName(String methodPrefix, String declaredMethodName) {
        if (methodPrefix.isBlank()) {
            return declaredMethodName;
        }
        return lowerCamel(methodPrefix) + Character.toUpperCase(declaredMethodName.charAt(0)) + declaredMethodName.substring(1);
    }

    private String parameterDeclaration(ExecutableElement method) {
        List<String> parameters = new ArrayList<>();
        int index = 0;
        for (VariableElement parameter : method.getParameters()) {
            parameters.add(parameter.asType() + " arg" + index++);
        }
        return String.join(", ", parameters);
    }

    private String argumentList(ExecutableElement method) {
        List<String> arguments = new ArrayList<>();
        for (int i = 0; i < method.getParameters().size(); i++) {
            arguments.add("arg" + i);
        }
        return String.join(", ", arguments);
    }

    private String throwsClause(ExecutableElement method) {
        if (method.getThrownTypes().isEmpty()) {
            return "";
        }
        return " throws " + method.getThrownTypes().stream()
                .map(TypeMirror::toString)
                .collect(Collectors.joining(", "));
    }

    private record CompositionIndex(
            Map<String, TargetDomain> targetDomains,
            DefaultDirectedGraph<String, DefaultEdge> graph) {
        boolean isEmpty() {
            return targetDomains.isEmpty();
        }
    }

    private record AnnotationConfig(List<TypeElement> targets, String methodPrefix) {
    }

    private static final class TargetDomain {
        private final TypeElement targetInterface;
        private final Set<TypeElement> sourceInterfaces = new LinkedHashSet<>();

        private TargetDomain(TypeElement targetInterface) {
            this.targetInterface = targetInterface;
        }

        void addSourceInterface(TypeElement sourceInterface) {
            sourceInterfaces.add(sourceInterface);
        }

        TypeElement targetInterface() {
            return targetInterface;
        }

        Set<TypeElement> sourceInterfaces() {
            return sourceInterfaces;
        }

        String targetQualifiedName() {
            return targetInterface.getQualifiedName().toString();
        }

        String generatedSimpleName() {
            return targetInterface.getSimpleName() + "Generated";
        }
    }

    private record SourceInterface(TypeElement type, String resolverName, String methodPrefix) {
    }

    private record SourceMethod(SourceInterface sourceInterface, ExecutableElement method, String generatedMethodName) {
    }

    private record MethodSignature(String name, List<String> parameters, String returnType) {
        static MethodSignature from(SourceMethod sourceMethod) {
            return new MethodSignature(
                    sourceMethod.generatedMethodName(),
                    sourceMethod.method().getParameters().stream()
                            .map(parameter -> parameter.asType().toString())
                            .toList(),
                    sourceMethod.method().getReturnType().toString());
        }

        String displayName() {
            return name + "(" + String.join(", ", parameters) + ")";
        }
    }

    private boolean isCycleComponent(DefaultDirectedGraph<String, DefaultEdge> graph, Set<String> stronglyConnectedSet) {
        if (stronglyConnectedSet.size() > 1) {
            return true;
        }
        if (stronglyConnectedSet.isEmpty()) {
            return false;
        }
        String onlyVertex = stronglyConnectedSet.iterator().next();
        return graph.containsEdge(onlyVertex, onlyVertex);
    }
}
