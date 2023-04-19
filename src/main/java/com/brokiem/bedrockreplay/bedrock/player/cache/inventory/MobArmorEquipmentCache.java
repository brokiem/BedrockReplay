/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.player.cache.inventory;

import org.cloudburstmc.protocol.bedrock.packet.MobArmorEquipmentPacket;

import java.util.HashMap;
import java.util.Map;

public class MobArmorEquipmentCache {

    private final Map<Long, MobArmorEquipmentPacket> packets = new HashMap<>();

    public void storePacket(long runtimeEntityId, MobArmorEquipmentPacket packet) {
        packets.put(runtimeEntityId, packet);
    }

    public Map<Long, MobArmorEquipmentPacket> getPackets() {
        return packets;
    }
}