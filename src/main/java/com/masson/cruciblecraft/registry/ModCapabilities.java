package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.item.PortableFluidTankItem;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.content.storage.ILogisticsStorage;

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
    public static final BlockCapability<ILogisticsStorage, Direction> LOGISTICS_STORAGE =
            BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath(
                            CrucibleCraft.MODID, "logistics_storage"),
                    ILogisticsStorage.class);
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
        event.registerBlockEntity(ENERGY, ModBlockEntities.CRUCIBLE.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(ENERGY, ModBlockEntities.BOILER.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(ENERGY, ModBlockEntities.STEAM_ENGINE.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(ENERGY, ModBlockEntities.DYNAMO.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.ELECTRIC_MOTOR.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.ELECTRIC_HEATER.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.ELECTRIC_ENGINE.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.ROTATIONAL_AXLE.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.ROTATIONAL_GEARBOX.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.FUEL_GENERATOR.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.LOGISTICS_CORE.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.LOGISTICS_CORE_WALL.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(ENERGY, ModBlockEntities.CRUSHER.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY, ModBlockEntities.PROCESSING_MACHINE.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.MULTIBLOCK_PORT.get(),
                (blockEntity, side) -> blockEntity.portType()
                                == PortType.ENERGY_INPUT
                        ? blockEntity
                        : null);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.CABLE.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.LASER_ENGRAVER.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.LASER_ENGRAVER.get(),
                (blockEntity, side) -> blockEntity.items(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.LASER_ENGRAVER.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.FUSION_REACTOR.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.FUSION_REACTOR.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.FUSION_REACTOR.get(),
                (blockEntity, side) -> blockEntity.items(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.QUANTUM_ENERGIZER.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.LONG_DISTANCE_TRANSFORMER.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.REACTOR_CORE.get(),
                (blockEntity, side) -> blockEntity.items());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.REACTOR_CORE.get(),
                (blockEntity, side) -> blockEntity.fluids());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.FLUID_PIPE.get(),
                (blockEntity, side) -> blockEntity.fluidHandler(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.MTE_INPLACE.get(),
                (blockEntity, side) -> blockEntity.fluidHandler(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MTE_INPLACE.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.MTE_INPLACE.get(),
                (blockEntity, side) -> blockEntity.handles(
                        blockEntity.spec().kind().energyType(), side)
                        ? blockEntity
                        : null);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ITEM_PIPE.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.HOPPER.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.BOOKSHELF.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.BOTTLE_CRATE.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.DRAWER.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.LOCKER.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.LOCKER.get(),
                (blockEntity, side) -> blockEntity.handles(
                        com.masson.cruciblecraft.api.energy.EnergyType.ELECTRIC, side)
                        ? blockEntity
                        : null);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MASS_STORAGE.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
        event.registerBlockEntity(
                LOGISTICS_STORAGE,
                ModBlockEntities.MASS_STORAGE.get(),
                (blockEntity, side) -> blockEntity.logisticsStorage());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.DUST_FUNNEL.get(),
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
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MULTIBLOCK_PORT.get(),
                (blockEntity, side) -> blockEntity.itemHandler());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.MULTIBLOCK_PORT.get(),
                (blockEntity, side) -> blockEntity.fluidHandler());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.FLUID_DEPOSIT_EXTRACTOR.get(),
                (blockEntity, side) -> blockEntity.externalFluid());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.FUEL_GENERATOR.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.SOLID_BURNING_BOX.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.SOLID_BURNING_BOX.get(),
                (blockEntity, side) -> blockEntity.items(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.FLUID_BED_BURNING_BOX.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.BATTERY.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.TRANSFORMER.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.HEAT_EXCHANGER.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.HEAT_EXCHANGER.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.LARGE_HEAT_EXCHANGER.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.LARGE_HEAT_EXCHANGER.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.BEDROCK_DRILL.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.BEDROCK_DRILL.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.BEDROCK_DRILL.get(),
                (blockEntity, side) -> blockEntity.items(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.FLUID_BED_BURNING_BOX.get(),
                (blockEntity, side) -> blockEntity.items(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.FLUID_BED_BURNING_BOX.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.TREE_HOLE.get(),
                (blockEntity, side) -> blockEntity.fluids());
    }
}
