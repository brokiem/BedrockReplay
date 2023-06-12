/**
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.bedrock.data;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.brokiem.bedrockreplay.utils.FileManager;
import lombok.SneakyThrows;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

public class BedrockData {

    public static NbtMap BIOME_DEFINITIONS;
    public static NbtMap ENTITY_IDENTIFIERS;
    public static ArrayList<ItemDefinition> ITEM_DEFINITIONS = new ArrayList<>();

    @SneakyThrows
    public static void loadBiomeDefinitions() {
        InputStream stream = FileManager.getFileResourceAsInputStream("bedrock-data/biome_definitions.nbt");

        NbtMap biomesTag;
        assert stream != null;
        try (NBTInputStream biomenbtInputStream = NbtUtils.createNetworkReader(stream)) {
            biomesTag = (NbtMap) biomenbtInputStream.readTag();
            BIOME_DEFINITIONS = biomesTag;
        }
    }

    public static void loadCreativeItems() {
    }

    @SneakyThrows
    public static void loadEntityIdentifiers() {
        InputStream stream = FileManager.getFileResourceAsInputStream("bedrock-data/entity_identifiers.nbt");

        assert stream != null;
        try (NBTInputStream nbtInputStream = NbtUtils.createNetworkReader(stream)) {
            ENTITY_IDENTIFIERS = (NbtMap) nbtInputStream.readTag();
        }
    }

    public static void loadItemEntries() {
        String data = FileManager.getFileResourceAsString("bedrock-data/required_item_list.json");

        Map<String, Map<String, Object>> itemEntries = JSON.parseObject(data, new TypeReference<>() {});
        assert itemEntries != null;
        itemEntries.forEach((itemName, val) -> {
            int id = (int) val.get("runtime_id");
            boolean componentBased = (boolean) val.get("component_based");
            ITEM_DEFINITIONS.add(new SimpleItemDefinition(itemName, id, componentBased));
        });
    }
}