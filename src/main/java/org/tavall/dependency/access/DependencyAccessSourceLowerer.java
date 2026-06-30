/*
 * TJVD License (TJ ValentineÃ¢â‚¬â„¢s Discretionary License) Ã¢â‚¬â€ Version 1.0 (2025)
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

/**
 * Lowers dependency-access authoring source into the generated metadata shape.
 */
public final class DependencyAccessSourceLowerer {
    private static final String GRANT_ANNOTATION = "@GrantDependencyAccess";
    private static final String VARIABLE_TYPE_ARGUMENTS_ANNOTATION = "@VariableTypeArguments";
    private static final String DEP_ACCESS_NAME = "DependencyAccess";

    /**
     * Lowers a source set by expanding granted dependency metadata and stripping implemented type arguments.
     *
     * @param sourcesByQualifiedName source content keyed by the source type name
     * @return the lowered source content keyed by the same source type name
     */
    public Map<String, String> lowerSources(Map<String, String> sourcesByQualifiedName) {
        Map<String, String> loweredSources = new LinkedHashMap<>();
        if (sourcesByQualifiedName == null || sourcesByQualifiedName.isEmpty()) {
            return loweredSources;
        }

        Set<String> variableTypeArgumentsInterfaces = collectVariableTypeArgumentsInterfaces(sourcesByQualifiedName);
        for (Map.Entry<String, String> entry : sourcesByQualifiedName.entrySet()) {
            String sourceName = entry.getKey();
            String source = entry.getValue();
            loweredSources.put(sourceName, lowerSource(source, variableTypeArgumentsInterfaces));
        }

        return loweredSources;
    }

    /**
     * Lowers a single source file using the supplied variable access interface names.
     *
     * @param source the authoring source text
     * @param variableTypeArgumentsInterfaces the known variable access interface names
     * @return the lowered source text
     */
    public String lowerSource(String source, Set<String> variableTypeArgumentsInterfaces) {
        if (source == null || source.isBlank()) {
            return source;
        }

        String normalizedSource = normalizeLineEndings(source);
        int grantIndex = normalizedSource.indexOf(GRANT_ANNOTATION);
        if (grantIndex < 0) {
            return source;
        }

        int classKeywordIndex = findClassKeywordIndex(normalizedSource, grantIndex);
        if (classKeywordIndex < 0) {
            throw new IllegalArgumentException("Unable to find class declaration for @GrantDependencyAccess source");
        }

        int bodyStartIndex = findBodyStartIndex(normalizedSource, classKeywordIndex);
        if (bodyStartIndex < 0) {
            throw new IllegalArgumentException("Unable to find class body for @GrantDependencyAccess source");
        }

        DependencyAccessSourceHeader classHeader = parseClassHeader(normalizedSource, classKeywordIndex, bodyStartIndex);
        List<String> rewrittenInterfaceSegments = new ArrayList<>();
        List<DependencyAccessGrantCandidate> grants = parseAccessGrants(
                classHeader,
                variableTypeArgumentsInterfaces,
                rewrittenInterfaceSegments);

        String generatedGrantAnnotations = renderGrantAnnotations(grants);
        String rewrittenHeader = renderLoweredHeader(classHeader, rewrittenInterfaceSegments);
        String prefix = normalizedSource.substring(0, grantIndex);
        String originalAnnotationAndModifierPrefix = normalizedSource.substring(grantIndex, classHeader.classKeywordIndex());
        String suffix = normalizedSource.substring(classHeader.bodyStartIndex());

        return prefix + generatedGrantAnnotations + originalAnnotationAndModifierPrefix + rewrittenHeader + suffix;
    }

    private Set<String> collectVariableTypeArgumentsInterfaces(Map<String, String> sourcesByQualifiedName) {
        Set<String> variableTypeArgumentsInterfaces = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : sourcesByQualifiedName.entrySet()) {
            String sourceName = entry.getKey();
            String source = normalizeLineEndings(entry.getValue());
            if (!source.contains(VARIABLE_TYPE_ARGUMENTS_ANNOTATION)) {
                continue;
            }

            String interfaceName = parsePrimaryInterfaceName(source);
            if (interfaceName == null || interfaceName.isBlank()) {
                continue;
            }

            variableTypeArgumentsInterfaces.add(interfaceName);
            String simpleSourceName = extractSimpleName(sourceName);
            if (!simpleSourceName.isBlank()) {
                variableTypeArgumentsInterfaces.add(simpleSourceName);
            }
        }
        variableTypeArgumentsInterfaces.add(DEP_ACCESS_NAME);
        return variableTypeArgumentsInterfaces;
    }

    private String renderGrantAnnotations(List<DependencyAccessGrantCandidate> grants) {
        StringBuilder builder = new StringBuilder();
        for (DependencyAccessGrantCandidate grant : grants) {
            builder.append("@org.tavall.dependency.annotations.GrantedDependencyAccess(\n");
            builder.append("        accessType = ").append(grant.accessType()).append(".class,\n");
            builder.append("        dependencyTypes = {");

            List<String> dependencyTypes = grant.dependencyTypes();
            if (!dependencyTypes.isEmpty()) {
                builder.append('\n');
                for (int index = 0; index < dependencyTypes.size(); index++) {
                    String dependencyType = dependencyTypes.get(index);
                    builder.append("                ").append(dependencyType).append(".class");
                    if (index + 1 < dependencyTypes.size()) {
                        builder.append(',');
                    }
                    builder.append('\n');
                }
                builder.append("        ");
            }

            builder.append("}\n");
            builder.append(")\n");
        }
        return builder.toString();
    }

    private String renderLoweredHeader(
            DependencyAccessSourceHeader classHeader,
            List<String> rewrittenInterfaceSegments) {
        String header = classHeader.headerText();
        String rewrittenImplementsClause = renderLoweredImplementsClause(rewrittenInterfaceSegments);
        if (classHeader.originalImplementsClause() == null) {
            return header;
        }

        return header.substring(0, classHeader.implementsKeywordIndex())
                + rewrittenImplementsClause
                + " "
                + header.substring(classHeader.implementsClauseEndIndex());
    }

    private String renderLoweredImplementsClause(List<String> rewrittenInterfaceSegments) {
        if (rewrittenInterfaceSegments == null || rewrittenInterfaceSegments.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder("implements ");
        for (int index = 0; index < rewrittenInterfaceSegments.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(rewrittenInterfaceSegments.get(index));
        }
        return builder.toString();
    }

    private List<DependencyAccessGrantCandidate> parseAccessGrants(
            DependencyAccessSourceHeader classHeader,
            Set<String> variableTypeArgumentsInterfaces,
            List<String> rewrittenInterfaceSegments) {
        List<DependencyAccessGrantCandidate> grants = new ArrayList<>();
        String implementsClause = classHeader.originalImplementsClause();
        if (implementsClause == null || implementsClause.isBlank()) {
            throw new IllegalArgumentException(
                    "No implements clause found on @GrantDependencyAccess class " + classHeader.className());
        }

        List<String> interfaceSegments = splitTopLevel(implementsClause);
        for (String interfaceSegment : interfaceSegments) {
            String trimmedSegment = interfaceSegment.trim();
            if (trimmedSegment.isBlank()) {
                continue;
            }

            int genericStartIndex = trimmedSegment.indexOf('<');
            if (genericStartIndex < 0) {
                rewrittenInterfaceSegments.add(trimmedSegment);
                continue;
            }

            int genericEndIndex = trimmedSegment.lastIndexOf('>');
            if (genericEndIndex < genericStartIndex) {
                throw new IllegalArgumentException("Malformed dependency-access declaration: " + trimmedSegment);
            }

            String accessType = trimmedSegment.substring(0, genericStartIndex).trim();
            if (accessType.isBlank()) {
                throw new IllegalArgumentException("Malformed dependency-access declaration: " + trimmedSegment);
            }
            if (!variableTypeArgumentsInterfaces.contains(extractSimpleName(accessType))
                    && !variableTypeArgumentsInterfaces.contains(accessType)) {
                throw new IllegalArgumentException(
                        "access interface must have or inherit @VariableTypeArguments: " + accessType);
            }

            String typeArgumentsText = trimmedSegment.substring(genericStartIndex + 1, genericEndIndex).trim();
            List<String> dependencyTypes = parseDependencyTypes(typeArgumentsText, classHeader.typeParameters(), accessType);
            grants.add(new DependencyAccessGrantCandidate(accessType, dependencyTypes));
            rewrittenInterfaceSegments.add(accessType);
        }

        if (grants.isEmpty()) {
            throw new IllegalArgumentException(
                    "No dependency-access interfaces were found on @GrantDependencyAccess class "
                            + classHeader.className());
        }

        return grants;
    }

    private List<String> parseDependencyTypes(
            String typeArgumentsText,
            Set<String> classTypeParameters,
            String accessType) {
        List<String> dependencyTypes = new ArrayList<>();
        Set<String> seenTypes = new LinkedHashSet<>();
        List<String> typeArguments = splitTopLevel(typeArgumentsText);

        for (String typeArgument : typeArguments) {
            String trimmedTypeArgument = typeArgument.trim();
            if (trimmedTypeArgument.isBlank()) {
                continue;
            }
            if (trimmedTypeArgument.contains("?")) {
                throw new IllegalArgumentException("wildcard dependency argument is not allowed for " + accessType
                        + ": " + trimmedTypeArgument);
            }

            String candidateType = extractRawTypeToken(trimmedTypeArgument);
            String simpleCandidate = extractSimpleName(candidateType);
            if (classTypeParameters.contains(simpleCandidate) || classTypeParameters.contains(candidateType)) {
                throw new IllegalArgumentException("type variable dependency argument is not allowed for " + accessType
                        + ": " + trimmedTypeArgument);
            }
            if (!seenTypes.add(trimmedTypeArgument)) {
                throw new IllegalArgumentException("duplicate dependency type within access interface " + accessType
                        + ": " + trimmedTypeArgument);
            }

            dependencyTypes.add(trimmedTypeArgument);
        }

        return dependencyTypes;
    }

    private DependencyAccessSourceHeader parseClassHeader(String source, int classKeywordIndex, int bodyStartIndex) {
        int headerStartIndex = classKeywordIndex;
        int headerEndIndex = bodyStartIndex;
        String header = source.substring(headerStartIndex, headerEndIndex);

        int classNameIndex = header.indexOf("class ");
        if (classNameIndex < 0) {
            throw new IllegalArgumentException("Unable to locate class declaration");
        }

        int implementsKeywordIndex = header.indexOf("implements");
        String implementsClause = null;
        int implementsClauseEndIndex = header.length();
        if (implementsKeywordIndex >= 0) {
            implementsClause = header.substring(implementsKeywordIndex + "implements".length()).trim();
            implementsClauseEndIndex = header.length();
        }

        Set<String> classTypeParameters = parseClassTypeParameters(header);
        String className = parseClassName(header);

        return new DependencyAccessSourceHeader(
                header,
                classKeywordIndex,
                bodyStartIndex,
                implementsKeywordIndex,
                implementsClauseEndIndex,
                className,
                classTypeParameters,
                implementsClause);
    }

    private Set<String> parseClassTypeParameters(String header) {
        Set<String> typeParameters = new LinkedHashSet<>();
        int classNameIndex = header.indexOf("class ");
        if (classNameIndex < 0) {
            return typeParameters;
        }

        String classDeclarationRemainder = header.substring(classNameIndex + "class ".length()).trim();
        if (classDeclarationRemainder.isBlank()) {
            return typeParameters;
        }

        int classIdentifierEndIndex = 0;
        while (classIdentifierEndIndex < classDeclarationRemainder.length()) {
            char current = classDeclarationRemainder.charAt(classIdentifierEndIndex);
            if (!Character.isJavaIdentifierPart(current)) {
                break;
            }
            classIdentifierEndIndex++;
        }

        if (classIdentifierEndIndex >= classDeclarationRemainder.length()
                || classDeclarationRemainder.charAt(classIdentifierEndIndex) != '<') {
            return typeParameters;
        }

        int startIndex = classIdentifierEndIndex;
        if (startIndex < 0) {
            return typeParameters;
        }

        int endIndex = findMatchingAngleBracket(classDeclarationRemainder, startIndex);
        if (endIndex < 0) {
            return typeParameters;
        }

        String typeParameterBlock = classDeclarationRemainder.substring(startIndex + 1, endIndex).trim();
        List<String> typeParameterSegments = splitTopLevel(typeParameterBlock);
        for (String segment : typeParameterSegments) {
            String trimmedSegment = segment.trim();
            if (trimmedSegment.isBlank()) {
                continue;
            }
            String candidate = trimmedSegment;
            int spaceIndex = trimmedSegment.indexOf(' ');
            if (spaceIndex >= 0) {
                candidate = trimmedSegment.substring(0, spaceIndex).trim();
            }
            int extendsIndex = candidate.indexOf("extends");
            if (extendsIndex >= 0) {
                candidate = candidate.substring(0, extendsIndex).trim();
            }
            if (!candidate.isBlank()) {
                typeParameters.add(candidate);
            }
        }

        return typeParameters;
    }

    private String parseClassName(String header) {
        int classKeywordIndex = header.indexOf("class ");
        if (classKeywordIndex < 0) {
            return "";
        }

        String remainder = header.substring(classKeywordIndex + "class ".length()).trim();
        if (remainder.isBlank()) {
            return "";
        }

        int endIndex = 0;
        while (endIndex < remainder.length()) {
            char current = remainder.charAt(endIndex);
            if (Character.isJavaIdentifierPart(current)) {
                endIndex++;
                continue;
            }
            break;
        }

        return remainder.substring(0, endIndex);
    }

    private String parsePrimaryInterfaceName(String source) {
        int interfaceIndex = source.indexOf("interface ");
        if (interfaceIndex < 0) {
            return null;
        }

        String remainder = source.substring(interfaceIndex + "interface ".length()).trim();
        if (remainder.isBlank()) {
            return null;
        }

        int endIndex = 0;
        while (endIndex < remainder.length()) {
            char current = remainder.charAt(endIndex);
            if (Character.isJavaIdentifierPart(current)) {
                endIndex++;
                continue;
            }
            break;
        }

        return remainder.substring(0, endIndex);
    }

    private int findClassKeywordIndex(String source, int startIndex) {
        int grantEndIndex = source.indexOf('\n', startIndex);
        int searchIndex = grantEndIndex < 0 ? startIndex : grantEndIndex;
        return source.indexOf("class ", searchIndex);
    }

    private int findBodyStartIndex(String source, int classKeywordIndex) {
        int braceIndex = source.indexOf('{', classKeywordIndex);
        return braceIndex;
    }

    private int findMatchingAngleBracket(String text, int startIndex) {
        int depth = 0;
        for (int index = startIndex; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '<') {
                depth++;
            } else if (current == '>') {
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
        for (int index = 0; index < text.length(); index++) {
            char currentChar = text.charAt(index);
            if (currentChar == '<') {
                genericDepth++;
            } else if (currentChar == '>') {
                genericDepth--;
            } else if (currentChar == ',' && genericDepth == 0) {
                segments.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(currentChar);
        }

        if (current.length() > 0) {
            segments.add(current.toString());
        }
        return segments;
    }

    private String extractRawTypeToken(String typeToken) {
        String trimmedToken = typeToken.trim();
        int genericStartIndex = trimmedToken.indexOf('<');
        if (genericStartIndex >= 0) {
            return trimmedToken.substring(0, genericStartIndex).trim();
        }
        int spaceIndex = trimmedToken.indexOf(' ');
        if (spaceIndex >= 0) {
            return trimmedToken.substring(0, spaceIndex).trim();
        }
        return trimmedToken;
    }

    private String extractSimpleName(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return "";
        }

        int lastDotIndex = typeName.lastIndexOf('.');
        if (lastDotIndex < 0) {
            return typeName;
        }
        return typeName.substring(lastDotIndex + 1);
    }

    private String normalizeLineEndings(String source) {
        return source.replace("\r\n", "\n");
    }

}
