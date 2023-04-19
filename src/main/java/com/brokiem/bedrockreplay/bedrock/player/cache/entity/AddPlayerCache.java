/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.player.cache.entity;

import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AddPlayerCache {

    private final Map<Long, AddPlayerPacket> packets = new ConcurrentHashMap<>();

    public void storePacket(long entityRuntimeId, AddPlayerPacket packet) {
        packets.put(entityRuntimeId, packet);
    }

    public Map<Long, AddPlayerPacket> getPackets() {
        return packets;
    }

    public void removePacket(long entityRuntimeId) {
        packets.remove(entityRuntimeId);
    }
}