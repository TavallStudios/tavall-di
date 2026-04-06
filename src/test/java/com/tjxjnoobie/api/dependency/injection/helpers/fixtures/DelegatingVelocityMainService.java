package com.tjxjnoobie.api.dependency.injection.helpers.fixtures;

import com.tjxjnoobie.api.dependency.annotations.DelegatesToInterface;
import com.tjxjnoobie.api.platform.velocity.startup.interfaces.IVelocityMain;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.concurrent.atomic.AtomicInteger;

@DelegatesToInterface(getLinkedInterface = IVelocityMain.class)
public class DelegatingVelocityMainService implements IVelocityMain, com.tjxjnoobie.api.dependency.IDependencyInjectableConcrete {
    private static final AtomicInteger INITIALIZATION_CALLS = new AtomicInteger();
    private static volatile String lastCommand;
    private static volatile int lastAliasCount;

    public static void reset() {
        INITIALIZATION_CALLS.set(0);
        lastCommand = null;
        lastAliasCount = 0;
    }

    public static int getInitializationCalls() {
        return INITIALIZATION_CALLS.get();
    }

    public static String getLastCommand() {
        return lastCommand;
    }

    public static int getLastAliasCount() {
        return lastAliasCount;
    }

    @Override
    public void onProxyInitialization(ProxyInitializeEvent event) {
        INITIALIZATION_CALLS.incrementAndGet();
    }

    @Override
    public void registerCommand(String command, Command commandClass, ProxyServer proxyServer, String... aliases) {
        lastCommand = command;
        lastAliasCount = aliases == null ? 0 : aliases.length;
    }
}
