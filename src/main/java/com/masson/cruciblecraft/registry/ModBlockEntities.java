package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.blockentity.AnvilBlockEntity;
import com.masson.cruciblecraft.content.blockentity.BellowsBlockEntity;
import com.masson.cruciblecraft.content.blockentity.BoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CokeOvenBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CeramicMoldBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrusherBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.DynamoBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FireboxBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SteamEngineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SubsurfaceFluidDepositBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CrucibleCraft.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FireboxBlockEntity>> FIREBOX =
            BLOCK_ENTITIES.register(
                    "firebox",
                    () -> BlockEntityType.Builder.of(
                            FireboxBlockEntity::new,
                            ModBlocks.FIREBOX.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrucibleBlockEntity>> CRUCIBLE =
            BLOCK_ENTITIES.register(
                    "crucible",
                    () -> BlockEntityType.Builder.of(
                            CrucibleBlockEntity::new,
                            ModBlocks.CRUCIBLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnvilBlockEntity>> ANVIL =
            BLOCK_ENTITIES.register(
                    "anvil",
                    () -> BlockEntityType.Builder.of(
                            AnvilBlockEntity::new,
                            ModBlocks.ANVIL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CokeOvenBlockEntity>>
            COKE_OVEN = BLOCK_ENTITIES.register(
                    "coke_oven",
                    () -> BlockEntityType.Builder.of(
                            CokeOvenBlockEntity::new,
                            ModBlocks.COKE_OVEN.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BellowsBlockEntity>>
            BELLOWS = BLOCK_ENTITIES.register(
                    "bellows",
                    () -> BlockEntityType.Builder.of(
                            BellowsBlockEntity::new,
                            ModBlocks.BELLOWS.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CeramicMoldBlockEntity>>
            CERAMIC_MOLD = BLOCK_ENTITIES.register(
                    "ceramic_mold",
                    () -> BlockEntityType.Builder.of(
                            CeramicMoldBlockEntity::new,
                            ModBlocks.CERAMIC_MOLD.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BoilerBlockEntity>> BOILER =
            BLOCK_ENTITIES.register(
                    "bronze_boiler",
                    () -> BlockEntityType.Builder.of(
                            BoilerBlockEntity::new,
                            ModBlocks.BRONZE_BOILER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamEngineBlockEntity>>
            STEAM_ENGINE = BLOCK_ENTITIES.register(
                    "bronze_steam_engine",
                    () -> BlockEntityType.Builder.of(
                            SteamEngineBlockEntity::new,
                            ModBlocks.BRONZE_STEAM_ENGINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DynamoBlockEntity>>
            DYNAMO = BLOCK_ENTITIES.register(
                    "bronze_dynamo",
                    () -> BlockEntityType.Builder.of(
                            DynamoBlockEntity::new,
                            ModBlocks.BRONZE_DYNAMO.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrusherBlockEntity>> CRUSHER =
            BLOCK_ENTITIES.register(
                    "bronze_crusher",
                    () -> BlockEntityType.Builder.of(
                            CrusherBlockEntity::new,
                            ModBlocks.BRONZE_CRUSHER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConfiguredProcessingMachineBlockEntity>>
            PROCESSING_MACHINE = BLOCK_ENTITIES.register(
                    "processing_machine",
                    () -> BlockEntityType.Builder.of(
                            ConfiguredProcessingMachineBlockEntity::new,
                            ModBlocks.configuredProcessingBlocks()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableBlockEntity>>
            CABLE = BLOCK_ENTITIES.register(
                    "cable",
                    () -> BlockEntityType.Builder.of(
                            CableBlockEntity::new,
                            ModBlocks.electricalConductorBlockArray()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPipeBlockEntity>>
            FLUID_PIPE = BLOCK_ENTITIES.register(
                    "fluid_pipe",
                    () -> BlockEntityType.Builder.of(
                            FluidPipeBlockEntity::new,
                            ModBlocks.fluidPipeBlockArray()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemPipeBlockEntity>>
            ITEM_PIPE = BLOCK_ENTITIES.register(
                    "item_pipe",
                    () -> BlockEntityType.Builder.of(
                            ItemPipeBlockEntity::new,
                            ModBlocks.itemPipeBlockArray()).build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<SubsurfaceFluidDepositBlockEntity>>
                    SUBSURFACE_FLUID_DEPOSIT = BLOCK_ENTITIES.register(
                            "subsurface_fluid_deposit",
                            () -> BlockEntityType.Builder.of(
                                    SubsurfaceFluidDepositBlockEntity::new,
                                    ModBlocks.SUBSURFACE_FLUID_DEPOSIT.get())
                                    .build(null));

    private ModBlockEntities() {}
}
