package com.tjxjnoobie.api.machine.data.interfaces;

public interface ILocalServerMetaData extends com.tjxjnoobie.api.dependency.IDependencyInjectableInterface {

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
