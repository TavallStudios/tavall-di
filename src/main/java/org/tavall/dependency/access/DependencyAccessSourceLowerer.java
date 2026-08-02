/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.access;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lowers expandable {@code DependencyAccess<...>} declarations into valid Java source.
 *
 * <p>One dependency parameter is preserved as a direct access token. Two or more parameters
 * produce a generated map-backed access class and rewrite the consumer to use that class.</p>
 */
public final class DependencyAccessSourceLowerer {
    private static final String DEPENDENCY_ACCESS_NAME = "DependencyAccess";
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^\\s*import\\s+[^;]+;");
    private static final Pattern CLASS_PATTERN = Pattern.compile("\\bclass\\s+([A-Za-z_$][A-Za-z0-9_$]*)");

    /**
     * Lowers all source files and adds generated access sources to the returned map.
     *
     * @param sourcesByQualifiedName source text keyed by qualified type name
     * @return lowered owner sources plus generated access sources
     */
    public Map<String, String> lowerSources(Map<String, String> sourcesByQualifiedName) {
        Map<String, String> loweredSources = new LinkedHashMap<>();
        if (sourcesByQualifiedName == null || sourcesByQualifiedName.isEmpty()) {
            return loweredSources;
        }

        for (Map.Entry<String, String> entry : sourcesByQualifiedName.entrySet()) {
            LoweredSource loweredSource = lowerSource(entry.getKey(), entry.getValue());
            loweredSources.put(entry.getKey(), loweredSource.source());
            for (GeneratedSource generatedSource : loweredSource.generatedSources()) {
                if (sourcesByQualifiedName.containsKey(generatedSource.qualifiedName())
                        || loweredSources.containsKey(generatedSource.qualifiedName())) {
                    throw new IllegalArgumentException(
                            "Generated dependency access type already exists: "
                                    + generatedSource.qualifiedName());
                }
                loweredSources.put(generatedSource.qualifiedName(), generatedSource.source());
            }
        }
        return loweredSources;
    }

    /**
     * Compatibility entry point for callers lowering one source at a time.
     *
     * @param source source text
     * @param ignoredVariableAccessInterfaces retained compatibility parameter
     * @return lowered source text
     */
    public String lowerSource(String source, Set<String> ignoredVariableAccessInterfaces) {
        return lowerSource("", source).source();
    }

    private LoweredSource lowerSource(String qualifiedName, String source) {
        if (source == null || source.isBlank() || !source.contains(DEPENDENCY_ACCESS_NAME)) {
            return new LoweredSource(source, List.of());
        }

        String normalizedSource = normalizeLineEndings(source);
        Matcher classMatcher = CLASS_PATTERN.matcher(normalizedSource);
        if (!classMatcher.find()) {
            return new LoweredSource(source, List.of());
        }

        int classKeywordIndex = classMatcher.start();
        int bodyStartIndex = normalizedSource.indexOf('{', classMatcher.end());
        if (bodyStartIndex < 0) {
            throw new IllegalArgumentException("Unable to find class body for " + qualifiedName);
        }

        String className = classMatcher.group(1);
        String header = normalizedSource.substring(classKeywordIndex, bodyStartIndex);
        int relativeImplementsIndex = findWord(header, "implements");
        if (relativeImplementsIndex < 0) {
            return new LoweredSource(source, List.of());
        }

        int implementsIndex = classKeywordIndex + relativeImplementsIndex;
        int implementsClauseStart = implementsIndex + "implements".length();
        String implementsClause = normalizedSource.substring(implementsClauseStart, bodyStartIndex).trim();
        List<String> implementedTypes = splitTopLevel(implementsClause);
        Set<String> classTypeParameters = parseClassTypeParameters(header, className);
        List<String> rewrittenTypes = new ArrayList<>(implementedTypes.size());
        List<GeneratedSource> generatedSources = new ArrayList<>();
        boolean foundDependencyAccess = false;

        for (String implementedType : implementedTypes) {
            String segment = implementedType.trim();
            if (!isDependencyAccessSegment(segment)) {
                rewrittenTypes.add(segment);
                continue;
            }
            if (foundDependencyAccess) {
                throw new IllegalArgumentException(
                        "A class may declare only one DependencyAccess surface: " + className);
            }
            foundDependencyAccess = true;

            if (!hasDelegatesToAnnotation(normalizedSource, classKeywordIndex)) {
                throw new IllegalArgumentException(
                        "DependencyAccess requires @DelegatesTo on " + className);
            }

            ParsedAccess parsedAccess = parseDependencyAccess(segment, classTypeParameters);
            if (parsedAccess.dependencyTypes().size() == 1) {
                rewrittenTypes.add(segment);
                continue;
            }

            String generatedSimpleName = className + "DependencyAccess";
            rewrittenTypes.add(parsedAccess.rawAccessType() + "<" + generatedSimpleName + ">");
            generatedSources.add(generateAccessSource(
                    normalizedSource,
                    generatedSimpleName,
                    parsedAccess.dependencyTypes()));
        }

        if (!foundDependencyAccess) {
            return new LoweredSource(source, List.of());
        }

        String rewrittenImplementsClause = "implements " + String.join(", ", rewrittenTypes) + " ";
        String rewrittenSource = normalizedSource.substring(0, implementsIndex)
                + rewrittenImplementsClause
                + normalizedSource.substring(bodyStartIndex);
        return new LoweredSource(rewrittenSource, List.copyOf(generatedSources));
    }

    private boolean hasDelegatesToAnnotation(String source, int classKeywordIndex) {
        String declarationPrefix = source.substring(0, classKeywordIndex);
        int previousTypeEnd = Math.max(
                declarationPrefix.lastIndexOf('}'),
                declarationPrefix.lastIndexOf(';'));
        String localPrefix = declarationPrefix.substring(Math.max(0, previousTypeEnd + 1));
        return localPrefix.contains("@DelegatesTo")
                || localPrefix.contains("@org.tavall.dependency.annotations.DelegatesTo");
    }

    private boolean isDependencyAccessSegment(String segment) {
        int genericStart = segment.indexOf('<');
        String rawType = genericStart < 0 ? segment : segment.substring(0, genericStart).trim();
        return DEPENDENCY_ACCESS_NAME.equals(simpleName(rawType));
    }

    private ParsedAccess parseDependencyAccess(
            String segment,
            Set<String> classTypeParameters) {
        int genericStart = segment.indexOf('<');
        if (genericStart < 0) {
            throw new IllegalArgumentException("Raw DependencyAccess is not allowed: " + segment);
        }
        int genericEnd = findMatchingAngleBracket(segment, genericStart);
        if (genericEnd < 0 || !segment.substring(genericEnd + 1).isBlank()) {
            throw new IllegalArgumentException("Malformed DependencyAccess declaration: " + segment);
        }

        String rawAccessType = segment.substring(0, genericStart).trim();
        List<String> dependencyTypes = splitTopLevel(
                segment.substring(genericStart + 1, genericEnd));
        if (dependencyTypes.isEmpty()) {
            throw new IllegalArgumentException("DependencyAccess requires at least one dependency type");
        }

        Set<String> seenTypes = new LinkedHashSet<>();
        for (String rawDependencyType : dependencyTypes) {
            String dependencyType = rawDependencyType.trim();
            if (dependencyType.isBlank()) {
                throw new IllegalArgumentException("Blank dependency type in " + segment);
            }
            if (dependencyType.contains("?")) {
                throw new IllegalArgumentException(
                        "wildcard dependency argument is not allowed: " + dependencyType);
            }
            if (dependencyType.contains("<") || dependencyType.contains(">")) {
                throw new IllegalArgumentException(
                        "parameterized dependency argument is not supported as a class token: "
                                + dependencyType);
            }
            if (classTypeParameters.contains(dependencyType)
                    || classTypeParameters.contains(simpleName(dependencyType))) {
                throw new IllegalArgumentException(
                        "type variable dependency argument is not allowed: " + dependencyType);
            }
            if (!seenTypes.add(dependencyType)) {
                throw new IllegalArgumentException(
                        "duplicate dependency type within DependencyAccess: " + dependencyType);
            }
        }

        return new ParsedAccess(rawAccessType, List.copyOf(seenTypes));
    }

    private GeneratedSource generateAccessSource(
            String ownerSource,
            String generatedSimpleName,
            List<String> dependencyTypes) {
        String packageName = parsePackageName(ownerSource);
        String qualifiedName = packageName.isBlank()
                ? generatedSimpleName
                : packageName + "." + generatedSimpleName;
        StringBuilder source = new StringBuilder();
        if (!packageName.isBlank()) {
            source.append("package ").append(packageName).append(";\n\n");
        }

        Matcher importMatcher = IMPORT_PATTERN.matcher(ownerSource);
        Set<String> imports = new LinkedHashSet<>();
        while (importMatcher.find()) {
            imports.add(importMatcher.group().trim());
        }
        for (String importStatement : imports) {
            source.append(importStatement).append('\n');
        }
        if (!imports.isEmpty()) {
            source.append('\n');
        }

        source.append("/* Generated by DependencyAccessSourceLowerer. */\n");
        source.append("@org.tavall.dependency.annotations.DelegatesTo\n");
        source.append("public final class ").append(generatedSimpleName)
                .append(" implements org.tavall.dependency.IDependencyAccess {\n")
                .append("    private final org.tavall.dependency.maps.interfaces.IDependencyMap dependencyMap;\n\n")
                .append("    public ").append(generatedSimpleName).append("() {\n")
                .append("        this(org.tavall.dependency.maps.DependencyMap.getDependencyMap());\n")
                .append("    }\n\n")
                .append("    public ").append(generatedSimpleName)
                .append("(org.tavall.dependency.maps.interfaces.IDependencyMap dependencyMap) {\n")
                .append("        this.dependencyMap = java.util.Objects.requireNonNull(dependencyMap, \"dependencyMap\");\n")
                .append("    }\n\n")
                .append("    @Override\n")
                .append("    public org.tavall.dependency.maps.interfaces.IDependencyMap getDependencyMap() {\n")
                .append("        return dependencyMap;\n")
                .append("    }\n\n");

        Set<String> accessorNames = new LinkedHashSet<>();
        for (String dependencyType : dependencyTypes) {
            String accessorName = accessorName(dependencyType);
            if (!accessorNames.add(accessorName)) {
                throw new IllegalArgumentException(
                        "generated dependency accessor name collision: " + accessorName);
            }
            source.append("    public ").append(dependencyType).append(' ')
                    .append(accessorName).append("() {\n")
                    .append("        return getDependencyMap().getInstance(")
                    .append(dependencyType).append(".class);\n")
                    .append("    }\n\n");
        }
        source.append("}\n");
        return new GeneratedSource(qualifiedName, source.toString());
    }

    private String accessorName(String dependencyType) {
        String name = simpleName(dependencyType.replace("[]", "Array"));
        if (name.length() > 1 && name.charAt(0) == 'I' && Character.isUpperCase(name.charAt(1))) {
            name = name.substring(1);
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Unable to generate accessor for " + dependencyType);
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private String parsePackageName(String source) {
        Matcher matcher = PACKAGE_PATTERN.matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    private Set<String> parseClassTypeParameters(String header, String className) {
        Set<String> typeParameters = new LinkedHashSet<>();
        int classNameIndex = header.indexOf(className);
        if (classNameIndex < 0) {
            return typeParameters;
        }

        int cursor = classNameIndex + className.length();
        while (cursor < header.length() && Character.isWhitespace(header.charAt(cursor))) {
            cursor++;
        }
        if (cursor >= header.length() || header.charAt(cursor) != '<') {
            return typeParameters;
        }

        int genericStart = cursor;
        int genericEnd = findMatchingAngleBracket(header, genericStart);
        if (genericEnd < 0) {
            return typeParameters;
        }

        for (String typeParameter : splitTopLevel(header.substring(genericStart + 1, genericEnd))) {
            String normalized = typeParameter.trim();
            int spaceIndex = normalized.indexOf(' ');
            if (spaceIndex > 0) {
                normalized = normalized.substring(0, spaceIndex);
            }
            if (!normalized.isBlank()) {
                typeParameters.add(normalized);
            }
        }
        return typeParameters;
    }

    private int findWord(String text, String word) {
        Matcher matcher = Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(text);
        return matcher.find() ? matcher.start() : -1;
    }

    private int findMatchingAngleBracket(String text, int startIndex) {
        int depth = 0;
        for (int index = startIndex; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '<') {
                depth++;
            } else if (character == '>') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private List<String> splitTopLevel(String text) {
        List<String> segments = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return segments;
        }

        StringBuilder current = new StringBuilder();
        int genericDepth = 0;
        int parenthesisDepth = 0;
        int bracketDepth = 0;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            switch (character) {
                case '<' -> genericDepth++;
                case '>' -> genericDepth--;
                case '(' -> parenthesisDepth++;
                case ')' -> parenthesisDepth--;
                case '[' -> bracketDepth++;
                case ']' -> bracketDepth--;
                case ',' -> {
                    if (genericDepth == 0 && parenthesisDepth == 0 && bracketDepth == 0) {
                        segments.add(current.toString().trim());
                        current.setLength(0);
                        continue;
                    }
                }
                default -> {
                }
            }
            current.append(character);
        }
        if (!current.isEmpty()) {
            segments.add(current.toString().trim());
        }
        return segments;
    }

    private String simpleName(String typeName) {
        String normalized = typeName.trim();
        int packageSeparator = Math.max(normalized.lastIndexOf('.'), normalized.lastIndexOf('$'));
        return packageSeparator < 0 ? normalized : normalized.substring(packageSeparator + 1);
    }

    private String normalizeLineEndings(String source) {
        return source.replace("\r\n", "\n").replace('\r', '\n');
    }

    private record ParsedAccess(String rawAccessType, List<String> dependencyTypes) {
    }

    private record GeneratedSource(String qualifiedName, String source) {
    }

    private record LoweredSource(String source, List<GeneratedSource> generatedSources) {
    }
}
