/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.player.cache.entity;

import org.cloudburstmc.protocol.bedrock.packet.AddItemEntityPacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AddItemEntityCache {

    private final Map<Long, AddItemEntityPacket> packets = new ConcurrentHashMap<>();

    public void storePacket(long entityRuntimeId, AddItemEntityPacket packet) {
        packets.put(entityRuntimeId, packet);
    }

    public Map<Long, AddItemEntityPacket> getPackets() {
        return packets;
    }

    public void removePacket(long entityRuntimeId) {
        packets.remove(entityRuntimeId);
    }
}