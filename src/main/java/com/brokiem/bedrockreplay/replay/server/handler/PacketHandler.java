/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.replay.server.handler;

import com.brokiem.bedrockreplay.replay.ReplayData;
import com.brokiem.bedrockreplay.replay.ReplayManager;
import com.brokiem.bedrockreplay.replay.server.ReplayViewerServer;
import com.brokiem.bedrockreplay.replay.server.player.ViewerPlayer;
import com.brokiem.bedrockreplay.utils.Callback;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.common.PacketSignal;

import java.util.List;

public class PacketHandler implements BedrockPacketHandler {

    private final ViewerPlayer player;

    public PacketHandler(ViewerPlayer player) {
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
        this.player.setEntityRuntimeId((long) (Math.random() * Long.MAX_VALUE));

        PlayStatusPacket status = new PlayStatusPacket();
        status.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
        player.getSession().sendPacket(status);

        ResourcePacksInfoPacket resourcePacksInfoPacket = new ResourcePacksInfoPacket();
        resourcePacksInfoPacket.setForcingServerPacksEnabled(false);
        resourcePacksInfoPacket.setScriptingEnabled(false);
        resourcePacksInfoPacket.setForcedToAccept(false);
        player.getSession().sendPacket(resourcePacksInfoPacket);

        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(RequestChunkRadiusPacket packet) {
        player.setViewDistance(packet.getRadius());

        ChunkRadiusUpdatedPacket chunkRadiusUpdatedPacket = new ChunkRadiusUpdatedPacket();
        chunkRadiusUpdatedPacket.setRadius(packet.getRadius());
        player.getSession().sendPacket(chunkRadiusUpdatedPacket);
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ResourcePackClientResponsePacket packet) {
        if (packet.getStatus() == ResourcePackClientResponsePacket.Status.HAVE_ALL_PACKS) {
            ResourcePackStackPacket stack = new ResourcePackStackPacket();
            stack.setForcedToAccept(false);
            stack.setGameVersion(ReplayViewerServer.BEDROCK_CODEC.getMinecraftVersion());
            stack.setExperimentsPreviouslyToggled(false);
            player.getSession().sendPacket(stack);
        }

        if (packet.getStatus() == ResourcePackClientResponsePacket.Status.COMPLETED) {
            ReplayViewerServer.sendEmptyChunks(player);
            ReplayViewerServer.sendStartGame(player);
            ReplayViewerServer.spawnPlayer(player);
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(SetLocalPlayerAsInitializedPacket packet) {
        SetEntityDataPacket setEntityDataPacket = new SetEntityDataPacket();
        setEntityDataPacket.setRuntimeEntityId(packet.getRuntimeEntityId());
        setEntityDataPacket.setTick(0);
        setEntityDataPacket.getMetadata().setFlag(EntityFlag.BREATHING, true);
        setEntityDataPacket.getMetadata().setFlag(EntityFlag.HAS_GRAVITY, true);
        setEntityDataPacket.getMetadata().setFlag(EntityFlag.NO_AI, true);
        player.getSession().sendPacketImmediately(setEntityDataPacket);

        SetTimePacket setTimePacket = new SetTimePacket();
        setTimePacket.setTime(6000);
        player.getSession().sendPacket(setTimePacket);

        player.sendMessage("§a[!] Welcome to BedrockReplay Viewer Server!");
        player.sendMessage("§e[!] Commands:");
        player.sendMessage("§a[!] !replay play <replayName> - play replay");
        player.sendMessage("§a[!] !replay pause/resume <replayName> - pause/resume replay");
        player.sendMessage("§a[!] !replay stop <replayName> - stop replay");
        player.sendMessage("§a[!] !replay list - list all replays");
        player.sendMessage("§a[!] !free - enable/disable free camera mode");
        return PacketSignal.HANDLED;
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
                            case "play" -> {
                                if (commandArgs.length > 1) {
                                    String replayName = commandArgs[1];
                                    ReplayData replayData = ReplayManager.getInstance().getReplay(replayName);

                                    if (replayData == null) {
                                        replayData = ReplayManager.getInstance().createReplay(packet.getSourceName(), replayName);
                                    }

                                    if (replayData.isPlaying()) {
                                        player.sendMessage("§c[!] Replay '" + replayName + "' is already playing!");
                                        return PacketSignal.HANDLED;
                                    }

                                    player.sendMessage("§a[!] Loading replay '" + replayName + "'...");
                                    ReplayData finalReplayData = replayData;
                                    finalReplayData.loadFromFile("./replays/" + replayName, new Callback() {
                                        @Override
                                        public void onSuccess() {
                                            player.sendMessage("§a[!] Replay loaded and started!");
                                            ReplayManager.getInstance().playReplay(replayName, player);
                                        }

                                        @Override
                                        public void onFailure(String errorMessage) {
                                            player.sendMessage("§c[!] Failed to load replay '" + replayName + "'! " + errorMessage);
                                        }
                                    });
                                } else {
                                    player.sendMessage("§c[!] Usage: !replay play <replayName>");
                                }
                            }
                            case "pause" -> {
                                if (commandArgs.length > 1) {
                                    String replayName = commandArgs[1];
                                    ReplayData replayData = ReplayManager.getInstance().getReplay(replayName);

                                    if (replayData == null) {
                                        player.sendMessage("§c[!] Replay '" + replayName + "' is not playing!");
                                        return PacketSignal.HANDLED;
                                    }

                                    if (!replayData.isPlaying()) {
                                        player.sendMessage("§c[!] Replay '" + replayName + "' is not playing!");
                                        return PacketSignal.HANDLED;
                                    }

                                    if (replayData.isPaused()) {
                                        player.sendMessage("§c[!] Replay '" + replayName + "' is already paused!");
                                        return PacketSignal.HANDLED;
                                    }

                                    player.sendMessage("§e[!] Replay paused!");
                                    ReplayManager.getInstance().pauseReplay(replayName);
                                } else {
                                    player.sendMessage("§c[!] Usage: !replay pause <replayName>");
                                }
                            }
                            case "unpause", "resume" -> {
                                if (commandArgs.length > 1) {
                                    String replayName = commandArgs[1];
                                    ReplayData replayData = ReplayManager.getInstance().getReplay(replayName);

                                    if (replayData == null) {
                                        player.sendMessage("§c[!] Replay '" + replayName + "' is not playing!");
                                        return PacketSignal.HANDLED;
                                    }

                                    if (!replayData.isPlaying()) {
                                        player.sendMessage("§c[!] Replay '" + replayName + "' is not playing!");
                                        return PacketSignal.HANDLED;
                                    }

                                    if (!replayData.isPaused()) {
                                        player.sendMessage("§c[!] Replay '" + replayName + "' is already resumed!");
                                        return PacketSignal.HANDLED;
                                    }

                                    player.sendMessage("§e[!] Replay resumed!");
                                    ReplayManager.getInstance().unpauseReplay(replayName);
                                } else {
                                    player.sendMessage("§c[!] Usage: !replay resume <replayName>");
                                }
                            }
                            case "stop" -> {
                                if (commandArgs.length > 1) {
                                    String replayName = commandArgs[1];
                                    ReplayData replayData = ReplayManager.getInstance().getReplay(replayName);

                                    if (replayData == null) {
                                        player.sendMessage("§c[!] Replay '" + replayName + "' is not playing!");
                                        return PacketSignal.HANDLED;
                                    }

                                    if (!replayData.isPlaying()) {
                                        player.sendMessage("§c[!] Replay '" + replayName + "' is not playing!");
                                        return PacketSignal.HANDLED;
                                    }

                                    player.sendMessage("§e[!] Replay stopped and cleared!");
                                    ReplayManager.getInstance().stopReplay(replayName, player);
                                } else {
                                    player.sendMessage("§c[!] Usage: !replay stop <replayName>");
                                }
                            }
                            case "list" -> {
                                player.sendMessage("§e[!] Replay list:");

                                List<String> fileList = ReplayManager.getInstance().getSavedReplays();
                                StringBuilder sb = new StringBuilder();
                                for (String fileName : fileList) {
                                    sb.append("§e").append(fileName.replace(".replay", "")).append("§r, ");
                                }
                                String fileNames = sb.toString();
                                // Remove the last comma from the string
                                if (fileNames.length() > 0) {
                                    fileNames = fileNames.substring(0, fileNames.length() - 1);
                                    fileNames = fileNames.substring(0, fileNames.length() - 1);
                                }
                                player.sendMessage("§a[!] " + fileNames);
                            }
                            default -> {
                                player.sendMessage("§e[!] Commands:");
                                player.sendMessage("§a[!] !replay play <replayName> - play replay");
                                player.sendMessage("§a[!] !replay pause/resume <replayName> - pause/resume replay");
                                player.sendMessage("§a[!] !replay stop <replayName> - stop replay");
                                player.sendMessage("§a[!] !replay list - list all replays");
                                player.sendMessage("§a[!] !free - enable/disable free camera mode");
                            }
                        }
                    } else {
                        player.sendMessage("§e[!] Commands:");
                        player.sendMessage("§a[!] !replay play <replayName> - play replay");
                        player.sendMessage("§a[!] !replay pause/resume <replayName> - pause/resume replay");
                        player.sendMessage("§a[!] !replay stop <replayName> - stop replay");
                        player.sendMessage("§a[!] !replay list - list all replays");
                        player.sendMessage("§a[!] !free - enable/disable free camera mode");
                    }
                } else if (command.equals("free")) {
                    if (player.isFreeCamera()) {
                        player.setFreeCamera(false);
                        player.sendMessage("§a[!] Free camera disabled!");
                    } else {
                        player.setFreeCamera(true);
                        player.sendMessage("§a[!] Free camera enabled!");
                    }
                } else {
                    player.sendMessage("§e[!] Commands:");
                    player.sendMessage("§a[!] !replay play <replayName> - play replay");
                    player.sendMessage("§a[!] !replay pause/resume <replayName> - pause/resume replay");
                    player.sendMessage("§a[!] !replay stop <replayName> - stop replay");
                    player.sendMessage("§a[!] !replay list - list all replays");
                    player.sendMessage("§a[!] !free - enable/disable free camera mode");
                }
            }
        } else {
            player.sendMessage("§e[!] Commands:");
            player.sendMessage("§a[!] !replay play <replayName> - play replay");
            player.sendMessage("§a[!] !replay pause/resume <replayName> - pause/resume replay");
            player.sendMessage("§a[!] !replay stop <replayName> - stop replay");
            player.sendMessage("§a[!] !replay list - list all replays");
            player.sendMessage("§a[!] !free - enable/disable free camera mode");
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handlePacket(BedrockPacket packet) {
        //OutputWindow.print("IN: " + packet.toString());

        packet.handle(this);
        return PacketSignal.HANDLED;
    }
}