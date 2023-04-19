/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.player.cache.entity;

import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;

import java.util.HashMap;
import java.util.Map;

public class PlayerListEntryCache {

    private final Map<String, PlayerListPacket.Entry> entries = new HashMap<>();

    public void storeEntry(String uuid, PlayerListPacket.Entry entry) {
        entries.put(uuid, entry);
    }

    public void removeEntry(String uuid) {
        entries.remove(uuid);
    }

    public Map<String, PlayerListPacket.Entry> getEntries() {
        return entries;
    }
}