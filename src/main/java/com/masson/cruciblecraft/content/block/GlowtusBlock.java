package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.worldgen.crop.GlowtusColor;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * GT6 {@code BlockGlowtus}: lily pad, light 15, one block per dye meta.
 * Not {@code lilypad_glowtus/white_glowtus}.
 */
public final class GlowtusBlock extends WaterlilyBlock {
    private final GlowtusColor color;

    public GlowtusBlock(GlowtusColor color) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .instabreak()
                .sound(SoundType.LILY_PAD)
                .noOcclusion()
                .lightLevel(state -> 15)
                .pushReaction(PushReaction.DESTROY));
        this.color = color;
    }

    public GlowtusColor color() {
        return color;
    }
}
