package com.masson.cruciblecraft.energy.largegasturbine;

import java.util.List;

import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * GT6 {@code MultiTileEntityLargeTurbineGas.addToolTips} plus
 * {@code addToolTipsEnergy} which only prints {@code mEnergyOUT}.
 */
public final class LargeGasTurbineTooltips {
    private LargeGasTurbineTooltips() {}

    public static void append(MteInPlaceSpec spec, List<Component> tooltip) {
        if (spec == null) {
            return;
        }
        LargeGasTurbineCatalog.Profile profile =
                LargeGasTurbineCatalog.find(spec.id()).orElse(null);
        if (profile == null) {
            return;
        }
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.large_gas_turbine.structure_header")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.large_gas_turbine.structure",
                        wallName(profile))
                .withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.large_gas_turbine.controller")
                .withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.large_gas_turbine.input")
                .withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.large_gas_turbine.exhaust")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.large_gas_turbine.energy_out",
                        profile.outputRec(),
                        profile.outputMin(),
                        profile.outputMax())
                .withStyle(ChatFormatting.RED));
    }

    private static Component wallName(LargeGasTurbineCatalog.Profile profile) {
        var holder = ModBlocks.mteInPlaceBlocksById().get(profile.wallId());
        Block wall = holder != null
                ? holder.get()
                : BuiltInRegistries.BLOCK.get(profile.wallId());
        if (wall == null || wall == Blocks.AIR) {
            return Component.literal(profile.wallId().toString());
        }
        return wall.getName();
    }
}
