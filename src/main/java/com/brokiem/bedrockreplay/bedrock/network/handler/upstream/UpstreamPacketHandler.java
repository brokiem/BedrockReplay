/*
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.network.handler.upstream;

import com.alibaba.fastjson.JSONObject;
import com.brokiem.bedrockreplay.bedrock.player.ProxiedPlayer;
import com.brokiem.bedrockreplay.bedrock.player.cache.PlayerPacketCache;
import com.brokiem.bedrockreplay.bedrock.server.ProxyServer;
import com.brokiem.bedrockreplay.output.OutputWindow;
import com.brokiem.bedrockreplay.replay.ReplayData;
import com.brokiem.bedrockreplay.replay.ReplayManager;
import com.brokiem.bedrockreplay.utils.Callback;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.common.PacketSignal;

import java.util.Base64;

public class UpstreamPacketHandler implements BedrockPacketHandler {

    protected final ProxiedPlayer player;

    public UpstreamPacketHandler(ProxiedPlayer player) {
        this.player = player;
    }

    @Override
    public PacketSignal handle(RequestNetworkSettingsPacket packet) {
        NetworkSettingsPacket networkSettingsPacket = new NetworkSettingsPacket();
        networkSettingsPacket.setCompressionThreshold(1);
        networkSettingsPacket.setCompressionAlgorithm(PacketCompressionAlgorithm.ZLIB);
        networkSettingsPacket.setClientThrottleEnabled(false);
        networkSettingsPacket.setClientThrottleThreshold(0);
        networkSettingsPacket.setClientThrottleScalar(0);
        player.getSession().sendPacketImmediately(networkSettingsPacket);

        player.getSession().getPeer().setCompression(PacketCompressionAlgorithm.ZLIB);
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(LoginPacket packet) {
        String extraData = packet.getExtra().getParsedString();
        String downstreamAddress = ProxyServer.getInstance().getDownstreamAddress();
        int downstreamPort = ProxyServer.getInstance().getDownstreamPort();

        player.setSkinData(JSONObject.parseObject(new String(Base64.getUrlDecoder().decode(extraData.split("\\.")[1]))));

        PlayStatusPacket playStatusPacket = new PlayStatusPacket();
        playStatusPacket.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
        player.getSession().sendPacket(playStatusPacket);

        // Connect to downstream server
        player.connect(downstreamAddress, downstreamPort);
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ClientCacheStatusPacket packet) {
        if (player.isAllowSendPacket()) {
            player.sendDownstreamPacket(packet);
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(SetLocalPlayerAsInitializedPacket packet) {
        player.sendMessage("§c[!] You are joined through BedrockReplay proxy!");
        player.sendMessage("§e[!] Replay commands:");
        player.sendMessage("§a[!] !replay record <replayName> - start recording replay");
        player.sendMessage("§a[!] !replay stop - stop replay from recording");
        player.sendMessage("§a\n");
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(TextPacket packet) {
        String message = packet.getMessage();

        if (message.startsWith("!")) {
            String[] args = message.substring(1).split(" ");
            if (args.length > 0) {
                String command = args[0];
                String[] commandArgs = new String[args.length - 1];
                System.arraycopy(args, 1, commandArgs, 0, commandArgs.length);

                if (command.equals("replay")) {
                    if (commandArgs.length > 0) {
                        switch (commandArgs[0]) {
                            case "record" -> {
                                if (commandArgs.length > 1) {
                                    String replayName = commandArgs[1];
                                    ReplayData replayData = ReplayManager.getInstance().getReplay(replayName);
                                    if (replayData == null) {
                                        replayData = ReplayManager.getInstance().createReplay(player.getPlayerInfo().username(), replayName);
                                    }
                                    if (replayData != null && !replayData.isRecording()) {
                                        replayData.setRecorderEntityRuntimeId(player.getEntityRuntimeId());
                                        // add cached packets to replay data
                                        PlayerPacketCache packetCache = player.getPacketCache();
                                        replayData.addPacketCache(packetCache);
                                        // start recording
                                        replayData.startRecording();
                                        player.sendMessage("§a[!] Replay started recording! (ID: " + replayName + ")");
                                    } else {
                                        player.sendMessage("§c[!] You are already started recording a replay!");
                                    }
                                } else {
                                    player.sendMessage("§c[!] Please enter a replay name!");
                                }
                                return PacketSignal.HANDLED;
                            }
                            case "stop" -> {
                                ReplayData replayData = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
                                if (replayData != null) {
                                    player.sendMessage("§a[!] Saving replay '" + replayData.getReplayId() + "'...");
                                    replayData.stopRecording();
                                    replayData.saveToFile("./replays/" + replayData.getReplayId(), new Callback() {
                                        @Override
                                        public void onSuccess() {
                                            player.sendMessage("§a[!] Replay saved! (ID: " + replayData.getReplayId() + ")");
                                            ReplayManager.getInstance().removeReplay(replayData.getReplayId());
                                        }

                                        @Override
                                        public void onFailure(String errorMessage) {
                                            player.sendMessage("§c[!] Failed to save replay: " + errorMessage);
                                        }
                                    });
                                } else {
                                    player.sendMessage("§c[!] You are not started recording a replay yet!");
                                }
                                return PacketSignal.HANDLED;
                            }
                        }
                    } else {
                        player.sendMessage("§cUsage: !replay <record|stop> [replayName]");
                        return PacketSignal.HANDLED;
                    }
                } else {
                    player.sendMessage("§cCommand list: !replay");
                    return PacketSignal.HANDLED;
                }
            }
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(PlayerAuthInputPacket packet) {
        ReplayData replay = ReplayManager.getInstance().getRecordingReplayByPlayer(player.getPlayerInfo().username());
        if (replay != null) {
            if (replay.isRecording()) {
                MovePlayerPacket pk = new MovePlayerPacket();
                pk.setRuntimeEntityId(replay.getRecorderEntityRuntimeId());
                pk.setPosition(packet.getPosition());
                pk.setRotation(packet.getRotation());
                pk.setOnGround(false);
                pk.setTick(replay.getTick());
                pk.setMode(MovePlayerPacket.Mode.NORMAL);
                pk.setTeleportationCause(MovePlayerPacket.TeleportationCause.UNKNOWN);
                pk.setEntityType(0);
                pk.setRidingRuntimeEntityId(0);
                replay.addPacket(pk);
            }
        }
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
    public PacketSignal handle(InventoryTransactionPacket packet) {
        // TODO: convert InventoryTransactionPacket to InventorySlotPacket/InventoryContentPacket
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(ItemStackRequestPacket packet) {
        // TODO: convert ItemStackRequestPacket to InventorySlotPacket/InventoryContentPacket
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(PacketViolationWarningPacket packet) {
        OutputWindow.print("Packet violation warning: " + packet.getContext());
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handlePacket(BedrockPacket packet) {
        //OutputWindow.print("UPSTREAM -> DOWN: " + packet.toString());

        PacketSignal packetSignal = packet.handle(this);
        if (packetSignal == PacketSignal.HANDLED) {
            return packetSignal;
        }

        player.getDownstreamSession().sendPacket(packet);
        return PacketSignal.HANDLED;
    }

    @Override
    public void onDisconnect(String reason) {
        OutputWindow.print("Disconnect packet received from upstream server: " + reason);
        player.disconnect(reason);
    }
}
