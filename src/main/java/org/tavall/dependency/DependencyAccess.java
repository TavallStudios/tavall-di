/*
 * TJVD License (TJ ValentineÃ¢â‚¬â„¢s Discretionary License) Ã¢â‚¬â€ Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency;

import org.tavall.dependency.annotations.VariableTypeArguments;

/**
 * Generic dependency-access contract used by authored classes that expose variable dependency type arguments.
 */
@VariableTypeArguments
public interface DependencyAccess<
        DependencyOneValue,
        DependencyTwoValue,
        DependencyThreeValue,
        DependencyFourValue> extends IDependencyAccess {
}
