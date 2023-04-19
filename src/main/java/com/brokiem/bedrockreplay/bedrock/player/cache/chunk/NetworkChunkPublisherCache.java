/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.player.cache.chunk;

import org.cloudburstmc.protocol.bedrock.packet.NetworkChunkPublisherUpdatePacket;

import java.util.ArrayList;
import java.util.List;

public class NetworkChunkPublisherCache {

    private final List<NetworkChunkPublisherUpdatePacket> packets = new ArrayList<>();

    public void storePacket(NetworkChunkPublisherUpdatePacket packet) {
        packets.add(packet);
    }

    public List<NetworkChunkPublisherUpdatePacket> getPackets() {
        return packets;
    }
}