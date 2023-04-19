/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.player.cache.block;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UpdateBlockCache {

    private final Map<Vector3i, UpdateBlockPacket> packets = new ConcurrentHashMap<>();

    public void storePacket(Vector3i blockPosition, UpdateBlockPacket packet) {
        packets.put(blockPosition, packet);
    }

    public UpdateBlockPacket getPacket(Vector3i blockPosition) {
        return packets.get(blockPosition);
    }

    public Map<Vector3i, UpdateBlockPacket> getPackets() {
        return packets;
    }

    public boolean containsPacket(Vector3i blockPosition) {
        return packets.containsKey(blockPosition);
    }

    public void removePacket(Vector3i blockPosition) {
        packets.remove(blockPosition);
    }
}