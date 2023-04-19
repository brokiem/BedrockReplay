/*
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.network.handler.downstream;

import com.alibaba.fastjson.JSONObject;
import com.brokiem.bedrockreplay.account.AccountManager;
import com.brokiem.bedrockreplay.auth.Live;
import com.brokiem.bedrockreplay.bedrock.network.registry.FakeDefinitionRegistry;
import com.brokiem.bedrockreplay.bedrock.player.ProxiedPlayer;
import com.brokiem.bedrockreplay.bedrock.player.cache.chunk.LevelChunkCache;
import com.brokiem.bedrockreplay.bedrock.player.cache.entity.SetEntityDataCache;
import com.brokiem.bedrockreplay.bedrock.server.ProxyServer;
import com.brokiem.bedrockreplay.output.OutputWindow;
import com.brokiem.bedrockreplay.replay.ReplayData;
import com.brokiem.bedrockreplay.replay.ReplayManager;
import com.brokiem.bedrockreplay.utils.FileManager;
import com.nimbusds.jwt.SignedJWT;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.SneakyThrows;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.data.defintions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.protocol.common.SimpleDefinitionRegistry;

import javax.crypto.SecretKey;
import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

public class DownstreamPacketHandler implements BedrockPacketHandler {

    protected final ProxiedPlayer player;

    public DownstreamPacketHandler(ProxiedPlayer player) {
        this.player = player;
    }

    @SneakyThrows
    @Override
    public PacketSignal handle(NetworkSettingsPacket packet) {
        player.getDownstreamSession().getPeer().setCodec(ProxyServer.BEDROCK_CODEC);
        player.getDownstreamSession().getPeer().setCompression(packet.getCompressionAlgorithm());
        player.getDownstreamSession().getPeer().setCompressionLevel(packet.getCompressionThreshold());

        try {
            player.getDownstreamSession().sendPacketImmediately(player.getLoginPacket());
        } catch (Exception ignored) {
            String newAccessToken = Live.getInstance().refreshToken(player.getRefreshToken()).getString("access_token");
            player.setAccessToken(newAccessToken);

            JSONObject json = new JSONObject();
            json.put("access_token", Base64.getEncoder().encodeToString(newAccessToken.getBytes()));
            json.put("refresh_token", Base64.getEncoder().encodeToString(player.getRefreshToken().getBytes()));
            String encrypted = AccountManager.encrypt(json.toJSONString());
            FileManager.writeToFile(".account", encrypted);

            player.getDownstreamSession().sendPacketImmediately(player.getLoginPacket());
        }

        player.setAllowSendPacket(true);
        return PacketSignal.HANDLED;
    }

    @SneakyThrows
    @Override
    public PacketSignal handle(ServerToClientHandshakePacket packet) {
        SignedJWT saltJwt = SignedJWT.parse(packet.getJwt());
        URI x5u = saltJwt.getHeader().getX509CertURL();
        ECPublicKey serverKey = EncryptionUtils.generateKey(x5u.toASCIIString());
        SecretKey key = EncryptionUtils.getSecretKey(this.player.getKeyPair().getPrivate(), serverKey, Base64.getDecoder().decode(saltJwt.getJWTClaimsSet().getStringClaim("salt")));
        this.player.getDownstreamSession().enableEncryption(key);

        ClientToServerHandshakePacket clientToServerHandshake = new ClientToServerHandshakePacket();
        player.getDownstreamSession().sendPacket(clientToServerHandshake);
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(StartGamePacket packet) {
        SetPlayerGameTypePacket setPlayerGameTypePacket = new SetPlayerGameTypePacket();
        setPlayerGameTypePacket.setGamemode(packet.getPlayerGameType().ordinal());
        player.getPacketCache().getSetPlayerGameTypeCache().setPacket(setPlayerGameTypePacket);

        BedrockCodecHelper upstreamCodecHelper = player.getSession().getPeer().getCodecHelper();
        BedrockCodecHelper downstreamCodecHelper = player.getDownstreamSession().getPeer().getCodecHelper();
        SimpleDefinitionRegistry.Builder<ItemDefinition> itemRegistry = SimpleDefinitionRegistry.builder();
        IntSet runtimeIds = new IntOpenHashSet();
        for (ItemDefinition definition : packet.getItemDefinitions()) {
            if (runtimeIds.add(definition.getRuntimeId())) {
                itemRegistry.add(definition);
            }
        }
        upstreamCodecHelper.setItemDefinitions(itemRegistry.build());
        downstreamCodecHelper.setItemDefinitions(itemRegistry.build());
        upstreamCodecHelper.setBlockDefinitions(FakeDefinitionRegistry.createBlockRegistry());
        downstreamCodecHelper.setBlockDefinitions(FakeDefinitionRegistry.createBlockRegistry());

        player.setEntityRuntimeId(packet.getRuntimeEntityId());
        player.getSession().sendPacket(packet);
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(MovePlayerPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(LevelChunkPacket packet) {
        // retain data so it doesn't get released
        packet.getData().retain();

        int x = packet.getChunkX();
        int y = packet.getChunkZ();

        ReplayData replayData = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replayData != null) {
            if (replayData.isRecording()) {
                replayData.addPacket(packet);
            }
        }

        LevelChunkCache.ChunkCoordinate chunkCoordinate = new LevelChunkCache.ChunkCoordinate(x, y);
        player.getPacketCache().getLevelChunkCache().storePacket(chunkCoordinate, packet);
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(SubChunkPacket packet) {
        ReplayData replayData = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replayData != null) {
            if (replayData.isRecording()) {
                replayData.addPacket(packet);
            }
        }

        player.getPacketCache().getSubChunkCache().storePacket(packet);
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(AddPlayerPacket packet) {
        ReplayData replayData = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replayData != null) {
            if (replayData.isRecording()) {
                replayData.addPacket(packet);
            }
        }
        player.getPacketCache().getAddPlayerCache().storePacket(packet.getRuntimeEntityId(), packet);
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(RemoveEntityPacket packet) {
        ReplayData replayData = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replayData != null) {
            if (replayData.isRecording()) {
                replayData.addPacket(packet);
            }
        }
        player.getPacketCache().getAddPlayerCache().removePacket(packet.getUniqueEntityId());
        player.getPacketCache().getAddItemEntityCache().removePacket(packet.getUniqueEntityId());
        player.getPacketCache().getAddEntityCache().removePacket(packet.getUniqueEntityId());
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(MoveEntityAbsolutePacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(NetworkChunkPublisherUpdatePacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        player.getPacketCache().getChunkPublisherCache().storePacket(packet);
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(UpdateBlockPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        player.getPacketCache().getUpdateBlockCache().storePacket(packet.getBlockPosition(), packet);
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(AddEntityPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        player.getPacketCache().getAddEntityCache().storePacket(packet.getRuntimeEntityId(), packet);
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(AddItemEntityPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        player.getPacketCache().getAddItemEntityCache().storePacket(packet.getRuntimeEntityId(), packet);
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(InventoryContentPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        player.getPacketCache().getInventoryContentCache().storePacket(packet);
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(InventorySlotPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        player.getPacketCache().getInventorySlotCache().storePacket(packet);
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(MobEquipmentPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        player.getPacketCache().getMobEquipmentCache().storePacket(packet.getRuntimeEntityId(), packet);
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(MobArmorEquipmentPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        player.getPacketCache().getMobArmorEquipmentCache().storePacket(packet.getRuntimeEntityId(), packet);
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(SetScorePacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        player.getPacketCache().getSetScoreCache().setPacket(packet);
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(SetDisplayObjectivePacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        player.getPacketCache().getSetDisplayObjectiveCache().storePacket(packet.getObjectiveId(), packet);
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(LevelSoundEventPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(LevelSoundEvent1Packet packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(LevelSoundEvent2Packet packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(TakeItemEntityPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null && replay.isRecording()) {
            replay.addPacket(packet);
        }
        player.getPacketCache().getAddItemEntityCache().removePacket(packet.getItemRuntimeEntityId());
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(LevelEventPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(AnimatePacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(RemoveObjectivePacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        player.getPacketCache().getSetDisplayObjectiveCache().removePacket(packet.getObjectiveId());
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(TextPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(PlayerSkinPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        player.getPacketCache().getPlayerSkinCache().storePacket(packet.getUuid().toString(), packet);
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(PlaySoundPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(EntityEventPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(SetPlayerGameTypePacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        player.getPacketCache().getSetPlayerGameTypeCache().setPacket(packet);
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(StopSoundPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(PlayerListPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        if (packet.getAction().ordinal() == PlayerListPacket.Action.ADD.ordinal()) {
            for (PlayerListPacket.Entry entry : packet.getEntries()) {
                player.getPacketCache().getPlayerListEntryCache().storeEntry(entry.getUuid().toString(), entry);
            }
        } else if (packet.getAction().ordinal() == PlayerListPacket.Action.REMOVE.ordinal()) {
            for (PlayerListPacket.Entry entry : packet.getEntries()) {
                player.getPacketCache().getPlayerListEntryCache().removeEntry(entry.getUuid().toString());
            }
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(SetEntityDataPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        SetEntityDataCache setEntityDataCache = player.getPacketCache().getSetEntityDataCache();
        if (packet.getRuntimeEntityId() == player.getEntityRuntimeId()) {
            if (packet.getMetadata().get(EntityDataTypes.NAME) != null || setEntityDataCache.getPacket(packet.getRuntimeEntityId()) == null) {
                setEntityDataCache.storePacket(packet.getRuntimeEntityId(), packet);
            } else {
                String nametag = setEntityDataCache.getPacket(packet.getRuntimeEntityId()).getMetadata().get(EntityDataTypes.NAME);
                if (nametag != null) {
                    packet.getMetadata().put(EntityDataTypes.NAME, nametag);
                }
                setEntityDataCache.storePacket(packet.getRuntimeEntityId(), packet);
            }
        } else {
            setEntityDataCache.storePacket(packet.getRuntimeEntityId(), packet);
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(SetEntityMotionPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(BlockEventPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(BossEventPacket packet) {
        if (packet.getPlayerUniqueEntityId() != player.getEntityRuntimeId()) {
            return PacketSignal.UNHANDLED;
        }

        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        player.getPacketCache().getBossEventCache().storePacket(packet.getPlayerUniqueEntityId(), packet);
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(SetTitlePacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(SetTimePacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                replay.addPacket(packet);
            }
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handlePacket(BedrockPacket packet) {
        //OutputWindow.print("DOWNSTREAM -> UP: " + packet.toString());

        PacketSignal packetSignal = packet.handle(this);
        if (packetSignal == PacketSignal.HANDLED) {
            return packetSignal;
        }

        player.getSession().sendPacket(packet);
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(DisconnectPacket packet) {
        OutputWindow.print("Disconnected from downstream server: " + packet.getKickMessage());
        player.disconnect(packet.getKickMessage());
        return PacketSignal.HANDLED;
    }

    @Override
    public void onDisconnect(String reason) {
        OutputWindow.print("Disconnected from downstream server: " + reason);
        player.disconnect(reason);
    }
}
