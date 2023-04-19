/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.player.cache.entity;

import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;

import java.util.HashMap;
import java.util.Map;

public class SetEntityDataCache {

    private final Map<Long, SetEntityDataPacket> packets = new HashMap<>();

    public void storePacket(long entityRuntimeId, SetEntityDataPacket packet) {
        packets.put(entityRuntimeId, packet);
    }

    public SetEntityDataPacket getPacket(long entityRuntimeId) {
        return packets.get(entityRuntimeId);
    }

    public void removePacket(long entityRuntimeId) {
        packets.remove(entityRuntimeId);
    }

    public Map<Long, SetEntityDataPacket> getPackets() {
        return packets;
    }

}