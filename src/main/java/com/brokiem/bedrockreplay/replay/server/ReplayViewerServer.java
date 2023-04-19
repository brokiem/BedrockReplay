/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.replay.server;

import com.brokiem.bedrockreplay.bedrock.data.BedrockData;
import com.brokiem.bedrockreplay.bedrock.network.registry.FakeDefinitionRegistry;
import com.brokiem.bedrockreplay.replay.server.handler.PacketHandler;
import com.brokiem.bedrockreplay.replay.server.player.ViewerPlayer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;
import lombok.SneakyThrows;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.protocol.bedrock.BedrockPong;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.v575.Bedrock_v575;
import org.cloudburstmc.protocol.bedrock.data.*;
import org.cloudburstmc.protocol.bedrock.data.defintions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockServerInitializer;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.common.SimpleDefinitionRegistry;
import org.cloudburstmc.protocol.common.util.OptionalBoolean;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.UUID;

public class ReplayViewerServer {

    @Getter
    private static ReplayViewerServer instance;

    @Getter
    private final InetSocketAddress address;

    public static final BedrockCodec BEDROCK_CODEC = Bedrock_v575.CODEC;
    public static final BedrockCodecHelper CODEC_HELPER = BEDROCK_CODEC.createHelper();

    private boolean isRunning = false;

    public ReplayViewerServer(InetSocketAddress address) {
        this.address = address;
        instance = this;
    }

    public void start() {
        if (isRunning) {
            return;
        }

        SimpleDefinitionRegistry.Builder<ItemDefinition> itemRegistry = SimpleDefinitionRegistry.builder();
        IntSet runtimeIds = new IntOpenHashSet();
        for (ItemDefinition definition : BedrockData.ITEM_DEFINITIONS) {
            if (runtimeIds.add(definition.getRuntimeId())) {
                itemRegistry.add(definition);
            }
        }
        CODEC_HELPER.setItemDefinitions(itemRegistry.build());
        CODEC_HELPER.setBlockDefinitions(FakeDefinitionRegistry.createBlockRegistry());

        BedrockPong pong = new BedrockPong()
                .edition("MCPE")
                .motd("BedrockReplay")
                .subMotd("Viewer Server")
                .playerCount(0)
                .maximumPlayerCount(1)
                .gameType("Survival")
                .protocolVersion(BEDROCK_CODEC.getProtocolVersion())
                .ipv4Port(address.getPort())
                .ipv6Port(address.getPort())
                .version(BEDROCK_CODEC.getMinecraftVersion());

        new ServerBootstrap()
                .channelFactory(RakChannelFactory.server(NioDatagramChannel.class))
                .option(RakChannelOption.RAK_ADVERTISEMENT, pong.toByteBuf())
                .group(new NioEventLoopGroup())
                .childHandler(new BedrockServerInitializer() {
                    @Override
                    protected void initSession(BedrockServerSession session) {
                        session.setCodec(BEDROCK_CODEC);
                        session.getPeer().getCodecHelper().setItemDefinitions(CODEC_HELPER.getItemDefinitions());
                        session.getPeer().getCodecHelper().setBlockDefinitions(CODEC_HELPER.getBlockDefinitions());

                        ViewerPlayer player = new ViewerPlayer(session);
                        session.setPacketHandler(new PacketHandler(player));
                    }
                })
                .bind(address)
                .syncUninterruptibly();

        isRunning = true;
    }

    public static void spawnPlayer(ViewerPlayer player) {
        BiomeDefinitionListPacket biomeDefinitionListPacket = new BiomeDefinitionListPacket();
        biomeDefinitionListPacket.setDefinitions(BedrockData.BIOME_DEFINITIONS);
        player.getSession().sendPacket(biomeDefinitionListPacket);

        AvailableEntityIdentifiersPacket entityPacket = new AvailableEntityIdentifiersPacket();
        entityPacket.setIdentifiers(BedrockData.ENTITY_IDENTIFIERS);
        player.getSession().sendPacket(entityPacket);

        CreativeContentPacket packet = new CreativeContentPacket();
        packet.setContents(new ItemData[0]);
        player.getSession().sendPacket(packet);

        PlayStatusPacket playStatusPacket = new PlayStatusPacket();
        playStatusPacket.setStatus(PlayStatusPacket.Status.PLAYER_SPAWN);
        player.getSession().sendPacket(playStatusPacket);

        UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
        attributesPacket.setRuntimeEntityId(player.getEntityRuntimeId());
        attributesPacket.setAttributes(Collections.singletonList(new AttributeData("minecraft:movement", 0.0f, 1024f, 0.1f, 0.1f)));
        player.getSession().sendPacket(attributesPacket);

        GameRulesChangedPacket gamerulePacket = new GameRulesChangedPacket();
        gamerulePacket.getGameRules().add(new GameRuleData<>("naturalregeneration", false));
        player.getSession().sendPacket(gamerulePacket);
    }

    public static void sendStartGame(ViewerPlayer player) {
        StartGamePacket startGamePacket = new StartGamePacket();
        startGamePacket.setUniqueEntityId(player.getEntityRuntimeId());
        startGamePacket.setRuntimeEntityId(player.getEntityRuntimeId());
        startGamePacket.setPlayerGameType(GameType.CREATIVE);
        startGamePacket.setPlayerPosition(Vector3f.from(0, 65, 0));
        startGamePacket.setRotation(Vector2f.from(1, 1));

        startGamePacket.setSeed(-1);
        startGamePacket.setDimensionId(0);
        startGamePacket.setGeneratorId(1);
        startGamePacket.setLevelGameType(GameType.CREATIVE);
        startGamePacket.setDifficulty(2);
        startGamePacket.setDefaultSpawn(Vector3i.from(0, 65, 0));
        startGamePacket.setAchievementsDisabled(true);
        startGamePacket.setDayCycleStopTime(-1);
        startGamePacket.setEduEditionOffers(0);
        startGamePacket.setEduFeaturesEnabled(false);
        startGamePacket.setEducationProductionId("");
        startGamePacket.setRainLevel(0);
        startGamePacket.setLightningLevel(0);
        startGamePacket.setPlatformLockedContentConfirmed(false);
        startGamePacket.setMultiplayerGame(true);
        startGamePacket.setBroadcastingToLan(true);
        startGamePacket.setXblBroadcastMode(GamePublishSetting.PUBLIC);
        startGamePacket.setPlatformBroadcastMode(GamePublishSetting.PUBLIC);
        startGamePacket.setCommandsEnabled(true);
        startGamePacket.setTexturePacksRequired(true);
        startGamePacket.setExperimentsPreviouslyToggled(false);
        startGamePacket.setBonusChestEnabled(false);
        startGamePacket.setStartingWithMap(false);
        startGamePacket.setTrustingPlayers(false);
        startGamePacket.setDefaultPlayerPermission(PlayerPermission.MEMBER);
        startGamePacket.setServerChunkTickRange(4);
        startGamePacket.setBehaviorPackLocked(false);
        startGamePacket.setResourcePackLocked(false);
        startGamePacket.setFromLockedWorldTemplate(false);
        startGamePacket.setUsingMsaGamertagsOnly(false);
        startGamePacket.setFromWorldTemplate(false);
        startGamePacket.setWorldTemplateOptionLocked(false);
        startGamePacket.setOnlySpawningV1Villagers(false);
        startGamePacket.setVanillaVersion(ReplayViewerServer.BEDROCK_CODEC.getMinecraftVersion());
        startGamePacket.setLimitedWorldWidth(0);
        startGamePacket.setLimitedWorldHeight(0);
        startGamePacket.setNetherType(true);
        startGamePacket.setEduSharedUriResource(EduSharedUriResource.EMPTY);
        startGamePacket.setForceExperimentalGameplay(OptionalBoolean.empty());
        startGamePacket.setChatRestrictionLevel(ChatRestrictionLevel.NONE);
        startGamePacket.setDisablingPlayerInteractions(false);
        startGamePacket.setDisablingPersonas(false);
        startGamePacket.setDisablingCustomSkins(false);
        startGamePacket.setLevelId("");
        startGamePacket.setLevelName("§eBedrockReplay Viewer Server");
        startGamePacket.setPremiumWorldTemplateId("");
        startGamePacket.setTrial(false);
        startGamePacket.setAuthoritativeMovementMode(AuthoritativeMovementMode.CLIENT);
        startGamePacket.setRewindHistorySize(0);
        startGamePacket.setServerAuthoritativeBlockBreaking(false);
        startGamePacket.setCurrentTick(0);
        startGamePacket.setEnchantmentSeed(0);
        startGamePacket.setMultiplayerCorrelationId("");
        startGamePacket.setServerEngine("");
        startGamePacket.setSpawnBiomeType(SpawnBiomeType.DEFAULT);
        startGamePacket.setCustomBiomeName("");
        startGamePacket.setPlayerPropertyData(NbtMap.EMPTY);
        startGamePacket.setWorldTemplateId(new UUID(0, 0));

        player.getSession().sendPacket(startGamePacket);
    }

    public static void sendEmptyChunks(ViewerPlayer player) {
        Vector3i position = Vector3i.from(0, 60, 0);
        int radius = 0;
        int chunkX = position.getX() >> 4;
        int chunkZ = position.getZ() >> 4;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                LevelChunkPacket data = new LevelChunkPacket();
                data.setChunkX(chunkX + x);
                data.setChunkZ(chunkZ + z);
                data.setSubChunksLength(0);
                data.setData(getEmptyChunkData());
                data.setCachingEnabled(false);
                player.getSession().sendPacket(data);
            }
        }
    }

    @SneakyThrows
    private static ByteBuf getEmptyChunkData() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(new byte[258]);

        try (NBTOutputStream stream = NbtUtils.createNetworkWriter(outputStream)) {
            stream.writeTag(NbtMap.EMPTY);
        }

        return Unpooled.copiedBuffer(outputStream.toByteArray());
    }
}