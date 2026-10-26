package org.tavall.dependency.injection.helpers.generationfixtures;

import org.tavall.dependency.DependencyAccess;
import org.tavall.dependency.GenerationOwnedDependencyAccess;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.dependency.annotations.PostConstruct;

@DelegatesTo(IGenerationReader.class)
public final class GenerationReader extends GenerationOwnedDependencyAccess
        implements IGenerationReader, DependencyAccess<GenerationProbeData> {
    @Override
    public String read() {
        return getInstance().value();
    }

    @PostConstruct
    private void initialized() {
        GenerationProbeData data = getInstance();
        data.initialized(this, data.value());
    }
}
