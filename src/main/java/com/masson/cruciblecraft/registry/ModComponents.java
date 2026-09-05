package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.heat.HeatComponent;
import com.masson.cruciblecraft.machine.MachineDurabilityComponent;
import com.masson.cruciblecraft.material.MaterialId;
import com.masson.cruciblecraft.material.MissingMaterialComponent;

import com.mojang.serialization.Codec;

import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(CrucibleCraft.MODID);

    public static final DeferredHolder<
            net.minecraft.core.component.DataComponentType<?>,
            net.minecraft.core.component.DataComponentType<HeatComponent>> HEAT =
            COMPONENTS.registerComponentType(
                    "heat",
                    builder -> builder
                            .persistent(HeatComponent.CODEC)
                            .networkSynchronized(HeatComponent.STREAM_CODEC)
            );

    public static final DeferredHolder<
            net.minecraft.core.component.DataComponentType<?>,
            net.minecraft.core.component.DataComponentType<MissingMaterialComponent>> MISSING_MATERIAL =
            COMPONENTS.registerComponentType(
                    "missing_material",
                    builder -> builder
                            .persistent(MissingMaterialComponent.CODEC)
                            .networkSynchronized(MissingMaterialComponent.STREAM_CODEC)
            );

    public static final DeferredHolder<
            net.minecraft.core.component.DataComponentType<?>,
            net.minecraft.core.component.DataComponentType<String>> MACHINE_MATERIAL =
            COMPONENTS.registerComponentType(
                    "machine_material",
                    builder -> builder
                            .persistent(MaterialId.CODEC)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<
            net.minecraft.core.component.DataComponentType<?>,
            net.minecraft.core.component.DataComponentType<String>> TOOL_MATERIAL =
            COMPONENTS.registerComponentType(
                    "tool_material",
                    builder -> builder
                            .persistent(MaterialId.CODEC)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<
            net.minecraft.core.component.DataComponentType<?>,
            net.minecraft.core.component.DataComponentType<MachineDurabilityComponent>> MACHINE_DURABILITY =
            COMPONENTS.registerComponentType(
                    "machine_durability",
                    builder -> builder
                            .persistent(MachineDurabilityComponent.CODEC)
                            .networkSynchronized(MachineDurabilityComponent.STREAM_CODEC));

    public static final DeferredHolder<
            net.minecraft.core.component.DataComponentType<?>,
            net.minecraft.core.component.DataComponentType<SimpleFluidContent>> PORTABLE_FLUID =
            COMPONENTS.registerComponentType(
                    "portable_fluid",
                    builder -> builder
                            .persistent(SimpleFluidContent.CODEC)
                            .networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    public static final DeferredHolder<
            net.minecraft.core.component.DataComponentType<?>,
            net.minecraft.core.component.DataComponentType<SimpleFluidContent>> FLUID_CELL_CONTENT =
            COMPONENTS.registerComponentType(
                    "fluid_cell_content",
                    builder -> builder
                            .persistent(SimpleFluidContent.CODEC)
                            .networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    public static final DeferredHolder<
            net.minecraft.core.component.DataComponentType<?>,
            net.minecraft.core.component.DataComponentType<SimpleFluidContent>> GAS_CELL_CONTENT =
            COMPONENTS.registerComponentType(
                    "gas_cell_content",
                    builder -> builder
                            .persistent(SimpleFluidContent.CODEC)
                            .networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    public static final DeferredHolder<
            net.minecraft.core.component.DataComponentType<?>,
            net.minecraft.core.component.DataComponentType<Integer>> CIRCUIT_CONFIG =
            COMPONENTS.registerComponentType(
                    "circuit_config",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<
            net.minecraft.core.component.DataComponentType<?>,
            net.minecraft.core.component.DataComponentType<Integer>> FIREPROOF =
            COMPONENTS.registerComponentType(
                    "fireproof",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<
            net.minecraft.core.component.DataComponentType<?>,
            net.minecraft.core.component.DataComponentType<
                    com.masson.cruciblecraft.logistics.hopper.DustAmountLedger.Snapshot>>
                    DUST_FUNNEL = COMPONENTS.registerComponentType(
                            "dust_funnel",
                            builder -> builder
                                    .persistent(
                                            com.masson.cruciblecraft.logistics.hopper
                                                    .DustAmountLedger.Snapshot.CODEC)
                                    .networkSynchronized(
                                            com.masson.cruciblecraft.logistics.hopper
                                                    .DustAmountLedger.Snapshot.STREAM_CODEC));

    public static final DeferredHolder<
            net.minecraft.core.component.DataComponentType<?>,
            net.minecraft.core.component.DataComponentType<Long>> ELECTRIC_CHARGE =
            COMPONENTS.registerComponentType(
                    "electric_charge",
                    builder -> builder
                            .persistent(Codec.LONG)
                            .networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static final DeferredHolder<
            net.minecraft.core.component.DataComponentType<?>,
            net.minecraft.core.component.DataComponentType<Long>> BATTERY_CHARGE =
            COMPONENTS.registerComponentType(
                    "battery_charge",
                    builder -> builder
                            .persistent(Codec.LONG)
                            .networkSynchronized(ByteBufCodecs.VAR_LONG));

    private ModComponents() {}
}
