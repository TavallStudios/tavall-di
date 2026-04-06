package com.tjxjnoobie.api.dependency.injection.helpers.fixtures;

import com.tjxjnoobie.api.dependency.annotations.DelegatesToInterface;
import com.tjxjnoobie.api.interfaces.IRedis;

import java.util.concurrent.atomic.AtomicInteger;

@DelegatesToInterface(getLinkedInterface = IRedis.class)
public class DelegatingRedisService implements IRedis, com.tjxjnoobie.api.dependency.IDependencyInjectableConcrete {
    private static final AtomicInteger CONNECT_CALLS = new AtomicInteger();
    private static final AtomicInteger DISCONNECT_CALLS = new AtomicInteger();
    private static volatile String lastPublishedMessage;

    public static void reset() {
        CONNECT_CALLS.set(0);
        DISCONNECT_CALLS.set(0);
        lastPublishedMessage = null;
    }

    public static int getConnectCalls() {
        return CONNECT_CALLS.get();
    }

    public static int getDisconnectCalls() {
        return DISCONNECT_CALLS.get();
    }

    public static String getLastPublishedMessage() {
        return lastPublishedMessage;
    }

    @Override
    public void connectToRedis() {
        CONNECT_CALLS.incrementAndGet();
    }

    @Override
    public void disconnectFromRedis() {
        DISCONNECT_CALLS.incrementAndGet();
    }

    @Override
    public void publishRedisUpdate(String message) {
        lastPublishedMessage = message;
    }
}
