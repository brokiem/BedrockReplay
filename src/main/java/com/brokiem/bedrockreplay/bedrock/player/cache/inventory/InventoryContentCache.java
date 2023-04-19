/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.player.cache.inventory;

import org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket;

import java.util.ArrayList;
import java.util.List;

public class InventoryContentCache {

    private final List<InventoryContentPacket> packets = new ArrayList<>();

    public void storePacket(InventoryContentPacket packet) {
        packets.add(packet);
    }

    public List<InventoryContentPacket> getPackets() {
        return packets;
    }
}