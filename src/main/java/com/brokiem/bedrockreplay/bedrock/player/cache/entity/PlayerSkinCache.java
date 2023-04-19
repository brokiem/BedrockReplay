/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.player.cache.entity;

import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket;

import java.util.HashMap;
import java.util.Map;

public class PlayerSkinCache {

    private final Map<String, PlayerSkinPacket> packets = new HashMap<>();

    public void storePacket(String uuid, PlayerSkinPacket packet) {
        packets.put(uuid, packet);
    }

    public Map<String, PlayerSkinPacket> getPackets() {
        return packets;
    }
}