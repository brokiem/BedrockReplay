/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.player.cache.inventory;

import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket;

import java.util.ArrayList;
import java.util.List;

public class InventorySlotCache {

    private final List<InventorySlotPacket> packets = new ArrayList<>();

    public void storePacket(InventorySlotPacket packet) {
        packets.add(packet);
    }

    public List<InventorySlotPacket> getPackets() {
        return packets;
    }
}