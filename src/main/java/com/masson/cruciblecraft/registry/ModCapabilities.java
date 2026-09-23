package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.api.fluid.LongFluidHandler;
import com.masson.cruciblecraft.content.block.LargeCrucibleWalls;
import com.masson.cruciblecraft.content.block.StainlessSteelMixerWalls;
import com.masson.cruciblecraft.content.block.AutoclaveWalls;
import com.masson.cruciblecraft.content.block.DenseLeadPorts;
import com.masson.cruciblecraft.content.block.ElectrolyzerParts;
import com.masson.cruciblecraft.content.block.GalvanizedGraaggWalls;
import com.masson.cruciblecraft.content.block.InvarOvenWalls;
import com.masson.cruciblecraft.content.block.TungstensteelCrusherWalls;
import com.masson.cruciblecraft.content.item.PortableFluidTankItem;
import com.masson.cruciblecraft.content.multiblock.PortCapabilityGate;
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
    public static final BlockCapability<LongFluidHandler, Direction>
            LONG_FLUID_HANDLER = BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath(
                            CrucibleCraft.MODID, "long_fluid_handler"),
                    LongFluidHandler.class);
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
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.LARGE_CRUCIBLE.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.LARGE_CRUCIBLE.get(),
                (blockEntity, side) -> blockEntity.inventory());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.LARGE_CRUCIBLE.get(),
                (blockEntity, side) -> blockEntity.process().fluids());
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.LARGE_BOILER.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.LARGE_BOILER.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                LONG_FLUID_HANDLER,
                ModBlockEntities.LARGE_BOILER.get(),
                (blockEntity, side) -> blockEntity.longFluids(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.TANK_3X3X3.get(),
                (blockEntity, side) -> blockEntity);
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
                ModBlockEntities.LARGE_ELECTROLYZER.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.LARGE_ELECTROLYZER.get(),
                (blockEntity, side) -> blockEntity.items(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.LARGE_ELECTROLYZER.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.LARGE_OVEN.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.LARGE_OVEN.get(),
                (blockEntity, side) -> blockEntity.items(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.LARGE_OVEN.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.LARGE_MIXER.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.LARGE_MIXER.get(),
                (blockEntity, side) -> blockEntity.items(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.LARGE_MIXER.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.LARGE_CRUSHER.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.LARGE_CRUSHER.get(),
                (blockEntity, side) -> blockEntity.items(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.LARGE_CRUSHER.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.LARGE_SHREDDER.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.LARGE_SHREDDER.get(),
                (blockEntity, side) -> blockEntity.items(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.LARGE_SHREDDER.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.LARGE_SLUICE.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.LARGE_SLUICE.get(),
                (blockEntity, side) -> blockEntity.items(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.LARGE_SLUICE.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.LARGE_BATH.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.LARGE_BATH.get(),
                (blockEntity, side) -> blockEntity.items(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.LARGE_BATH.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.LARGE_COAGULATOR.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.LARGE_COAGULATOR.get(),
                (blockEntity, side) -> blockEntity.items(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.LARGE_COAGULATOR.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.LARGE_AUTOCLAVE.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.LARGE_AUTOCLAVE.get(),
                (blockEntity, side) -> blockEntity.items(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.LARGE_AUTOCLAVE.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.LARGE_FERMENTER.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.LARGE_FERMENTER.get(),
                (blockEntity, side) -> blockEntity.items(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.LARGE_FERMENTER.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.AUTOMATIC_HAMMER.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.MULTIBLOCK_PORT.get(),
                (blockEntity, side) -> PortCapabilityGate.energyInsert(
                                blockEntity.portType())
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
                ModBlockEntities.FUSION_HULL.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.FUSION_HULL.get(),
                (blockEntity, side) -> blockEntity.fluids());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.FUSION_HULL.get(),
                (blockEntity, side) -> blockEntity.items());
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
                LONG_FLUID_HANDLER,
                ModBlockEntities.MTE_INPLACE.get(),
                (blockEntity, side) -> blockEntity.longFluidHandler(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MTE_INPLACE.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.MTE_INPLACE.get(),
                (blockEntity, side) -> {
                    if (blockEntity.forwardsGasTurbineEnergy(side)) {
                        return blockEntity;
                    }
                    if (blockEntity.forwardsSteamTurbineEnergy(side)) {
                        return blockEntity;
                    }
                    if (LargeCrucibleWalls.forwardsEnergy(blockEntity)) {
                        return blockEntity;
                    }
                    if (StainlessSteelMixerWalls.forwardsEnergy(blockEntity)) {
                        return blockEntity;
                    }
                    if (ElectrolyzerParts.forwardsEnergy(blockEntity)) {
                        return blockEntity;
                    }
                    if (InvarOvenWalls.forwardsEnergy(blockEntity)) {
                        return blockEntity;
                    }
                    if (AutoclaveWalls.forwardsEnergy(blockEntity)) {
                        return blockEntity;
                    }
                    if (TungstensteelCrusherWalls.forwardsEnergy(blockEntity)) {
                        return blockEntity;
                    }
                    if (DenseLeadPorts.forwardsEnergy(blockEntity)) {
                        return blockEntity;
                    }
                    if (GalvanizedGraaggWalls.forwardsEnergy(blockEntity)) {
                        return blockEntity;
                    }
                    if (blockEntity.forwardsLargeDynamoEnergy(side)) {
                        return blockEntity;
                    }
                    return blockEntity.handles(
                            blockEntity.spec().kind().energyType(), side)
                            ? blockEntity
                            : null;
                });
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
                ModBlockEntities.MIXING_BOWL.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.MIXING_BOWL.get(),
                (blockEntity, side) -> blockEntity.fluidHandler(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.COKE_OVEN.get(),
                (blockEntity, side) -> blockEntity.automationItems(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.COKE_OVEN.get(),
                (blockEntity, side) -> blockEntity.automationFluids(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.FIREBRICK.get(),
                (blockEntity, side) -> blockEntity.items(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.FIREBRICK.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.CRUCIBLE.get(),
                (blockEntity, side) -> blockEntity.externalFluids());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CRUCIBLE.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CERAMIC_MOLD.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.CERAMIC_MOLD.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.FOUNDRY_CASTING.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.FOUNDRY_CASTING.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.COINAGE_MOLD.get(),
                (blockEntity, side) -> blockEntity.itemHandler());
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
                ModBlockEntities.COOLER.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.COOLER.get(),
                (blockEntity, side) -> blockEntity.fluxStorage(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.FLUX_CONVERTER.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.FLUX_CONVERTER.get(),
                (blockEntity, side) -> blockEntity.fluxStorage(side));
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
                ModBlockEntities.LARGE_GAS_TURBINE.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.LARGE_GAS_TURBINE.get(),
                (blockEntity, side) -> blockEntity.fluids(side));
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.LARGE_DYNAMO.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.LIGHTNING_ROD.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.VON_DA_GRAAGG.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.MATTER_FABRICATOR.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MATTER_FABRICATOR.get(),
                (blockEntity, side) -> blockEntity.items(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.MATTER_FABRICATOR.get(),
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
