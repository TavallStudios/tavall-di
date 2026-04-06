package com.tjxjnoobie.api.dependency.injection.helpers.multiplefixtures;

import com.tjxjnoobie.api.dependency.annotations.DelegatesToInterface;
import com.tjxjnoobie.api.interfaces.IRedis;
import com.tjxjnoobie.api.interfaces.IUtils;
import com.tjxjnoobie.api.machine.data.interfaces.ILocalServerMetaData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@DelegatesToInterface(
        getLinkedInterface = IRedis.class,
        getLinkedInterfaces = {IUtils.class, ILocalServerMetaData.class}
)
public class DelegatingMultiInterfaceService implements IRedis, IUtils, com.tjxjnoobie.api.dependency.IDependencyInjectableConcrete {
    private static final AtomicInteger CONNECT_CALLS = new AtomicInteger();

    private final Map<String, Object> configValues = new HashMap<>();
    private String serverId = "";
    private String gameId = "";

    public static void reset() {
        CONNECT_CALLS.set(0);
    }

    public static int getConnectCalls() {
        return CONNECT_CALLS.get();
    }

    @Override
    public void connectToRedis() {
        CONNECT_CALLS.incrementAndGet();
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
        this.serverId = "annotated-multi-server";
    }

    @Override
    public void createGameID() {
        this.gameId = "annotated-multi-game";
    }

    @Override
    public String generateRandomID(int length) {
        return "annotated-multi-" + length;
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
