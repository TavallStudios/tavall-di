package org.tavall.dependency.injection.helpers.generationfixtures;

import java.util.ArrayList;
import java.util.List;

public final class GenerationProbeData {
    private final List<GenerationReader> consumers = new ArrayList<>();
    private final List<String> initializedValues = new ArrayList<>();
    private boolean failInitialization;
    private String value = "first";

    public void initialized(GenerationReader consumer, String value) {
        consumers.add(consumer);
        initializedValues.add(value);
        if (failInitialization) throw new IllegalStateException("Fixture initialization failure");
    }

    public List<GenerationReader> consumers() { return consumers; }
    public List<String> initializedValues() { return initializedValues; }
    public void failInitialization(boolean fail) { failInitialization = fail; }
    public String value() { return value; }
    public void value(String value) { this.value = value; }
}
