package com.masson.cruciblecraft.registry;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.fluid.MoltenTransferMath;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, CrucibleCraft.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(BuiltInRegistries.FLUID, CrucibleCraft.MODID);
    private static final Map<String, MoltenFluidEntry> MOLTEN_BY_MATERIAL = new LinkedHashMap<>();
    private static volatile Map<Fluid, MaterialDefinition> materialByFluid;
    private static Collection<MoltenFluidEntry> moltenFluids = java.util.List.of();

    public static final Supplier<FluidType> CREOSOTE_TYPE = FLUID_TYPES.register(
            "creosote",
            () -> new FluidType(FluidType.Properties.create()
                    .density(1_100)
                    .viscosity(2_000)));

    public static final DeferredHolder<Fluid, FlowingFluid> CREOSOTE_SOURCE =
            FLUIDS.register("creosote", () -> new BaseFlowingFluid.Source(properties()));
    public static final DeferredHolder<Fluid, FlowingFluid> CREOSOTE_FLOWING =
            FLUIDS.register("flowing_creosote", () -> new BaseFlowingFluid.Flowing(properties()));

    public static final Supplier<FluidType> STEAM_TYPE = FLUID_TYPES.register(
            "steam",
            () -> new FluidType(FluidType.Properties.create()
                    .temperature(373)
                    .density(-100)
                    .viscosity(200)));
    public static final DeferredHolder<Fluid, FlowingFluid> STEAM_SOURCE =
            FLUIDS.register("steam", () -> new BaseFlowingFluid.Source(steamProperties()));
    public static final DeferredHolder<Fluid, FlowingFluid> STEAM_FLOWING =
            FLUIDS.register("flowing_steam", () -> new BaseFlowingFluid.Flowing(steamProperties()));

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(
                CREOSOTE_TYPE,
                CREOSOTE_SOURCE,
                CREOSOTE_FLOWING)
                .bucket(ModItems.CREOSOTE_BUCKET)
                .block(ModBlocks.CREOSOTE);
    }

    private static BaseFlowingFluid.Properties steamProperties() {
        return new BaseFlowingFluid.Properties(STEAM_TYPE, STEAM_SOURCE, STEAM_FLOWING)
                .bucket(ModItems.STEAM_BUCKET)
                .block(ModBlocks.STEAM)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2);
    }

    public static void registerMaterials(Collection<MaterialDefinition> definitions) {
        if (!MOLTEN_BY_MATERIAL.isEmpty()) {
            throw new IllegalStateException("Molten material fluids already registered");
        }
        for (MaterialDefinition material : definitions) {
            if (!material.moltenFluid()) {
                continue;
            }
            String sourceId = "molten_" + material.id();
            String flowingId = "flowing_" + sourceId;
            Supplier<FluidType> type = FLUID_TYPES.register(
                    sourceId,
                    () -> new FluidType(FluidType.Properties.create()
                            .temperature(MoltenTransferMath.celsiusToKelvin(
                                    material.thermal().meltingPoint()))
                            .density(Math.max(1, (int) Math.round(material.thermal().density() * 1_000.0)))
                            .viscosity(6_000)));
            AtomicReference<DeferredHolder<Fluid, FlowingFluid>> source = new AtomicReference<>();
            AtomicReference<DeferredHolder<Fluid, FlowingFluid>> flowing = new AtomicReference<>();
            Supplier<BaseFlowingFluid.Properties> properties = () -> new BaseFlowingFluid.Properties(
                    type,
                    () -> source.get().get(),
                    () -> flowing.get().get());
            source.set(FLUIDS.register(sourceId, () -> new BaseFlowingFluid.Source(properties.get())));
            flowing.set(FLUIDS.register(flowingId, () -> new BaseFlowingFluid.Flowing(properties.get())));
            MOLTEN_BY_MATERIAL.put(
                    material.id(),
                    new MoltenFluidEntry(material, type, source.get(), flowing.get()));
        }
        moltenFluids = java.util.List.copyOf(MOLTEN_BY_MATERIAL.values());
    }

    /**
     * Called from common setup after deferred holders have bound. Publication is
     * volatile so capability queries on either logical side only see a complete map.
     */
    public static void finalizeMaterialLookup() {
        if (materialByFluid != null) {
            return;
        }
        IdentityHashMap<Fluid, MaterialDefinition> reverse = new IdentityHashMap<>();
        for (MoltenFluidEntry entry : moltenFluids) {
            if (!entry.source().isBound() || !entry.flowing().isBound()) {
                throw new IllegalStateException(
                        "Molten fluid lookup finalized before registries were bound");
            }
            reverse.put(entry.source().get(), entry.material());
            reverse.put(entry.flowing().get(), entry.material());
        }
        materialByFluid = java.util.Collections.unmodifiableMap(reverse);
    }

    public static Optional<MoltenFluidEntry> molten(String materialId) {
        return Optional.ofNullable(MOLTEN_BY_MATERIAL.get(materialId));
    }

    public static Optional<MaterialDefinition> material(Fluid fluid) {
        Map<Fluid, MaterialDefinition> lookup = materialByFluid;
        if (lookup == null) {
            throw new IllegalStateException(
                    "Molten fluid lookup used before common setup finalized it");
        }
        return Optional.ofNullable(lookup.get(fluid));
    }

    public static Collection<MoltenFluidEntry> moltenFluids() {
        return moltenFluids;
    }

    public record MoltenFluidEntry(
            MaterialDefinition material,
            Supplier<FluidType> type,
            DeferredHolder<Fluid, FlowingFluid> source,
            DeferredHolder<Fluid, FlowingFluid> flowing) {}

    private ModFluids() {}
}
