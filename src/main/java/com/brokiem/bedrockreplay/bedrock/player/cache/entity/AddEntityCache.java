/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.player.cache.entity;

import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AddEntityCache {

    private final Map<Long, AddEntityPacket> packets = new ConcurrentHashMap<>();

    public void storePacket(long entityRuntimeId, AddEntityPacket packet) {
        packets.put(entityRuntimeId, packet);
    }

    public Map<Long, AddEntityPacket> getPackets() {
        return packets;
    }

    public void removePacket(long entityRuntimeId) {
        packets.remove(entityRuntimeId);
    }
}