/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.player.cache.score;

import org.cloudburstmc.protocol.bedrock.packet.SetScorePacket;

public class SetScoreCache {

    private SetScorePacket packet = null;

    public void setPacket(SetScorePacket packet) {
        this.packet = packet;
    }

    public SetScorePacket getPacket() {
        return this.packet;
    }
}