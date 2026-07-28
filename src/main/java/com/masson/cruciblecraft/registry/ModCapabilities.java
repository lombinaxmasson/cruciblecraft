package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.air.IAirSource;
import com.masson.cruciblecraft.api.heat.IHeatSource;
import com.masson.cruciblecraft.api.kinetic.IKineticSource;
import com.masson.cruciblecraft.content.block.BellowsBlock;
import com.masson.cruciblecraft.content.block.SteamEngineBlock;
import com.masson.cruciblecraft.steam.MachineSideRules;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities {
    public static final BlockCapability<IAirSource, Direction> AIR_SOURCE =
            BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, "air_source"),
                    IAirSource.class);
    public static final BlockCapability<IHeatSource, Direction> HEAT_SOURCE =
            BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, "heat_source"),
                    IHeatSource.class);
    public static final BlockCapability<IKineticSource, Direction> KINETIC_SOURCE =
            BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, "kinetic_source"),
                    IKineticSource.class);

    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                HEAT_SOURCE,
                ModBlockEntities.FIREBOX.get(),
                (blockEntity, side) -> side == Direction.UP ? blockEntity : null);
        event.registerBlockEntity(
                AIR_SOURCE,
                ModBlockEntities.BELLOWS.get(),
                (blockEntity, side) ->
                        side == blockEntity.getBlockState().getValue(BellowsBlock.FACING)
                                ? blockEntity
                                : null);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.COKE_OVEN.get(),
                (blockEntity, side) -> blockEntity.externalItems());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.COKE_OVEN.get(),
                (blockEntity, side) -> blockEntity.externalFluids());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.CRUCIBLE.get(),
                (blockEntity, side) -> blockEntity.externalFluids());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.BOILER.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.STEAM_ENGINE.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                KINETIC_SOURCE,
                ModBlockEntities.STEAM_ENGINE.get(),
                (blockEntity, side) -> MachineSideRules.engineExposesKinetic(
                        blockEntity.getBlockState().getValue(SteamEngineBlock.FACING), side)
                                ? blockEntity : null);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CRUSHER.get(),
                (blockEntity, side) -> blockEntity.items(side));
    }
}
