/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.replay;

import com.brokiem.bedrockreplay.bedrock.player.cache.PlayerPacketCache;
import com.brokiem.bedrockreplay.replay.server.ReplayViewerServer;
import com.brokiem.bedrockreplay.utils.Callback;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.cloudburstmc.protocol.bedrock.codec.BedrockPacketDefinition;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.*;

public class ReplayData {

    @Getter
    private String replayId;
    @Getter
    private int tick = 0;
    @Setter
    @Getter
    private long recorderEntityRuntimeId = -1;
    private final Queue<BedrockPacket> packets = new ConcurrentLinkedQueue<>();
    private final Map<Integer, List<BedrockPacket>> packetsByTick = new HashMap<>();

    private ScheduledExecutorService scheduler;

    @Getter
    private boolean isRecording = false;
    @Getter
    @Setter
    private boolean isPlaying = false;
    @Getter
    @Setter
    private boolean isPaused = false;

    public ReplayData(String replayId) {
        this.replayId = replayId;
    }

    public void startRecording() {
        if (this.isRecording) {
            return;
        }

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduler.scheduleAtFixedRate(() -> {
            this.packetsByTick.put(this.tick, new ArrayList<>(this.packets));
            this.packets.clear();
            ++this.tick;
        }, 0, 50, TimeUnit.MILLISECONDS);
        this.isRecording = true;
    }

    public void addPacket(BedrockPacket packet) {
        this.packets.add(packet);
    }

    public void stopRecording() {
        if (!this.isRecording) {
            return;
        }

        this.isRecording = false;

        this.scheduler.shutdown();
        this.tick = 0;

        List<Integer> ticks = new ArrayList<>(this.packetsByTick.keySet());
        for (Integer tick : ticks) {
            if (this.packetsByTick.get(tick).isEmpty()) {
                this.packetsByTick.remove(tick);
            }
        }
    }

    @SneakyThrows
    public void saveToFile(String path, Callback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                try (FileOutputStream fos = new FileOutputStream(path + ".replay");
                     DataOutputStream dos = new DataOutputStream(new XZOutputStream(fos, new LZMA2Options(LZMA2Options.PRESET_MAX)))) {

                    ByteBuf packetBuffersByTick = Unpooled.buffer();

                    for (Map.Entry<Integer, List<BedrockPacket>> entry : this.packetsByTick.entrySet()) {
                        int tick = entry.getKey();
                        List<BedrockPacket> packets = entry.getValue();
                        ByteBuf serializedPackets = Unpooled.buffer();

                        for (BedrockPacket packet : packets) {
                            try {
                                ByteBuf buf = Unpooled.buffer();
                                ReplayViewerServer.BEDROCK_CODEC.tryEncode(ReplayViewerServer.CODEC_HELPER, buf, packet);
                                BedrockPacketDefinition<BedrockPacket> definition = ReplayViewerServer.BEDROCK_CODEC.getPacketDefinition((Class<BedrockPacket>) packet.getClass());

                                serializedPackets.writeShortLE(definition.getId());
                                serializedPackets.writeIntLE(buf.readableBytes());
                                serializedPackets.writeBytes(buf);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        packetBuffersByTick.writeIntLE(tick);
                        packetBuffersByTick.writeIntLE(serializedPackets.readableBytes());
                        packetBuffersByTick.writeBytes(serializedPackets);
                    }

                    dos.writeLong(this.recorderEntityRuntimeId);
                    dos.writeInt(packetBuffersByTick.readableBytes());
                    dos.write(packetBuffersByTick.array(), packetBuffersByTick.readerIndex(), packetBuffersByTick.readableBytes());
                }
                callback.onSuccess();
            } catch (Exception e) {
                callback.onFailure("Failed to save replay: " + e.getMessage());
            }
            executor.shutdown();
        });
    }

    @SneakyThrows
    public void loadFromFile(String path, Callback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                try (FileInputStream fis = new FileInputStream(path + ".replay");
                     DataInputStream dis = new DataInputStream(new XZInputStream(fis))) {

                    this.recorderEntityRuntimeId = dis.readLong();
                    int packetBuffersByTickLength = dis.readInt();
                    byte[] packetBuffersByTickBytes = new byte[packetBuffersByTickLength];
                    dis.read(packetBuffersByTickBytes);

                    ByteBuf packetBuffersByTick = Unpooled.wrappedBuffer(packetBuffersByTickBytes);

                    while (packetBuffersByTick.isReadable()) {
                        int tick = packetBuffersByTick.readIntLE();
                        int serializedPacketsLength = packetBuffersByTick.readIntLE();

                        ByteBuf serializedPackets = packetBuffersByTick.readRetainedSlice(serializedPacketsLength);
                        List<BedrockPacket> packets = new ArrayList<>();

                        while (serializedPackets.isReadable()) {
                            int packetId = serializedPackets.readShortLE();
                            int packetLength = serializedPackets.readIntLE();

                            ByteBuf packetBuffer = serializedPackets.readRetainedSlice(packetLength);

                            try {
                                BedrockPacket packet = ReplayViewerServer.BEDROCK_CODEC.tryDecode(ReplayViewerServer.CODEC_HELPER, packetBuffer, packetId);
                                packets.add(packet);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        this.packetsByTick.put(tick, packets);
                    }
                }
                callback.onSuccess();
            } catch (Exception e) {
                callback.onFailure("Failed to load replay: " + e.getMessage());
            }
            executor.shutdown();
        });
    }

    public List<BedrockPacket> getPacketsForTick(int tick) {
        return this.packetsByTick.getOrDefault(tick, new ArrayList<>());
    }

    public Map<Integer, List<BedrockPacket>> getRecordedPackets() {
        return this.packetsByTick;
    }

    public boolean containsTick(int tick) {
        return this.packetsByTick.containsKey(tick);
    }

    public void removeTick(int tick) {
        this.packetsByTick.remove(tick);
    }
    
    public void addPacketCache(PlayerPacketCache packetCache) {
        packetCache.getChunkPublisherCache().getPackets().forEach(this::addPacket);
        packetCache.getLevelChunkCache().getPackets().values().forEach(this::addPacket);
        packetCache.getSubChunkCache().getPackets().forEach(this::addPacket);
        packetCache.getUpdateBlockCache().getPackets().values().forEach(this::addPacket);
        packetCache.getAddPlayerCache().getPackets().values().forEach(this::addPacket);
        packetCache.getAddEntityCache().getPackets().values().forEach(this::addPacket);
        packetCache.getAddItemEntityCache().getPackets().values().forEach(this::addPacket);
        packetCache.getInventoryContentCache().getPackets().forEach(this::addPacket);
        packetCache.getInventorySlotCache().getPackets().forEach(this::addPacket);
        packetCache.getMobEquipmentCache().getPackets().values().forEach(this::addPacket);
        packetCache.getMobArmorEquipmentCache().getPackets().values().forEach(this::addPacket);
        this.addPacket(packetCache.getSetScoreCache().getPacket());
        packetCache.getSetDisplayObjectiveCache().getPackets().values().forEach(this::addPacket);
        packetCache.getPlayerSkinCache().getPackets().values().forEach(this::addPacket);
        this.addPacket(packetCache.getSetPlayerGameTypeCache().getPacket());
        packetCache.getPlayerListEntryCache().getEntries().values().stream()
                .map(entry -> {
                    PlayerListPacket playerListPacket = new PlayerListPacket();
                    playerListPacket.setAction(PlayerListPacket.Action.ADD);
                    playerListPacket.getEntries().add(entry);
                    return playerListPacket;
                })
                .forEach(this::addPacket);
        packetCache.getSetEntityDataCache().getPackets().values().forEach(this::addPacket);
        packetCache.getBossEventCache().getPackets().values().forEach(this::addPacket);
    }
}
