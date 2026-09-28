package com.masson.cruciblecraft.recipe.gt;

import java.util.Objects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * One original compact-family holder carried inside a release-only bundle.
 *
 * <p>{@link #sourceId()} is the original datapack recipe id. It is deliberately
 * not the physical bundle holder id because deduplication, transport fragment
 * reassembly, and diagnostics use the authored source id.
 */
public record CompactGTRecipeFamilyBundleEntry(
        ResourceLocation sourceId,
        CompactGTRecipeFamilyDefinition definition) {
    public static final MapCodec<CompactGTRecipeFamilyBundleEntry> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("source_id")
                            .forGetter(CompactGTRecipeFamilyBundleEntry::sourceId),
                    CompactGTRecipeFamilyDefinition.MAP_CODEC
                            .fieldOf("definition")
                            .forGetter(CompactGTRecipeFamilyBundleEntry::definition)
            ).apply(instance, CompactGTRecipeFamilyBundleEntry::new));

    public CompactGTRecipeFamilyBundleEntry {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(definition, "definition");
    }
}
