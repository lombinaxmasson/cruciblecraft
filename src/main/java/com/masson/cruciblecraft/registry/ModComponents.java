package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.heat.HeatComponent;
import com.masson.cruciblecraft.machine.MachineDurabilityComponent;
import com.masson.cruciblecraft.material.MissingMaterialComponent;
import com.mojang.serialization.Codec;

import net.minecraft.network.codec.ByteBufCodecs;
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
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<
            net.minecraft.core.component.DataComponentType<?>,
            net.minecraft.core.component.DataComponentType<String>> TOOL_MATERIAL =
            COMPONENTS.registerComponentType(
                    "tool_material",
                    builder -> builder
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<
            net.minecraft.core.component.DataComponentType<?>,
            net.minecraft.core.component.DataComponentType<MachineDurabilityComponent>> MACHINE_DURABILITY =
            COMPONENTS.registerComponentType(
                    "machine_durability",
                    builder -> builder
                            .persistent(MachineDurabilityComponent.CODEC)
                            .networkSynchronized(MachineDurabilityComponent.STREAM_CODEC));

    private ModComponents() {}
}
