/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.player.cache;

import com.brokiem.bedrockreplay.bedrock.player.cache.block.UpdateBlockCache;
import com.brokiem.bedrockreplay.bedrock.player.cache.chunk.LevelChunkCache;
import com.brokiem.bedrockreplay.bedrock.player.cache.chunk.NetworkChunkPublisherCache;
import com.brokiem.bedrockreplay.bedrock.player.cache.chunk.SubChunkCache;
import com.brokiem.bedrockreplay.bedrock.player.cache.entity.*;
import com.brokiem.bedrockreplay.bedrock.player.cache.inventory.InventoryContentCache;
import com.brokiem.bedrockreplay.bedrock.player.cache.inventory.InventorySlotCache;
import com.brokiem.bedrockreplay.bedrock.player.cache.inventory.MobArmorEquipmentCache;
import com.brokiem.bedrockreplay.bedrock.player.cache.inventory.MobEquipmentCache;
import com.brokiem.bedrockreplay.bedrock.player.cache.score.SetDisplayObjectiveCache;
import com.brokiem.bedrockreplay.bedrock.player.cache.score.SetScoreCache;
import lombok.Getter;

public class PlayerPacketCache {

    @Getter
    private final LevelChunkCache levelChunkCache = new LevelChunkCache();
    @Getter
    private final AddPlayerCache addPlayerCache = new AddPlayerCache();
    @Getter
    private final SubChunkCache subChunkCache = new SubChunkCache();
    @Getter
    private final NetworkChunkPublisherCache chunkPublisherCache = new NetworkChunkPublisherCache();
    @Getter
    private final AddItemEntityCache addItemEntityCache = new AddItemEntityCache();
    @Getter
    private final AddEntityCache addEntityCache = new AddEntityCache();
    @Getter
    private final UpdateBlockCache updateBlockCache = new UpdateBlockCache();
    @Getter
    private final InventoryContentCache inventoryContentCache = new InventoryContentCache();
    @Getter
    private final InventorySlotCache inventorySlotCache = new InventorySlotCache();
    @Getter
    private final MobEquipmentCache mobEquipmentCache = new MobEquipmentCache();
    @Getter
    private final MobArmorEquipmentCache mobArmorEquipmentCache = new MobArmorEquipmentCache();
    @Getter
    private final SetScoreCache setScoreCache = new SetScoreCache();
    @Getter
    private final SetDisplayObjectiveCache setDisplayObjectiveCache = new SetDisplayObjectiveCache();
    @Getter
    private final PlayerSkinCache playerSkinCache = new PlayerSkinCache();
    @Getter
    private final SetPlayerGameTypeCache setPlayerGameTypeCache = new SetPlayerGameTypeCache();
    @Getter
    private final PlayerListEntryCache playerListEntryCache = new PlayerListEntryCache();
    @Getter
    private final SetEntityDataCache setEntityDataCache = new SetEntityDataCache();
    @Getter
    private final BossEventCache bossEventCache = new BossEventCache();

    public PlayerPacketCache() { }
}