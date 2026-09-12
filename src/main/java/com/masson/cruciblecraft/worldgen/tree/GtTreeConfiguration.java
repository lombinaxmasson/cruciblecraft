package com.masson.cruciblecraft.worldgen.tree;

import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record GtTreeConfiguration(GtTreeSpecies species) implements FeatureConfiguration {
    public static final Codec<GtTreeConfiguration> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                            Codec.STRING.fieldOf("species")
                                    .forGetter(config -> config.species().id()))
                    .apply(instance, GtTreeConfiguration::fromId));

    public static GtTreeConfiguration fromId(String id) {
        for (GtTreeSpecies species : GtTreeSpecies.ALL) {
            if (species.id().equals(id)) {
                return new GtTreeConfiguration(species);
            }
        }
        throw new IllegalArgumentException("Unknown GT tree species " + id);
    }
}
