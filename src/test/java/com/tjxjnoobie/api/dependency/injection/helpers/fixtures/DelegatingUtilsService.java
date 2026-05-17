package com.tjxjnoobie.api.dependency.injection.helpers.fixtures;

import com.tjxjnoobie.api.dependency.annotations.DelegatesToInterface;
import com.tjxjnoobie.api.interfaces.IUtils;

import java.util.HashMap;
import java.util.Map;

@DelegatesToInterface(getLinkedInterface = IUtils.class)
public class DelegatingUtilsService implements IUtils, com.tjxjnoobie.api.dependency.IDependencyInjectableConcrete {
    private final Map<String, Object> configValues = new HashMap<>();
    private String serverId = "";
    private String gameId = "";

    @Override
    public String getServerID() {
        return serverId;
    }

    @Override
    public String getGameID() {
        return gameId;
    }

    @Override
    public void setServerID(String serverID) {
        this.serverId = serverID;
    }

    @Override
    public void setGameID(String gameID) {
        this.gameId = gameID;
    }

    @Override
    public void createServerID() {
        this.serverId = "server-generated";
    }

    @Override
    public void createGameID() {
        this.gameId = "game-generated";
    }

    @Override
    public String generateRandomID(int length) {
        return "generated-" + length;
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
