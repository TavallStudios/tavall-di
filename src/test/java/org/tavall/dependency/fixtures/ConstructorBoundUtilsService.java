package org.tavall.dependency.fixtures;

import org.tavall.dependency.IDependencyInjectableConcrete;
import org.tavall.dependency.fixtures.contracts.interfaces.IUtils;

import java.util.HashMap;
import java.util.Map;

public class ConstructorBoundUtilsService implements IUtils, IDependencyInjectableConcrete {
    private final Map<String, Object> configValues = new HashMap<>();
    private final String serverId;

    public ConstructorBoundUtilsService(String serverId) {
        this.serverId = serverId;
    }

    @Override
    public String getServerID() {
        return serverId;
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
