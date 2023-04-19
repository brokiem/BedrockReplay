/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.player.cache.score;

import org.cloudburstmc.protocol.bedrock.packet.SetDisplayObjectivePacket;

import java.util.HashMap;
import java.util.Map;

public class SetDisplayObjectiveCache {

    private final Map<String, SetDisplayObjectivePacket> packets = new HashMap<>();

    public void storePacket(String objectiveId, SetDisplayObjectivePacket packet) {
        this.packets.put(objectiveId, packet);
    }

    public void removePacket(String objectiveId) {
        this.packets.remove(objectiveId);
    }

    public Map<String, SetDisplayObjectivePacket> getPackets() {
        return this.packets;
    }
}