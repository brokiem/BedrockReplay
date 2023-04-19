/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.player.cache.entity;

import org.cloudburstmc.protocol.bedrock.packet.SetPlayerGameTypePacket;

public class SetPlayerGameTypeCache {

    private SetPlayerGameTypePacket packet = null;

    public void setPacket(SetPlayerGameTypePacket packet) {
        this.packet = packet;
    }

    public SetPlayerGameTypePacket getPacket() {
        return this.packet;
    }
}