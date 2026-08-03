package org.tavall.dependency.annotations.fixtures.arrayonly;

import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.dependency.fixtures.contracts.interfaces.IRedis;
import org.tavall.dependency.fixtures.contracts.interfaces.IUtils;
import org.tavall.dependency.fixtures.contracts.machine.data.interfaces.ILocalServerMetaData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@DelegatesTo({
        IRedis.class,
        IUtils.class,
        ILocalServerMetaData.class
})
public class ArrayOnlyDelegatingService implements IRedis, IUtils {
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
        serverId = "array-only-server";
    }

    @Override
    public void createGameID() {
        gameId = "array-only-game";
    }

    @Override
    public String generateRandomID(int length) {
        return "array-only-" + length;
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
