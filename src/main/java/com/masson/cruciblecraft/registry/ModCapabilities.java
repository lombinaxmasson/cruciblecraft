package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities {
    public static final BlockCapability<IEnergyHandler, Direction> ENERGY =
            BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, "energy"),
                    IEnergyHandler.class);
    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ENERGY, ModBlockEntities.FIREBOX.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(ENERGY, ModBlockEntities.BELLOWS.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(ENERGY, ModBlockEntities.CRUCIBLE.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(ENERGY, ModBlockEntities.BOILER.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(ENERGY, ModBlockEntities.STEAM_ENGINE.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(ENERGY, ModBlockEntities.CRUSHER.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY, ModBlockEntities.PROCESSING_MACHINE.get(),
                (blockEntity, side) -> blockEntity);
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
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CRUSHER.get(),
                (blockEntity, side) -> blockEntity.items(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.PROCESSING_MACHINE.get(),
                (blockEntity, side) -> blockEntity.items(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.PROCESSING_MACHINE.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
    }
}
