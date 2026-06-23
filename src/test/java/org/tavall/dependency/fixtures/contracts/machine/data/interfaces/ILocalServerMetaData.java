package org.tavall.dependency.fixtures.contracts.machine.data.interfaces;

public interface ILocalServerMetaData extends org.tavall.dependency.IDependencyInjectableInterface {

    default String getLocalServerID() {
        return "";
    }

    default void setServerID(String serverID) {
    }

    default String getGameID() {
        return "";
    }

    default void setGameID(String gameID) {
    }
}
