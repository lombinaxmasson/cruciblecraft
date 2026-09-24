package com.masson.cruciblecraft.compat.jade;

import java.util.Locale;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.block.AnvilBlock;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.CokeOvenBlock;
import com.masson.cruciblecraft.content.block.CeramicMoldBlock;
import com.masson.cruciblecraft.content.block.GtSurfaceRockBlock;
import com.masson.cruciblecraft.content.block.RockBlock;
import com.masson.cruciblecraft.content.block.SteamEngineBlock;
import com.masson.cruciblecraft.content.item.MaterialFormItem;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.content.block.BoilerBlock;
import com.masson.cruciblecraft.content.block.CrusherBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.compat.jade.observation.BatteryObservation;
import com.masson.cruciblecraft.compat.jade.observation.ConverterObservation;
import com.masson.cruciblecraft.compat.jade.observation.CrucibleObservation;
import com.masson.cruciblecraft.compat.jade.observation.FluidPipeObservation;
import com.masson.cruciblecraft.compat.jade.observation.JadeDisplayUnits;
import com.masson.cruciblecraft.compat.jade.observation.LargeBoilerObservation;
import com.masson.cruciblecraft.compat.jade.observation.ObservationField;
import com.masson.cruciblecraft.compat.jade.observation.ReactorCoreObservation;
import com.masson.cruciblecraft.compat.jade.observation.SourceWailaRows;
import com.masson.cruciblecraft.compat.jade.observation.SteamEngineObservation;
import com.masson.cruciblecraft.compat.jade.observation.TransformerObservation;
import com.masson.cruciblecraft.content.block.DynamoBlock;
import com.masson.cruciblecraft.content.block.ElectricEngineBlock;
import com.masson.cruciblecraft.content.block.ElectricHeaterBlock;
import com.masson.cruciblecraft.content.block.ElectricMotorBlock;
import com.masson.cruciblecraft.content.block.FluidBedBurningBoxBlock;
import com.masson.cruciblecraft.content.block.FluidSpringBlock;
import com.masson.cruciblecraft.content.block.FuelGeneratorBlock;
import com.masson.cruciblecraft.content.block.GtSmallOreBlock;
import com.masson.cruciblecraft.content.block.ReactorCoreBlock;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.content.block.SolidBurningBoxBlock;
import com.masson.cruciblecraft.content.blockentity.DynamoBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ElectricEngineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ElectricHeaterBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ElectricMotorBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidBedBurningBoxBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidSpringBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FuelGeneratorBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SteamEngineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.BedrockOreBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ReactorCoreBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SolidBurningBoxBlockEntity;
import com.masson.cruciblecraft.energy.battery.BatteryBlock;
import com.masson.cruciblecraft.energy.battery.BatteryBlockEntity;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.content.block.LargeBoilerBlock;
import com.masson.cruciblecraft.content.blockentity.AnvilBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CokeOvenBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CeramicMoldBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FoundryCastingBlockEntity;
import com.masson.cruciblecraft.content.mold.MoldRecipes;
import com.masson.cruciblecraft.content.blockentity.BoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrusherBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LargeBoilerBlockEntity;
import com.masson.cruciblecraft.energy.transformer.TransformerBlock;
import com.masson.cruciblecraft.energy.transformer.TransformerBlockEntity;
import com.masson.cruciblecraft.energy.converter.EnergyConverterHost;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineDisplayData;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class CrucibleJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(
                FluidPipeComponentProvider.INSTANCE,
                FluidPipeBlockEntity.class);
        registration.registerBlockDataProvider(
                ItemPipeComponentProvider.INSTANCE,
                ItemPipeBlockEntity.class);
        registration.registerBlockDataProvider(
                BoilerComponentProvider.INSTANCE,
                BoilerBlockEntity.class);
        registration.registerBlockDataProvider(
                LargeBoilerComponentProvider.INSTANCE,
                LargeBoilerBlockEntity.class);
        registration.registerBlockDataProvider(
                ProcessingMachineComponentProvider.INSTANCE,
                ConfiguredProcessingMachineBlockEntity.class);
        registration.registerBlockDataProvider(
                SteamEngineComponentProvider.INSTANCE,
                SteamEngineBlockEntity.class);
        registration.registerBlockDataProvider(
                CrucibleComponentProvider.INSTANCE,
                CrucibleBlockEntity.class);
        registration.registerBlockDataProvider(
                TransformerComponentProvider.INSTANCE,
                TransformerBlockEntity.class);
        registration.registerBlockDataProvider(
                ReactorCoreComponentProvider.INSTANCE,
                ReactorCoreBlockEntity.class);
        registration.registerBlockDataProvider(
                BatteryComponentProvider.INSTANCE,
                BatteryBlockEntity.class);
        registration.registerBlockDataProvider(
                ConverterComponentProvider.INSTANCE,
                DynamoBlockEntity.class);
        registration.registerBlockDataProvider(
                ConverterComponentProvider.INSTANCE,
                ElectricHeaterBlockEntity.class);
        registration.registerBlockDataProvider(
                ConverterComponentProvider.INSTANCE,
                ElectricEngineBlockEntity.class);
        registration.registerBlockDataProvider(
                ConverterComponentProvider.INSTANCE,
                FuelGeneratorBlockEntity.class);
        registration.registerBlockDataProvider(
                ConverterComponentProvider.INSTANCE,
                SolidBurningBoxBlockEntity.class);
        registration.registerBlockDataProvider(
                ConverterComponentProvider.INSTANCE,
                FluidBedBurningBoxBlockEntity.class);
        registration.registerBlockDataProvider(
                ConverterComponentProvider.INSTANCE,
                ElectricMotorBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(
                CrucibleComponentProvider.INSTANCE, MteInPlaceBlock.class);
        registration.registerBlockComponent(
                TransformerComponentProvider.INSTANCE, TransformerBlock.class);
        registration.registerBlockComponent(
                SteamEngineComponentProvider.INSTANCE, SteamEngineBlock.class);
        registration.registerBlockComponent(AnvilComponentProvider.INSTANCE, AnvilBlock.class);
        registration.registerBlockComponent(AnvilComponentProvider.INSTANCE, MteInPlaceBlock.class);
        registration.registerBlockComponent(CokeOvenComponentProvider.INSTANCE, CokeOvenBlock.class);
        registration.registerBlockComponent(CeramicMoldComponentProvider.INSTANCE, CeramicMoldBlock.class);
        registration.registerBlockComponent(CeramicMoldComponentProvider.INSTANCE, MteInPlaceBlock.class);
        registration.registerBlockComponent(BoilerComponentProvider.INSTANCE, BoilerBlock.class);
        registration.registerBlockComponent(
                LargeBoilerComponentProvider.INSTANCE, LargeBoilerBlock.class);
        registration.registerBlockComponent(
                LargeBoilerComponentProvider.INSTANCE, MteInPlaceBlock.class);
        registration.registerBlockComponent(CrusherComponentProvider.INSTANCE, CrusherBlock.class);
        registration.registerBlockComponent(
                ProcessingMachineComponentProvider.INSTANCE,
                ProcessingMachineBlock.class);
        registration.registerBlockComponent(
                CableComponentProvider.INSTANCE, CableBlock.class);
        registration.registerBlockComponent(
                FluidPipeComponentProvider.INSTANCE,
                FluidPipeBlock.class);
        registration.registerBlockComponent(
                FluidSpringComponentProvider.INSTANCE,
                FluidSpringBlock.class);
        registration.registerBlockComponent(
                ItemPipeComponentProvider.INSTANCE,
                ItemPipeBlock.class);
        registration.registerBlockComponent(
                ReactorCoreComponentProvider.INSTANCE, ReactorCoreBlock.class);
        registration.registerBlockComponent(
                BatteryComponentProvider.INSTANCE, BatteryBlock.class);
        registration.registerBlockComponent(
                ConverterComponentProvider.INSTANCE, DynamoBlock.class);
        registration.registerBlockComponent(
                ConverterComponentProvider.INSTANCE, ElectricHeaterBlock.class);
        registration.registerBlockComponent(
                ConverterComponentProvider.INSTANCE, ElectricEngineBlock.class);
        registration.registerBlockComponent(
                ConverterComponentProvider.INSTANCE, FuelGeneratorBlock.class);
        registration.registerBlockComponent(
                ConverterComponentProvider.INSTANCE, SolidBurningBoxBlock.class);
        registration.registerBlockComponent(
                ConverterComponentProvider.INSTANCE, FluidBedBurningBoxBlock.class);
        registration.registerBlockComponent(
                ConverterComponentProvider.INSTANCE, ElectricMotorBlock.class);
        registration.registerBlockComponent(
                SurfaceRockComponentProvider.INSTANCE, GtSurfaceRockBlock.class);
        registration.registerBlockComponent(
                SmallOreComponentProvider.INSTANCE, GtSmallOreBlock.class);
        registration.registerBlockComponent(
                RockBlockComponentProvider.INSTANCE, RockBlock.class);
        registration.usePickedResult(ModBlocks.GT_SURFACE_ROCK.get());
        registration.usePickedResult(ModBlocks.GT_INDICATOR_FLOWER.get());
    }

    private enum FluidPipeComponentProvider
            implements IBlockComponentProvider,
            IServerDataProvider<BlockAccessor> {
        INSTANCE;
        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "fluid_pipe");

        @Override
        public void appendTooltip(
                ITooltip tooltip,
                BlockAccessor accessor,
                IPluginConfig config) {
            if (!(accessor.getBlockEntity()
                    instanceof FluidPipeBlockEntity)) {
                return;
            }
            FluidPipeObservation observation =
                    FluidPipeObservation.fromServerData(
                            accessor.getServerData());
            for (int index = 0; index < observation.tanks().size(); index++) {
                FluidPipeObservation.Tank tank =
                        observation.tanks().get(index);
                SourceWailaRows.tank(
                        tooltip,
                        Integer.toString(index + 1),
                        observation.fluid(index),
                        tank.capacity());
            }
        }

        @Override
        public void appendServerData(
                CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity()
                    instanceof FluidPipeBlockEntity pipe) {
                FluidPipeObservation.writeServerData(data, pipe);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum FluidSpringComponentProvider implements IBlockComponentProvider {
        INSTANCE;
        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "fluid_spring");

        @Override
        public void appendTooltip(
                ITooltip tooltip,
                BlockAccessor accessor,
                IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof FluidSpringBlockEntity spring) {
                ResourceLocation fluidId = ResourceLocation.tryParse(spring.fluidId());
                Component fluidName = fluidId == null
                        ? Component.literal(spring.fluidId())
                        : new FluidStack(
                                BuiltInRegistries.FLUID.get(fluidId),
                                Math.max(1, spring.amount()))
                                .getHoverName();
                tooltip.add(Component.translatable(
                        "jade.cruciblecraft.fluid_spring.fluid",
                        fluidName));
                tooltip.add(Component.translatable(
                        "jade.cruciblecraft.fluid_spring.amount",
                        spring.amount()));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum SteamEngineComponentProvider
            implements IBlockComponentProvider,
            IServerDataProvider<BlockAccessor> {
        INSTANCE;
        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "steam_engine_source");

        @Override
        public void appendTooltip(
                ITooltip tooltip,
                BlockAccessor accessor,
                IPluginConfig config) {
            SteamEngineObservation observation =
                    SteamEngineObservation.fromServerData(
                            accessor.getServerData());
            if (observation.state().available()) {
                SourceWailaRows.state(
                        tooltip,
                        observation.state().value());
            }
            if (observation.inputMinimum().available()
                    && observation.inputMaximum().available()
                    && observation.outputMinimum().available()
                    && observation.outputMaximum().available()) {
                SourceWailaRows.energyIoRange(
                        tooltip,
                        observation.inputMinimum().value(),
                        observation.inputMaximum().value(),
                        "SU",
                        observation.outputMinimum().value(),
                        observation.outputMaximum().value(),
                        "KU");
            }
            if (observation.outputRate().available()) {
                SourceWailaRows.energyOutput(
                        tooltip,
                        observation.outputRate().value(),
                        "KU");
            }
        }

        @Override
        public void appendServerData(
                CompoundTag data,
                BlockAccessor accessor) {
            if (accessor.getBlockEntity()
                    instanceof SteamEngineBlockEntity engine) {
                SteamEngineObservation.writeServerData(data, engine);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum ItemPipeComponentProvider
            implements IBlockComponentProvider,
            IServerDataProvider<BlockAccessor> {
        INSTANCE;
        private static final String DELIVERED = "cc_delivered";
        private static final String CLOG_EVENTS = "cc_clog_events";
        private static final String COVER_COUNT = "cc_cover_count";
        private static final String COVERS = "cc_covers";
        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "item_pipe");

        @Override
        public void appendTooltip(
                ITooltip tooltip,
                BlockAccessor accessor,
                IPluginConfig config) {
            if (!(accessor.getBlock() instanceof ItemPipeBlock block)
                    || !(accessor.getBlockEntity()
                            instanceof ItemPipeBlockEntity pipe)) {
                return;
            }
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.item_pipe",
                    block.pipe().materialId(),
                    accessor.getServerData().contains(DELIVERED)
                            ? accessor.getServerData().getInt(DELIVERED)
                            : 0,
                    accessor.getServerData().contains(CLOG_EVENTS)
                            ? accessor.getServerData().getLong(CLOG_EVENTS)
                            : 0L,
                    accessor.getServerData().contains(COVER_COUNT)
                            ? accessor.getServerData().getInt(COVER_COUNT)
                            : pipe.coverSnapshot().size()));
            if (accessor.getServerData().contains(COVERS)
                    && !accessor.getServerData()
                            .getString(COVERS).isBlank()) {
                tooltip.add(Component.translatable(
                        "jade.cruciblecraft.pipe_covers",
                        accessor.getServerData().getString(COVERS)));
            }
        }

        @Override
        public void appendServerData(
                CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity()
                    instanceof ItemPipeBlockEntity pipe) {
                data.putInt(DELIVERED, pipe.deliveredThisWindow());
                data.putLong(CLOG_EVENTS, pipe.clogEvents());
                data.putInt(COVER_COUNT, pipe.coverSnapshot().size());
                data.putString(COVERS, pipe.coverSummary());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum CableComponentProvider
            implements IBlockComponentProvider {
        INSTANCE;
        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "cable");

        @Override
        public void appendTooltip(
                ITooltip tooltip,
                BlockAccessor accessor,
                IPluginConfig config) {
            if (!(accessor.getBlock() instanceof CableBlock block)
                    || !(accessor.getBlockEntity()
                            instanceof CableBlockEntity cable)) {
                return;
            }
            var electrical = block.transportProperties();
            String material = block.isLuFiber() ? "lu_fiber" : block.conductor().materialId();
            String specification = block.isLuFiber()
                    ? "lu_fiber"
                    : block.conductor().sourceSpecification();
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.cable",
                    material,
                    specification,
                    electrical.maxVoltage(),
                    electrical.maxAmperage(),
                    electrical.lossPerMeter(),
                    cable.transferredAmperes(),
                    cable.burnCounter()));
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum BoilerComponentProvider
            implements IBlockComponentProvider,
            IServerDataProvider<BlockAccessor> {
        INSTANCE;
        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, "bronze_boiler");
        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof BoilerBlockEntity boiler) {
                SourceWailaRows.state(tooltip, boiler.status());
                if (accessor.getBlock() instanceof EnergyConverterHost host) {
                    var profile = host.converterProfile();
                    SourceWailaRows.energyIoRecommended(
                            tooltip,
                            profile.inputPacket().size(),
                            profile.inputPacket().identity(),
                            profile.outputPacket().maxAmountPerTick(),
                            profile.outputPacket().identity());
                }
                SourceWailaRows.efficiency(
                        tooltip,
                        boiler.efficiencyBasisPoints());
                SourceWailaRows.energyAmount(
                        tooltip,
                        "ENERGY_CONTAINED",
                        boiler.accumulatedHu(),
                        "HU");
                var fluids = boiler.fluids(null);
                SourceWailaRows.tank(
                        tooltip,
                        "1",
                        fluids.getFluidInTank(0),
                        fluids.getTankCapacity(0));
                SourceWailaRows.tank(
                        tooltip,
                        "2",
                        fluids.getFluidInTank(1),
                        fluids.getTankCapacity(1));
                tooltip.remove(JadeIds.UNIVERSAL_FLUID_STORAGE);
                tooltip.add(Component.translatable("jade.cruciblecraft.boiler",
                        boiler.waterAmount(), boiler.waterCapacity(),
                        boiler.steamAmount(), boiler.steamCapacity(), boiler.accumulatedHu()));
            }
        }
        @Override
        public void appendServerData(
                CompoundTag data,
                BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof BoilerBlockEntity boiler) {
                ConverterObservation.writeServerData(data, boiler);
            }
        }
        @Override public ResourceLocation getUid() { return UID; }
    }

    private enum LargeBoilerComponentProvider
            implements IBlockComponentProvider,
            IServerDataProvider<BlockAccessor> {
        INSTANCE;
        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "large_boiler");

        @Override
        public void appendTooltip(
                ITooltip tooltip,
                BlockAccessor accessor,
                IPluginConfig config) {
            if (!(accessor.getBlockEntity()
                    instanceof LargeBoilerBlockEntity)) {
                return;
            }
            tooltip.remove(JadeIds.UNIVERSAL_FLUID_STORAGE);
            LargeBoilerObservation observation =
                    LargeBoilerObservation.fromServerData(
                            accessor.getServerData());
            if (observation.waterAmount().available()
                    && observation.waterCapacity().available()
                    && observation.waterId().available()) {
                SourceWailaRows.longTank(
                        tooltip,
                        "water",
                        observation.waterAmount().value(),
                        observation.waterCapacity().value(),
                        SourceWailaRows.fluidName(
                                observation.waterId().value()));
            }
            if (observation.steamAmount().available()
                    && observation.steamCapacity().available()
                    && observation.steamId().available()) {
                SourceWailaRows.longTank(
                        tooltip,
                        "steam",
                        observation.steamAmount().value(),
                        observation.steamCapacity().value(),
                        SourceWailaRows.fluidName(
                                observation.steamId().value()));
            }
        }

        @Override
        public void appendServerData(
                CompoundTag data,
                BlockAccessor accessor) {
            if (accessor.getBlockEntity()
                    instanceof LargeBoilerBlockEntity boiler) {
                LargeBoilerObservation.writeServerData(data, boiler);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum CrusherComponentProvider implements IBlockComponentProvider {
        INSTANCE;
        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, "bronze_crusher");
        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof CrusherBlockEntity crusher) {
                tooltip.add(Component.translatable("jade.cruciblecraft.crusher",
                        crusher.powerDemand(),
                        crusher.progress(),
                        crusher.duration(),
                        ProcessingMachineDisplayData.statusComponent(
                                crusher.pausedReason(), crusher.statusArgument())));
            }
        }
        @Override public ResourceLocation getUid() { return UID; }
    }

    private enum ProcessingMachineComponentProvider
            implements IBlockComponentProvider,
            IServerDataProvider<BlockAccessor> {
        INSTANCE;
        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, "processing_machine");
        private static final String SOURCE_STATE = "cc_waila_processing_state";
        private static final String SOURCE_ENERGY_MAX = "cc_waila_processing_energy_max";
        private static final String SOURCE_ENERGY_TYPE = "cc_waila_processing_energy_type";
        private static final String SOURCE_TANK_COUNT = "cc_waila_processing_tank_count";
        private static final String SOURCE_TANK_ID = "cc_waila_processing_tank_id_";
        private static final String SOURCE_TANK_AMOUNT = "cc_waila_processing_tank_amount_";
        private static final String SOURCE_TANK_CAPACITY = "cc_waila_processing_tank_capacity_";

        @Override
        public void appendTooltip(
                ITooltip tooltip,
                BlockAccessor accessor,
                IPluginConfig config) {
            if (accessor.getBlockEntity()
                    instanceof ConfiguredProcessingMachineBlockEntity machine) {
                CompoundTag sourceData = accessor.getServerData();
                if (sourceData.contains(SOURCE_STATE)) {
                    SourceWailaRows.state(
                            tooltip,
                            sourceData.getString(SOURCE_STATE));
                }
                if (sourceData.contains(SOURCE_ENERGY_MAX)
                        && sourceData.contains(SOURCE_ENERGY_TYPE)) {
                    SourceWailaRows.energyInputRange(
                            tooltip,
                            1L,
                            sourceData.getLong(SOURCE_ENERGY_MAX),
                            sourceData.getString(SOURCE_ENERGY_TYPE));
                }
                if (sourceData.contains(SOURCE_TANK_COUNT)) {
                    int sourceTankCount = Math.max(
                            0,
                            sourceData.getInt(SOURCE_TANK_COUNT));
                    for (int tank = 0; tank < sourceTankCount; tank++) {
                        SourceWailaRows.tank(
                                tooltip,
                                Integer.toString(tank + 1),
                                SourceWailaRows.fluid(
                                        sourceData.getString(SOURCE_TANK_ID + tank),
                                        sourceData.getInt(SOURCE_TANK_AMOUNT + tank)),
                                sourceData.getInt(SOURCE_TANK_CAPACITY + tank));
                    }
                }
                if (machine.spec().energy() != null
                        && !sourceData.contains(SOURCE_ENERGY_MAX)) {
                    SourceWailaRows.energyInputRange(
                            tooltip,
                            1L,
                            machine.spec().energy().maxPacket(),
                            machine.spec().energy().type().name());
                }
                tooltip.add(Component.translatable(
                        "jade.cruciblecraft.processing_machine",
                        machine.powerDemandLong(),
                        machine.progress(),
                        machine.duration(),
                        ProcessingMachineDisplayData.statusComponent(
                                machine.pausedReason(), machine.statusArgument())));
                for (int tank = 0; tank < machine.tanks().size(); tank++) {
                    var fluidTank = machine.tanks().get(tank);
                    if (!fluidTank.getFluid().isEmpty()) {
                        tooltip.add(Component.translatable(
                                "jade.cruciblecraft.processing_tank",
                                tank + 1,
                                fluidTank.getFluid().getHoverName(),
                                fluidTank.getFluidAmount(),
                                fluidTank.getCapacity()));
                        if (machine.fluidOutputTanks().contains(tank)) {
                            SourceWailaRows.fluidOutput(
                                    tooltip,
                                    Integer.toString(tank + 1),
                                    fluidTank.getFluid());
                        } else {
                            SourceWailaRows.tank(
                                    tooltip,
                                    Integer.toString(tank + 1),
                                    fluidTank.getFluid(),
                                    fluidTank.getCapacity());
                        }
                    }
                }
            }
        }

        @Override
        public void appendServerData(
                CompoundTag data,
                BlockAccessor accessor) {
            if (!(accessor.getBlockEntity()
                    instanceof ConfiguredProcessingMachineBlockEntity machine)) {
                return;
            }
            data.putString(
                    SOURCE_STATE,
                    machine.runningActively()
                            ? "active"
                            : machine.pausedReason().isBlank()
                                    ? "ready"
                                    : "stopped");
            if (machine.spec().energy() != null) {
                data.putLong(
                        SOURCE_ENERGY_MAX,
                        machine.spec().energy().maxPacket());
                data.putString(
                        SOURCE_ENERGY_TYPE,
                        machine.spec().energy().type().name());
            }
            data.putInt(SOURCE_TANK_COUNT, machine.tanks().size());
            for (int tank = 0; tank < machine.tanks().size(); tank++) {
                var fluid = machine.tanks().get(tank).getFluid();
                var id = fluid.isEmpty()
                        ? null
                        : BuiltInRegistries.FLUID.getKey(fluid.getFluid());
                data.putString(
                        SOURCE_TANK_ID + tank,
                        id == null ? "" : id.toString());
                data.putInt(
                        SOURCE_TANK_AMOUNT + tank,
                        fluid.getAmount());
                data.putInt(
                        SOURCE_TANK_CAPACITY + tank,
                        machine.tanks().get(tank).getCapacity());
            }
        }

        @Override public ResourceLocation getUid() { return UID; }
    }

    private enum CrucibleComponentProvider
            implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, "crucible");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof CrucibleBlockEntity crucible)) {
                return;
            }
            tooltip.remove(JadeIds.UNIVERSAL_FLUID_STORAGE);
            if (crucible.casingMaterialQuarantined()) {
                tooltip.add(Component.translatable(
                        "jade.cruciblecraft.material_quarantined",
                        Component.translatable("device.cruciblecraft.crucible"),
                        crucible.quarantinedCasingMaterialId()));
            }
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.casing",
                    Component.translatable("material.cruciblecraft." + crucible.casingMaterialId()),
                    crucible.casingTier(),
                    crucible.processingTier()));
            CrucibleObservation observation =
                    CrucibleObservation.fromServerData(accessor.getServerData());
            if (observation.temperatureKelvin().available()) {
                SourceWailaRows.temperature(
                        tooltip,
                        kelvinText(observation.temperatureKelvin()),
                        observation.meltdownKelvin().available()
                                ? kelvinText(observation.meltdownKelvin())
                                : SourceWailaRows.unavailable());
            }
            SourceWailaRows.weight(tooltip, SourceWailaRows.unavailable());
            if (observation.metals().available()
                    && observation.metals().value() != null) {
                for (CrucibleObservation.MetalAmount metal
                        : observation.metals().value()) {
                    SourceWailaRows.contents(
                            tooltip,
                            metal.materialId(),
                            JadeDisplayUnits.formatIngotAmount(metal.units()));
                }
            }
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.temperature_k",
                    kelvinText(observation.temperatureKelvin())));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.buffered_heat",
                    longText(observation.bufferedHeatHu())));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.meltdown_at",
                    kelvinText(observation.meltdownKelvin())));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.fill_level",
                    percentText(observation.fillPercent())));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.render_state",
                    renderText(observation)));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.cache_slot",
                    Component.translatable(
                            "jade.cruciblecraft." + observation.cacheDisplay())));
            if (observation.metals().available()
                    && observation.metals().value() != null) {
                for (CrucibleObservation.MetalAmount metal : observation.metals().value()) {
                    tooltip.add(Component.translatable(
                            "jade.cruciblecraft.material_amount",
                            JadeDisplayUnits.formatIngotAmount(metal.units()),
                            Component.translatable(
                                    "material.cruciblecraft." + metal.materialId())));
                }
            }
        }

        @Override
        public int getDefaultPriority() {
            return TooltipPosition.TAIL;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof CrucibleBlockEntity crucible) {
                CrucibleObservation.writeServerData(data, crucible);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        private static Component kelvinText(ObservationField<Double> field) {
            if (!field.available() || field.value() == null) {
                return Component.translatable("jade.cruciblecraft.unavailable");
            }
            return Component.literal(String.format(
                    Locale.ROOT,
                    "%.2f",
                    field.value()));
        }

        private static Component longText(ObservationField<Long> field) {
            if (!field.available() || field.value() == null) {
                return Component.translatable("jade.cruciblecraft.unavailable");
            }
            return Component.literal(Long.toString(field.value()));
        }

        private static Component percentText(ObservationField<Integer> field) {
            if (!field.available() || field.value() == null) {
                return Component.translatable("jade.cruciblecraft.unavailable");
            }
            return Component.literal(field.value() + "%");
        }

        private static Component renderText(CrucibleObservation observation) {
            String key = observation.renderKey();
            if ("unavailable".equals(key)) {
                return Component.translatable("jade.cruciblecraft.unavailable");
            }
            return Component.translatable("jade.cruciblecraft.render_state." + key);
        }
    }

    private enum TransformerComponentProvider
            implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, "transformer");

        @Override
        public void appendTooltip(
                ITooltip tooltip,
                BlockAccessor accessor,
                IPluginConfig config) {
            if (!(accessor.getBlock() instanceof TransformerBlock block)) {
                return;
            }
            TransformerObservation observation =
                    TransformerObservation.fromBlockAndServerData(
                            block.profile(),
                            accessor.getServerData());
            if (observation.active().available()) {
                SourceWailaRows.state(
                        tooltip,
                        Boolean.TRUE.equals(observation.active().value())
                                ? "active"
                                : "ready");
            }
            SourceWailaRows.energyIoRange(
                    tooltip,
                    block.profile().acceptMin(observation.reversed()),
                    block.profile().acceptMax(observation.reversed()),
                    "EU",
                    Math.max(
                            1L,
                            block.profile().emitRec(observation.reversed())
                                    / 2L),
                    Math.multiplyExact(
                            block.profile().emitRec(observation.reversed()),
                            2L),
                    "EU");
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.transformer.profile",
                    observation.lowVoltage().toUpperCase(Locale.ROOT),
                    observation.highVoltage().toUpperCase(Locale.ROOT)));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.transformer.mode",
                    observation.modeKnown()
                            ? Component.translatable(
                                    "jade.cruciblecraft.transformer.mode."
                                            + observation.modeKey())
                            : Component.translatable(
                                    "jade.cruciblecraft.unavailable")));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.transformer.buffer",
                    fieldLong(observation.storedEu()),
                    fieldLong(observation.capacityEu())));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.transformer.activity",
                    activityText(observation.active())));
            for (Direction direction : Direction.values()) {
                String name = direction.getSerializedName();
                TransformerObservation.Side side =
                        observation.sides().get(name);
                tooltip.add(Component.translatable(
                        "jade.cruciblecraft.transformer.side",
                        Component.translatable("jade.cruciblecraft.side." + name),
                        sideText(side)));
            }
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity()
                    instanceof TransformerBlockEntity transformer) {
                TransformerObservation.writeServerData(data, transformer);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        private static Component fieldLong(ObservationField<Long> field) {
            if (!field.available() || field.value() == null) {
                return Component.translatable("jade.cruciblecraft.unavailable");
            }
            return Component.literal(Long.toString(field.value()));
        }

        private static Component activityText(ObservationField<Boolean> field) {
            if (!field.available() || field.value() == null) {
                return Component.translatable("jade.cruciblecraft.unavailable");
            }
            return Component.translatable(
                    field.value()
                            ? "jade.cruciblecraft.transformer.activity.active"
                            : "jade.cruciblecraft.transformer.activity.idle");
        }

        private static Component sideText(TransformerObservation.Side side) {
            if (side == null || !side.available()) {
                return Component.translatable("jade.cruciblecraft.unavailable");
            }
            if (side.input()) {
                return Component.translatable(
                        "jade.cruciblecraft.transformer.input",
                        side.voltage());
            }
            return Component.translatable(
                    "jade.cruciblecraft.transformer.output",
                    side.voltage(),
                    side.packetMultiplier());
        }
    }

    private enum ReactorCoreComponentProvider
            implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;
        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, "reactor_core");

        @Override
        public void appendTooltip(
                ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            ReactorCoreObservation observation =
                    ReactorCoreObservation.fromServerData(accessor.getServerData());
            if (observation.running().available()
                    && observation.stopped().available()) {
                SourceWailaRows.state(
                        tooltip,
                        observation.stopped().value()
                                ? "stopped"
                                : observation.running().value()
                                        ? "active"
                                        : "ready");
            }
            if (observation.coolantId().available()
                    && observation.coolantAmount().available()) {
                SourceWailaRows.tank(
                        tooltip,
                        "1",
                        SourceWailaRows.fluid(
                                observation.coolantId().value(),
                                observation.coolantAmount().value()),
                        ReactorCoreBlockEntity.COOLANT_CAPACITY);
            }
            if (observation.outputId().available()
                    && observation.outputAmount().available()) {
                SourceWailaRows.tank(
                        tooltip,
                        "2",
                        SourceWailaRows.fluid(
                                observation.outputId().value(),
                                observation.outputAmount().value()),
                        ReactorCoreBlockEntity.COOLANT_CAPACITY
                                * com.masson.cruciblecraft.nuclear.ReactorCoolant.STEAM_PER_WATER);
            }
            if (observation.rods().available()
                    && observation.rods().value() != null) {
                for (ReactorCoreObservation.Rod rod
                        : observation.rods().value()) {
                    if (rod.name().isBlank()) {
                        continue;
                    }
                    SourceWailaRows.rod(
                            tooltip,
                            rod.name(),
                            rod.remaining(),
                            rod.neutrons(),
                            rod.moderated());
                }
            }
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.reactor.heat",
                    fieldLong(observation.heatHu())));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.reactor.last_heat",
                    fieldLong(observation.lastHeatHu())));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.reactor.neutrons",
                    fieldInt(observation.neutrons())));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.reactor.coolant",
                    fieldString(observation.coolantId()),
                    fieldInt(observation.coolantAmount())));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.reactor.output",
                    fieldString(observation.outputId()),
                    fieldInt(observation.outputAmount())));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.reactor.running",
                    fieldBool(observation.running())));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.reactor.safety",
                    safetyText(observation.safety())));
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof ReactorCoreBlockEntity core) {
                ReactorCoreObservation.writeServerData(data, core);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum BatteryComponentProvider
            implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;
        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, "battery");

        @Override
        public void appendTooltip(
                ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            BatteryObservation observation =
                    BatteryObservation.fromServerData(accessor.getServerData());
            if (observation.stored().available()) {
                SourceWailaRows.state(
                        tooltip,
                        observation.stored().value() > 0L
                                ? "active"
                                : "ready");
            }
            if (observation.sizeMin().available()
                    && observation.sizeMax().available()
                    && observation.energyType().available()) {
                SourceWailaRows.energyIoRange(
                        tooltip,
                        observation.sizeMin().value(),
                        observation.sizeMax().value(),
                        observation.energyType().value(),
                        observation.sizeMin().value(),
                        observation.sizeMax().value(),
                        observation.energyType().value());
            }
            if (observation.stored().available()
                    && observation.energyType().available()) {
                SourceWailaRows.energyAmount(
                        tooltip,
                        "ENERGY_CONTAINED",
                        observation.stored().value(),
                        observation.energyType().value());
            }
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.battery.charge",
                    fieldString(observation.energyType()),
                    fieldLong(observation.stored()),
                    fieldLong(observation.capacity())));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.battery.packet",
                    fieldLong(observation.sizeMin()),
                    fieldLong(observation.sizeMax()),
                    fieldLong(observation.inputSize())));
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof BatteryBlockEntity battery) {
                BatteryObservation.writeServerData(data, battery);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum ConverterComponentProvider
            implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;
        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "converter_dynamo");

        @Override
        public void appendTooltip(
                ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            ConverterObservation observation =
                    ConverterObservation.fromServerData(accessor.getServerData());
            if (observation.activity().available()) {
                SourceWailaRows.state(
                        tooltip,
                        observation.activity().value());
            }
            if (observation.windowMin().available()
                    && observation.windowMax().available()
                    && observation.inputPacket().available()
                    && observation.outputPacket().available()) {
                SourceWailaRows.energyIoRange(
                        tooltip,
                        observation.windowMin().value(),
                        observation.windowMax().value(),
                        observation.acceptsDisplay(),
                        observation.outputPacket().value(),
                        observation.outputPacket().value(),
                        observation.emitsDisplay());
            }
            if (observation.bufferStored().available()
                    && observation.bufferCapacity().available()) {
                SourceWailaRows.energyAmount(
                        tooltip,
                        "ENERGY_CONTAINED",
                        observation.bufferStored().value(),
                        observation.emitsDisplay());
            }
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.converter.accepts",
                    fieldString(observation.accepts())));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.converter.emits",
                    fieldString(observation.emits())));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.converter.packet",
                    fieldLong(observation.inputPacket()),
                    fieldLong(observation.outputPacket())));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.converter.window",
                    fieldLong(observation.windowMin()),
                    fieldLong(observation.windowNominal()),
                    fieldLong(observation.windowMax())));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.converter.activity",
                    fieldString(observation.activity())));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.converter.buffer",
                    fieldLong(observation.bufferStored()),
                    fieldLong(observation.bufferCapacity())));
            for (ConverterObservation.Tank tank : observation.tanks()) {
                tooltip.add(Component.translatable(
                        "jade.cruciblecraft.converter.tank",
                        tankFluidName(tank),
                        tank.amount(),
                        tank.capacity()));
            }
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            ConverterObservation.writeServerData(data, accessor.getBlockEntity());
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private static Component fieldLong(ObservationField<Long> field) {
        if (!field.available() || field.value() == null) {
            return Component.translatable("jade.cruciblecraft.unavailable");
        }
        return Component.literal(Long.toString(field.value()));
    }

    private static Component fieldInt(ObservationField<Integer> field) {
        if (!field.available() || field.value() == null) {
            return Component.translatable("jade.cruciblecraft.unavailable");
        }
        return Component.literal(Integer.toString(field.value()));
    }

    private static Component fieldString(ObservationField<String> field) {
        if (!field.available() || field.value() == null) {
            return Component.translatable("jade.cruciblecraft.unavailable");
        }
        if (field.value().isBlank()) {
            return Component.translatable("jade.cruciblecraft.unavailable");
        }
        return Component.literal(field.value());
    }

    private static Component tankFluidName(ConverterObservation.Tank tank) {
        if (tank.fluidId().isBlank() || tank.amount() <= 0) {
            return Component.translatable(
                    "jade.cruciblecraft.converter.tank_empty");
        }
        ResourceLocation id = ResourceLocation.tryParse(tank.fluidId());
        if (id == null) {
            return Component.literal(tank.fluidId());
        }
        var fluid = BuiltInRegistries.FLUID.get(id);
        if (fluid == Fluids.EMPTY) {
            return Component.literal(tank.fluidId());
        }
        return new FluidStack(fluid, Math.max(1, tank.amount()))
                .getHoverName();
    }

    private static Component fieldBool(ObservationField<Boolean> field) {
        if (!field.available() || field.value() == null) {
            return Component.translatable("jade.cruciblecraft.unavailable");
        }
        return Component.translatable(
                field.value()
                        ? "jade.cruciblecraft.reactor.running.on"
                        : "jade.cruciblecraft.reactor.running.off");
    }

    private static Component safetyText(ObservationField<String> field) {
        if (!field.available() || field.value() == null || field.value().isBlank()) {
            return Component.translatable("jade.cruciblecraft.unavailable");
        }
        return Component.translatable(
                "jade.cruciblecraft.reactor.safety." + field.value());
    }

    private enum AnvilComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, "anvil");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof AnvilBlockEntity anvil)) {
                return;
            }
            if (anvil.materialQuarantined()) {
                tooltip.add(Component.translatable(
                        "jade.cruciblecraft.material_quarantined",
                        Component.translatable("device.cruciblecraft.anvil"),
                        anvil.quarantinedMaterialId()));
            }
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.machine_material",
                    Component.translatable("material.cruciblecraft." + anvil.materialId()),
                    anvil.materialTier()));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.anvil_durability",
                    anvil.durability(),
                    anvil.maxDurability()));
            for (int slot = 0; slot < 2; slot++) {
                if (!anvil.workpiece(slot).isEmpty()) {
                    tooltip.add(Component.translatable(
                            "jade.cruciblecraft.anvil_slot",
                            slot + 1,
                            anvil.workpiece(slot).getCount(),
                            anvil.workpiece(slot).getHoverName()));
                }
            }
            if (anvil.strikes() > 0) {
                tooltip.add(Component.translatable(
                        "jade.cruciblecraft.anvil_progress",
                        anvil.strikes()));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum CokeOvenComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, "coke_oven");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof CokeOvenBlockEntity cokeOven)) {
                return;
            }
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.coke_oven.structure",
                    Component.translatable(cokeOven.structureValid()
                            ? "jade.cruciblecraft.coke_oven.valid"
                            : "jade.cruciblecraft.coke_oven.invalid")));
            if (cokeOven.recipeDuration() > 0) {
                tooltip.add(Component.translatable(
                        "jade.cruciblecraft.coke_oven.progress",
                        cokeOven.progress(),
                        cokeOven.recipeDuration()));
            }
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.coke_oven.fluid",
                    cokeOven.creosoteAmount(),
                    CokeOvenBlockEntity.TANK_CAPACITY));
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum CeramicMoldComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, "ceramic_mold");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof CeramicMoldBlockEntity mold) {
                appendMold(
                        tooltip,
                        mold.pattern(),
                        mold.isFilled(),
                        mold.outputCount(),
                        mold.materialId(),
                        mold.isSolidified(),
                        mold.temperature(),
                        mold.moldMaxTemperatureCelsius());
                return;
            }
            if (accessor.getBlockEntity() instanceof FoundryCastingBlockEntity mold
                    && !mold.basin()) {
                appendMold(
                        tooltip,
                        mold.pattern(),
                        mold.isFilled(),
                        mold.outputCount(),
                        mold.materialId(),
                        mold.isSolidified(),
                        mold.temperature(),
                        mold.moldMaxTemperatureCelsius());
            }
        }

        private static void appendMold(
                ITooltip tooltip,
                int pattern,
                boolean filled,
                int outputCount,
                String materialId,
                boolean solidified,
                float temperature,
                float maximumTemperature) {
            SourceWailaRows.temperature(
                    tooltip,
                    Component.literal(String.format(
                            Locale.ROOT,
                            "%.2f K",
                            JadeDisplayUnits.celsiusToKelvin(temperature))),
                    Component.literal(String.format(
                            Locale.ROOT,
                            "%.2f K",
                            JadeDisplayUnits.celsiusToKelvin(
                                    maximumTemperature))));
            MoldRecipes.recipe(pattern).ifPresent(prefix ->
                    SourceWailaRows.producing(
                            tooltip,
                            Component.literal(title(prefix.serializedName()))));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.mold_shape",
                    MoldRecipes.recipe(pattern)
                            .map(prefix -> title(prefix.serializedName()))
                            .orElse(title("unshaped"))));
            if (pattern != 0) {
                tooltip.add(Component.translatable(
                        "jade.cruciblecraft.mold_units",
                        MoldRecipes.requiredUnits(pattern)));
            }
            if (!filled) {
                tooltip.add(Component.translatable("jade.cruciblecraft.mold_empty"));
                return;
            }
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.mold_contents",
                    outputCount,
                    materialId));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.mold_state",
                    Component.translatable(solidified
                            ? "jade.cruciblecraft.mold_solid"
                            : "jade.cruciblecraft.mold_cooling"),
                    String.format(Locale.ROOT, "%.1f", temperature)));
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        private static String title(String value) {
            return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
        }
    }

    private enum SurfaceRockComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "surface_rock");

        @Override
        public void appendTooltip(
                ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlock() instanceof GtSurfaceRockBlock)) {
                return;
            }
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.surface_rock.material",
                    GtSurfaceRockBlock.materialName(
                            accessor.getBlockState(),
                            accessor.getBlockEntity())));
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum SmallOreComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "small_ore");

        @Override
        public void appendTooltip(
                ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof BedrockOreBlockEntity ore
                    && ore.hasMaterial()) {
                tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.small_ore.material",
                        MaterialFormItem.materialDisplayName(ore.materialId())));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum RockBlockComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "rock_block");

        @Override
        public void appendTooltip(
                ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlock() instanceof RockBlock rock)) {
                return;
            }
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.surface_rock.material",
                    MaterialFormItem.formName(
                            rock.materialId(),
                            MaterialPrefixCatalog.require("rock"))));
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
