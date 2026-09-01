package com.masson.cruciblecraft.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration for empty-input item scatter: one item entity per {@code rarity}
 * surface positions, drawn from the {@code item_tag} item tag.
 */
public record ItemScatterConfiguration(
        int rarity,
        TagKey<Item> itemTag) implements FeatureConfiguration {
    public static final Codec<ItemScatterConfiguration> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                            Codec.INT.fieldOf("rarity")
                                    .forGetter(ItemScatterConfiguration::rarity),
                            ResourceLocation.CODEC
                                    .xmap(
                                            location -> TagKey.create(
                                                    Registries.ITEM, location),
                                            TagKey::location)
                                    .fieldOf("item_tag")
                                    .forGetter(ItemScatterConfiguration::itemTag))
                    .apply(instance, ItemScatterConfiguration::new));
}
