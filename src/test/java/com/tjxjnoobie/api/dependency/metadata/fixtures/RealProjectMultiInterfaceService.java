package com.tjxjnoobie.api.dependency.metadata.fixtures;

import com.tjxjnoobie.api.interfaces.IRedis;
import com.tjxjnoobie.api.interfaces.IUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RealProjectMultiInterfaceService implements IRedis, IUtils, com.tjxjnoobie.api.dependency.IDependencyInjectableConcrete {
    private static final AtomicInteger REDIS_CONNECT_CALLS = new AtomicInteger();

    private final Map<String, Object> configValues = new HashMap<>();
    private String serverId = "";
    private String gameId = "";

    public static void reset() {
        REDIS_CONNECT_CALLS.set(0);
    }

    public static int getRedisConnectCalls() {
        return REDIS_CONNECT_CALLS.get();
    }

    @Override
    public void connectToRedis() {
        REDIS_CONNECT_CALLS.incrementAndGet();
    }

    @Override
    public String getServerID() {
        return serverId;
    }

    @Override
    public String getGameID() {
        return gameId;
    }

    @Override
    public String getLocalServerID() {
        return serverId;
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
        this.serverId = "multi-server";
    }

    @Override
    public void createGameID() {
        this.gameId = "multi-game";
    }

    @Override
    public String generateRandomID(int length) {
        return "multi-" + length;
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
