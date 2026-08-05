package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.item.PortableFluidTankItem;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

public final class ModCapabilities {
    public static final BlockCapability<IEnergyHandler, Direction> ENERGY =
            BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, "energy"),
                    IEnergyHandler.class);
    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> new FluidHandlerItemStack(
                        ModComponents.PORTABLE_FLUID,
                        stack,
                        PortableFluidTankItem.CAPACITY),
                ModItems.PORTABLE_FLUID_TANK.get());
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> ModItems.FLUID_CELL.get().handler(stack),
                ModItems.FLUID_CELL.get());
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> ModItems.GAS_CELL.get().handler(stack),
                ModItems.GAS_CELL.get());
        event.registerBlockEntity(ENERGY, ModBlockEntities.FIREBOX.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(ENERGY, ModBlockEntities.BELLOWS.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(ENERGY, ModBlockEntities.CRUCIBLE.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(ENERGY, ModBlockEntities.BOILER.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(ENERGY, ModBlockEntities.STEAM_ENGINE.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(ENERGY, ModBlockEntities.DYNAMO.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(ENERGY, ModBlockEntities.CRUSHER.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY, ModBlockEntities.PROCESSING_MACHINE.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.CABLE.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.FLUID_PIPE.get(),
                (blockEntity, side) -> blockEntity.fluidHandler(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ITEM_PIPE.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
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
