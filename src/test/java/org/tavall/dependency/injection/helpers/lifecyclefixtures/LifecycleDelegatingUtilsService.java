package org.tavall.dependency.injection.helpers.lifecyclefixtures;

import org.tavall.dependency.IDependencyInjectableConcrete;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.dependency.fixtures.contracts.interfaces.IUtils;

import java.util.HashMap;
import java.util.Map;

@DelegatesTo(IUtils.class)
public class LifecycleDelegatingUtilsService implements IUtils, IDependencyInjectableConcrete {
    private final Map<String, Object> configValues = new HashMap<>();

    @Override
    public String getServerID() {
        return "lifecycle-utils-server";
    }

    @Override
    public Map<String, Object> getConfigValues() {
        return configValues;
    }

    @Override
    public void setConfigValue(String key, Object value) {
        configValues.put(key, value);
    }
}
