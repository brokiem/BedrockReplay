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
import com.brokiem.bedrockreplay.bedrock.server.ProxyServer;
import com.brokiem.bedrockreplay.output.OutputWindow;
import com.brokiem.bedrockreplay.utils.FileManager;
import com.nimbusds.jwt.SignedJWT;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.SneakyThrows;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.data.defintions.ItemDefinition;
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

        player.getSession().sendPacket(packet);
        return PacketSignal.HANDLED;
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
