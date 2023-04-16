/*
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.network.handler.upstream;

import com.alibaba.fastjson.JSONObject;
import com.brokiem.bedrockreplay.bedrock.player.ProxiedPlayer;
import com.brokiem.bedrockreplay.bedrock.server.ProxyServer;
import com.brokiem.bedrockreplay.output.OutputWindow;
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
