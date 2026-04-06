package com.tjxjnoobie.api.dependency.injection.helpers.lifecyclefixtures;

import com.tjxjnoobie.api.dependency.IDependencyInjectableConcrete;
import com.tjxjnoobie.api.dependency.annotations.DelegatesToInterface;
import com.tjxjnoobie.api.interfaces.IUtils;

import java.util.HashMap;
import java.util.Map;

@DelegatesToInterface(getLinkedInterface = IUtils.class)
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
