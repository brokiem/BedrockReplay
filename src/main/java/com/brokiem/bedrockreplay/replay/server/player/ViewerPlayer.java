/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.replay.server.player;

import com.brokiem.bedrockreplay.replay.ReplayManager;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetPlayerGameTypePacket;
import org.cloudburstmc.protocol.bedrock.packet.TextPacket;

import java.util.Random;
import java.util.UUID;

public class ViewerPlayer {

    @Setter
    @Getter
    private long entityRuntimeId;

    @Getter
    private final BedrockServerSession session;

    @Setter
    @Getter
    private int viewDistance = 8;

    private boolean freeCamera = false;

    @Getter
    private final UUID clonedPlayerUuid;
    @Getter
    private final long clonedPlayerId;

    @Setter
    @Getter
    private int lastGamemode = GameMode.UNKNOWN.ordinal();
    @Setter
    @Getter
    private SetEntityDataPacket lastSetEntityDataPacket;

    public enum GameMode {
        SURVIVAL,
        CREATIVE,
        ADVENTURE,
        SPECTATOR,
        UNKNOWN
    }

    public ViewerPlayer(BedrockServerSession session) {
        this.clonedPlayerId = new Random().nextLong();
        this.clonedPlayerUuid = UUID.randomUUID();
        this.session = session;
    }

    public void sendMessage(String message) {
        TextPacket textPacket = new TextPacket();
        textPacket.setType(TextPacket.Type.RAW);
        textPacket.setNeedsTranslation(false);
        textPacket.setSourceName("");
        textPacket.setXuid("");
        textPacket.setMessage(message);
        this.session.sendPacket(textPacket);
    }

    public void setGamemode(GameMode gamemode) {
        if (gamemode.ordinal() == GameMode.UNKNOWN.ordinal()) {
            return;
        }

        SetPlayerGameTypePacket setPlayerGameTypePacket = new SetPlayerGameTypePacket();
        setPlayerGameTypePacket.setGamemode(gamemode.ordinal());
        this.session.sendPacket(setPlayerGameTypePacket);
    }

    public void setFreeCamera(boolean freeCamera) {
        this.freeCamera = freeCamera;

        if (freeCamera) {
            this.setGamemode(GameMode.CREATIVE);
            ReplayManager.getInstance().spawnPlayerClone(this);
        } else {
            ReplayManager.getInstance().despawnPlayerClone(this);
            this.setGamemode(GameMode.values()[this.lastGamemode]);
        }
    }

    public boolean isFreeCamera() {
        return this.freeCamera;
    }
}