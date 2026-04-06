package com.tjxjnoobie.api.interfaces;

import com.tjxjnoobie.api.dependency.annotations.ComposesToInterface;
import com.tjxjnoobie.api.dependency.composition.domains.IInfrastructureDomain;
import com.tjxjnoobie.api.machine.data.interfaces.ILocalServerMetaData;

import java.util.HashMap;
import java.util.Map;

@ComposesToInterface(IInfrastructureDomain.class)
public interface IUtils extends ILocalServerMetaData, com.tjxjnoobie.api.dependency.IDependencyInjectableInterface {

    default String getServerID() {
        return "";
    }

    default String getGameID() {
        return "";
    }

    default void setServerID(String serverID) {
    }

    default void setGameID(String gameID) {
    }

    default void createServerID() {
    }

    default void createGameID() {
    }

    default String generateRandomID(int length) {
        return "";
    }

    default Map<String, Object> getConfigValues() {
        return new HashMap<>();
    }

    default void setConfigValue(String key, Object value) {
    }
}
