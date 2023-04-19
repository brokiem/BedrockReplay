/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.player.cache.inventory;

import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;

import java.util.HashMap;
import java.util.Map;

public class MobEquipmentCache {

    private final Map<Long, MobEquipmentPacket> packets = new HashMap<>();

    public void storePacket(long runtimeEntityId, MobEquipmentPacket packet) {
        packets.put(runtimeEntityId, packet);
    }

    public Map<Long, MobEquipmentPacket> getPackets() {
        return packets;
    }
}