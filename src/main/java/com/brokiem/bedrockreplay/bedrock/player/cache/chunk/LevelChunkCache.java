/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.player.cache.chunk;

import org.cloudburstmc.protocol.bedrock.packet.LevelChunkPacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LevelChunkCache {

    private final Map<ChunkCoordinate, LevelChunkPacket> packets = new ConcurrentHashMap<>();

    public void storePacket(ChunkCoordinate chunkCoordinate, LevelChunkPacket packet) {
        packets.put(chunkCoordinate, packet);
    }

    public LevelChunkPacket getPacket(ChunkCoordinate chunkCoordinate) {
        return packets.get(chunkCoordinate);
    }

    public Map<ChunkCoordinate, LevelChunkPacket> getPackets() {
        return packets;
    }

    public boolean containsPacket(ChunkCoordinate chunkCoordinate) {
        return packets.containsKey(chunkCoordinate);
    }

    public LevelChunkPacket removePacket(ChunkCoordinate chunkCoordinate) {
        return packets.remove(chunkCoordinate);
    }

    public record ChunkCoordinate(int x, int y) {
        @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                ChunkCoordinate that = (ChunkCoordinate) o;
                return x == that.x && y == that.y;
            }

    }
}