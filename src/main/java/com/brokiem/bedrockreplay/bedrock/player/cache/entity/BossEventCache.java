/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.player.cache.entity;

import org.cloudburstmc.protocol.bedrock.packet.BossEventPacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BossEventCache {

    private final Map<Long, BossEventPacket> packets = new ConcurrentHashMap<>();

    public void storePacket(long entityRuntimeId, BossEventPacket packet) {
        packets.put(entityRuntimeId, packet);
    }

    public Map<Long, BossEventPacket> getPackets() {
        return packets;
    }

    public void removePacket(long entityRuntimeId) {
        packets.remove(entityRuntimeId);
    }
}