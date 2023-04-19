/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.player.cache.chunk;

import org.cloudburstmc.protocol.bedrock.packet.SubChunkPacket;

import java.util.ArrayList;
import java.util.List;

public class SubChunkCache {

    private final List<SubChunkPacket> packets = new ArrayList<>();

    public void storePacket(SubChunkPacket packet) {
        packets.add(packet);
    }

    public List<SubChunkPacket> getPackets() {
        return packets;
    }
}