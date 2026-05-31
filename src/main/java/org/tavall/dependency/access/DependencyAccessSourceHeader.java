/*
 * TJVD License (TJ ValentineÃ¢â‚¬â„¢s Discretionary License) Ã¢â‚¬â€ Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.access;

import java.util.Set;

/**
 * Parsed class header information used by the source lowerer.
 */
final class DependencyAccessSourceHeader {
    private final String headerText;
    private final int classKeywordIndex;
    private final int bodyStartIndex;
    private final int implementsKeywordIndex;
    private final int implementsClauseEndIndex;
    private final String className;
    private final Set<String> typeParameters;
    private final String originalImplementsClause;

    DependencyAccessSourceHeader(
            String headerText,
            int classKeywordIndex,
            int bodyStartIndex,
            int implementsKeywordIndex,
            int implementsClauseEndIndex,
            String className,
            Set<String> typeParameters,
            String originalImplementsClause) {
        this.headerText = headerText;
        this.classKeywordIndex = classKeywordIndex;
        this.bodyStartIndex = bodyStartIndex;
        this.implementsKeywordIndex = implementsKeywordIndex;
        this.implementsClauseEndIndex = implementsClauseEndIndex;
        this.className = className;
        this.typeParameters = typeParameters;
        this.originalImplementsClause = originalImplementsClause;
    }

    String headerText() {
        return headerText;
    }

    int classKeywordIndex() {
        return classKeywordIndex;
    }

    int bodyStartIndex() {
        return bodyStartIndex;
    }

    int implementsKeywordIndex() {
        return implementsKeywordIndex;
    }

    int implementsClauseEndIndex() {
        return implementsClauseEndIndex;
    }

    String className() {
        return className;
    }

    Set<String> typeParameters() {
        return typeParameters;
    }

    String originalImplementsClause() {
        return originalImplementsClause;
    }
}
