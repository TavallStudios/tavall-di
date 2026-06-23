package org.tavall.dependency.fixtures.contracts.interfaces;

import org.tavall.dependency.fixtures.contracts.machine.data.interfaces.ILocalServerMetaData;

import java.util.HashMap;
import java.util.Map;

public interface IUtils extends ILocalServerMetaData, org.tavall.dependency.IDependencyInjectableInterface {

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
